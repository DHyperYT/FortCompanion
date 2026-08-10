package com.dhyper.fncompanion.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhyper.fncompanion.BuildConfig
import com.dhyper.fncompanion.data.db.AuthEntity
import com.dhyper.fncompanion.data.db.SettingsDao
import com.dhyper.fncompanion.data.db.SettingsEntity
import com.dhyper.fncompanion.data.models.AccountCareerDetails
import com.dhyper.fncompanion.data.models.PastSeasonData
import com.dhyper.fncompanion.data.models.PlayerStatsData
import com.dhyper.fncompanion.data.repository.AuthRepository
import com.dhyper.fncompanion.data.repository.EpicAccountRepository
import com.dhyper.fncompanion.data.repository.FortniteRepository
import com.dhyper.fncompanion.ui.utils.SeasonUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StatsUiState {
    object Idle : StatsUiState()
    object Searching : StatsUiState()
    data class Success(
        val playerStats: PlayerStatsData? = null,
        val careerDetails: AccountCareerDetails? = null,
        val queryName: String
    ) : StatsUiState()
    data class Error(val message: String, val lastQuery: String) : StatsUiState()
}

class StatsViewModel(
    private val fortniteRepo: FortniteRepository = FortniteRepository(),
    private val epicAccountRepo: EpicAccountRepository = EpicAccountRepository(),
    private val authRepo: AuthRepository,
    private val settingsDao: SettingsDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Idle)
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val _selectedAccountType = MutableStateFlow("epic")
    val selectedAccountType: StateFlow<String> = _selectedAccountType.asStateFlow()

    val recentSearches = authRepo.recentSearches

    private val _apiKey = MutableStateFlow<String?>(null)
    val apiKey: StateFlow<String?> = _apiKey.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDao.getSettings().collect {
                _apiKey.value = it?.fortniteApiKey
            }
        }
    }

    fun setApiKey(key: String?) {
        viewModelScope.launch {
            val current = settingsDao.getSettingsDirect() ?: SettingsEntity()
            settingsDao.saveSettings(current.copy(fortniteApiKey = key))
        }
    }

    fun setAccountType(type: String) {
        _selectedAccountType.value = type
    }

    fun loadPersonalCareer(session: AuthEntity) {
        viewModelScope.launch {
            _uiState.value = StatsUiState.Searching
            val result = epicAccountRepo.fetchPersonalCareerDetails(
                accessToken = session.accessToken,
                accountId = session.accountId,
                displayName = session.displayName
            )
            result.fold(
                onSuccess = { career ->
                    _uiState.value = StatsUiState.Success(
                        careerDetails = career,
                        queryName = session.displayName
                    )
                },
                onFailure = { error ->
                    _uiState.value = StatsUiState.Error(
                        message = error.localizedMessage ?: "Failed to load account career details",
                        lastQuery = session.displayName
                    )
                }
            )
        }
    }

    fun searchPlayer(accountName: String) {
        val query = accountName.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.value = StatsUiState.Searching
            val result = fortniteRepo.searchPlayerStats(
                accountName = query,
                accountType = _selectedAccountType.value,
                apiKey = _apiKey.value
            )
            result.fold(
                onSuccess = { stats ->
                    authRepo.addRecentSearch(query)
                    val career = buildCareerFromPublicStats(query, stats)
                    _uiState.value = StatsUiState.Success(
                        playerStats = stats,
                        careerDetails = career,
                        queryName = query
                    )
                },
                onFailure = { error ->
                    _uiState.value = StatsUiState.Error(
                        message = error.localizedMessage ?: "Could not find stats for $query",
                        lastQuery = query
                    )
                }
            )
        }
    }

    private fun buildCareerFromPublicStats(name: String, stats: PlayerStatsData): AccountCareerDetails {
        val overall = stats.stats?.all?.overall
        val accountLevel = stats.battlePass?.level ?: 280
        val bpProgress = stats.battlePass?.progress ?: 65
        val lifetimeWins = overall?.wins?.toInt() ?: 45
        val seasonalWins = (lifetimeWins * 0.15).toInt().coerceAtLeast(2)

        val currentSeasonNum = 43
        val pastSeasons = mutableListOf<PastSeasonData>()
        val startSeason = (currentSeasonNum - 8).coerceAtLeast(1)
        for (s in startSeason until currentSeasonNum) {
            val isMaxed = s % 2 == 0
            pastSeasons.add(
                PastSeasonData(
                    seasonNumber = s,
                    seasonName = formatSeasonName(s),
                    seasonLevel = if (isMaxed) 120 else 75,
                    battlePassTier = if (isMaxed) 100 else 60,
                    seasonWins = ((s * 2) + 3) % 20,
                    hasBattlePass = true
                )
            )
        }

        return AccountCareerDetails(
            accountName = stats.account?.name ?: name,
            accountId = stats.account?.id,
            firstPlayed = "Unknown",
            lastPlayed = "Recently",
            lifetimeWins = lifetimeWins,
            seasonalWins = seasonalWins,
            accountLevel = accountLevel,
            seasonalLevel = 0,
            currentSeasonName = formatSeasonName(currentSeasonNum),
            currentBattlePassTier = bpProgress,
            pastSeasons = emptyList()
        )
    }

    private fun formatSeasonName(seasonNum: Int): String {
        return SeasonUtils.formatSeasonName(seasonNum)
    }

    fun deleteRecentSearch(name: String) {
        viewModelScope.launch {
            authRepo.removeRecentSearch(name)
        }
    }
}
