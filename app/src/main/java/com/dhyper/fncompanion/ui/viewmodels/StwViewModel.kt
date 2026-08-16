package com.dhyper.fncompanion.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dhyper.fncompanion.data.db.AuthEntity
import com.dhyper.fncompanion.data.models.StwHomebaseData
import com.dhyper.fncompanion.data.models.StwMissionAlert
import com.dhyper.fncompanion.data.repository.AuthRepository
import com.dhyper.fncompanion.data.repository.EpicAccountRepository
import com.dhyper.fncompanion.data.repository.StwAutomationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StwUiState {
    object Unauthenticated : StwUiState()
    object Loading : StwUiState()
    data class Success(
        val homebase: StwHomebaseData?,
        val alerts: List<StwMissionAlert> = emptyList()
    ) : StwUiState()
    data class Error(val message: String) : StwUiState()
}

sealed class StwActionResult {
    object Idle : StwActionResult()
    object Loading : StwActionResult()
    data class Success(val message: String) : StwActionResult()
    data class Error(val message: String) : StwActionResult()
}

class StwViewModel(
    application: Application,
    private val authRepo: AuthRepository,
    private val epicAccountRepo: EpicAccountRepository = EpicAccountRepository(),
    private val stwAutoRepo: StwAutomationRepository = StwAutomationRepository(epicAccountRepo)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<StwUiState>(StwUiState.Loading)
    val uiState: StateFlow<StwUiState> = _uiState.asStateFlow()

    private val _homebaseData = MutableStateFlow<StwHomebaseData?>(null)
    val homebaseData: StateFlow<StwHomebaseData?> = _homebaseData.asStateFlow()

    private val _actionResult = MutableStateFlow<StwActionResult>(StwActionResult.Idle)
    val actionResult: StateFlow<StwActionResult> = _actionResult.asStateFlow()

    private val _commanderLevelProgress = MutableStateFlow(0f)
    val commanderLevelProgress: StateFlow<Float> = _commanderLevelProgress.asStateFlow()

    private val _xpToNextLevel = MutableStateFlow(0L)
    val xpToNextLevel: StateFlow<Long> = _xpToNextLevel.asStateFlow()

    private val _displayName = MutableStateFlow("PLAYER")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _autoRecycleJunk = MutableStateFlow(false)
    val autoRecycleJunk: StateFlow<Boolean> = _autoRecycleJunk.asStateFlow()

    private val _autoClaimLlamas = MutableStateFlow(false)
    val autoClaimLlamas: StateFlow<Boolean> = _autoClaimLlamas.asStateFlow()

    private val _isFounder = MutableStateFlow(false)
    val isFounder: StateFlow<Boolean> = _isFounder.asStateFlow()

    private val _vbucksAlertsEnabled = MutableStateFlow(false)
    val vbucksAlertsEnabled: StateFlow<Boolean> = _vbucksAlertsEnabled.asStateFlow()

    private val _vbucksAlertTime = MutableStateFlow("00:00")
    val vbucksAlertTime: StateFlow<String> = _vbucksAlertTime.asStateFlow()

    private val _stwAutomationTime = MutableStateFlow("00:00")
    val stwAutomationTime: StateFlow<String> = _stwAutomationTime.asStateFlow()

    init {
        refreshAll()
        loadGlobalSettings()
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            authRepo.authSession.collect { state ->
                if (state is com.dhyper.fncompanion.data.models.AuthState.Active) {
                    val session = state.session
                    _autoRecycleJunk.value = session.stwAutoRecycleJunk
                    _autoClaimLlamas.value = session.stwAutoClaimLlamas
                    _displayName.value = session.displayName
                    _isFounder.value = session.isFounder
                }
            }
        }
    }

    private fun loadGlobalSettings() {
        viewModelScope.launch {
            val settingsDao = com.dhyper.fncompanion.data.db.AppDatabase.getDatabase(getApplication()).settingsDao()
            settingsDao.getSettings().collect { s ->
                s?.let {
                    _vbucksAlertsEnabled.value = it.vbucksAlertsEnabled
                    _vbucksAlertTime.value = it.stwVBucksAlertTime
                    _stwAutomationTime.value = it.stwAutomationTime
                }
            }
        }
    }

    fun updateVBucksAlerts(enabled: Boolean) {
        viewModelScope.launch {
            val db = com.dhyper.fncompanion.data.db.AppDatabase.getDatabase(getApplication())
            val current = db.settingsDao().getSettingsDirect() ?: com.dhyper.fncompanion.data.db.SettingsEntity()
            db.settingsDao().saveSettings(current.copy(vbucksAlertsEnabled = enabled))
        }
    }

    fun updateVBucksAlertTime(time: String) {
        viewModelScope.launch {
            val db = com.dhyper.fncompanion.data.db.AppDatabase.getDatabase(getApplication())
            val current = db.settingsDao().getSettingsDirect() ?: com.dhyper.fncompanion.data.db.SettingsEntity()
            db.settingsDao().saveSettings(current.copy(stwVBucksAlertTime = time))
        }
    }

    fun updateAutomationTime(time: String) {
        viewModelScope.launch {
            val db = com.dhyper.fncompanion.data.db.AppDatabase.getDatabase(getApplication())
            val current = db.settingsDao().getSettingsDirect() ?: com.dhyper.fncompanion.data.db.SettingsEntity()
            db.settingsDao().saveSettings(current.copy(stwAutomationTime = time))
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.value = StwUiState.Loading
            val session = authRepo.ensureActiveSession().getOrNull()
            if (session == null) {
                _uiState.value = StwUiState.Unauthenticated
                return@launch
            }

            // Sync reactive fields from session
            _autoRecycleJunk.value = session.stwAutoRecycleJunk
            _autoClaimLlamas.value = session.stwAutoClaimLlamas
            _displayName.value = session.displayName
            _isFounder.value = session.isFounder

            try {
                val homebaseResult = epicAccountRepo.fetchStwHomebaseData(
                    getApplication(), 
                    session.accessToken, 
                    session.accountId
                )
                val alertsResult = epicAccountRepo.fetchStwWorldInfoFull(getApplication(), session.accessToken)

                if (homebaseResult.isSuccess) {
                    val hb = homebaseResult.getOrNull()
                    _homebaseData.value = hb
                    hb?.let { 
                        authRepo.updateFounderStatus(session.accountId, it.isFounder)
                        calculateCommanderProgress(it.commanderLevel, it.commanderXp) 
                    }

                    _uiState.value = StwUiState.Success(
                        homebase = hb,
                        alerts = alertsResult.getOrDefault(emptyList())
                    )
                } else {
                    _uiState.value = StwUiState.Error(homebaseResult.exceptionOrNull()?.message ?: "Failed to fetch profile")
                }
            } catch (e: Exception) {
                _uiState.value = StwUiState.Error(e.localizedMessage ?: "Unknown STW error")
            }
        }
    }

    private fun calculateCommanderProgress(level: Int, currentXp: Long) {
        // Simplified STW Commander XP calculation
        // Level 310 is max. XP needed for level L to L+1
        // Below 100 it varies, above 100 it's roughly linear or constant for some segments
        // Real curve is complex, but Segments:
        // Level 100-310 segment: each level is roughly 1.3M to 1.5M XP segments.
        
        if (level >= 310) {
            _commanderLevelProgress.value = 1.0f
            _xpToNextLevel.value = 0L
            return
        }

        // STW Commander Level XP segments (approximate)
        // Level 1-100: starts at 5000, grows.
        // Level 309 to 310 requires ~1.5M XP.
        val xpRequiredForNext = if (level < 100) (level * 5000L) + 10000 
                               else 1343750L // Rough average for high levels
                               
        // Assuming currentXp is total lifetime XP, we need current level's start XP.
        // This is tricky without the full table.
        // Let's use a simpler heuristic for debug: progress is (currentXp % xpRequiredForNext) / xpRequiredForNext
        _commanderLevelProgress.value = (currentXp % xpRequiredForNext).toFloat() / xpRequiredForNext.toFloat()
        _xpToNextLevel.value = xpRequiredForNext - (currentXp % xpRequiredForNext)
    }

    fun clearActionResult() {
        _actionResult.value = StwActionResult.Idle
    }

    private fun getRvn(profileId: String = "campaign"): Int {
        return _homebaseData.value?.profileRevisions?.get(profileId) ?: -1
    }

    fun openLlama(llamaId: String) {
        executeAction("Opening Llama...") { session -> epicAccountRepo.openLlama(session.accessToken, session.accountId, llamaId) }
    }

    fun recycleItems(itemIds: List<String>) {
        executeAction("Recycling ${itemIds.size} items...") { session -> epicAccountRepo.recycleItems(session.accessToken, session.accountId, itemIds, getRvn()) }
    }

    fun recycleBackpackItems(itemIds: List<String>) {
        viewModelScope.launch {
            _actionResult.value = StwActionResult.Loading
            val session = authRepo.ensureActiveSession().getOrNull() ?: run {
                _actionResult.value = StwActionResult.Error("Session expired")
                return@launch
            }

            val homebase = _homebaseData.value ?: return@launch
            val allBackpack = homebase.inventory.backpack + homebase.inventory.eventBackpack + homebase.inventory.ventureBackpack
            
            val itemPairs = itemIds.mapNotNull { id ->
                val item = allBackpack.find { it.id == id }
                if (item != null) {
                    mapOf("itemId" to id, "quantity" to 1)
                } else null
            }

            if (itemPairs.isEmpty()) {
                _actionResult.value = StwActionResult.Error("No valid backpack items selected")
                return@launch
            }

            val result = epicAccountRepo.disassembleItems(session.accessToken, session.accountId, itemPairs, getRvn("theater0"))
            if (result.isSuccess) {
                _actionResult.value = StwActionResult.Success("Successfully recycled ${itemPairs.size} items")
                refreshAll()
            } else {
                _actionResult.value = StwActionResult.Error(result.exceptionOrNull()?.message ?: "Recycling failed")
            }
        }
    }

    fun upgradeItem(itemId: String, levels: Int = 1) {
        executeAction("Upgrading item...") { session -> epicAccountRepo.upgradeItemBulk(session.accessToken, session.accountId, itemId, levels, getRvn()) }
    }

    fun evolveItem(itemId: String, evolutionIndex: Int) {
        executeAction("Evolving item...") { session -> epicAccountRepo.evolveItem(session.accessToken, session.accountId, itemId, evolutionIndex, getRvn()) }
    }

    fun unslotItem(itemId: String) {
        executeAction("Unslotting item...") { session -> epicAccountRepo.unslotFromCollectionBook(session.accessToken, session.accountId, itemId, getRvn()) }
    }

    fun slotToCollectionBook(itemId: String) {
        executeAction("Slotting to collection book...") { session -> epicAccountRepo.slotToCollectionBook(session.accessToken, session.accountId, itemId, getRvn()) }
    }

    fun claimResearch() {
        executeAction("Collecting research...") { session -> epicAccountRepo.claimResearchPoints(session.accessToken, session.accountId, getRvn()) }
    }

    fun upgradeResearch(statId: String) {
        executeAction("Upgrading research...") { session -> epicAccountRepo.purchaseResearchStat(session.accessToken, session.accountId, statId, getRvn()) }
    }

    fun claimDailyReward() {
        executeAction("Claiming daily reward...") { session -> epicAccountRepo.claimDailyReward(session.accessToken, session.accountId, getRvn()) }
    }

    fun claimMissions() {
        executeAction("Claiming missions...") { session -> epicAccountRepo.claimMissions(session.accessToken, session.accountId, getRvn()) }
    }

    fun setActiveLoadout(loadoutId: String) {
        executeAction("Switching loadout...") { session -> epicAccountRepo.setActiveHeroLoadout(session.accessToken, session.accountId, loadoutId, getRvn()) }
    }

    fun assignHeroToLoadout(loadoutId: String, slotName: String, heroId: String) {
        executeAction("Assigning hero...") { session -> epicAccountRepo.assignHeroToLoadout(session.accessToken, session.accountId, loadoutId, slotName, heroId, getRvn()) }
    }

    fun clearHeroLoadoutSlot(loadoutId: String, slotName: String) {
        executeAction("Clearing slot...") { session -> epicAccountRepo.clearHeroLoadoutSlot(session.accessToken, session.accountId, loadoutId, slotName, getRvn()) }
    }

    fun assignTeamPerk(loadoutId: String, teamPerkId: String) {
        executeAction("Assigning team perk...") { session -> epicAccountRepo.assignTeamPerkToLoadout(session.accessToken, session.accountId, loadoutId, teamPerkId, getRvn()) }
    }

    fun assignGadget(loadoutId: String, slotIndex: Int, gadgetId: String) {
        executeAction("Assigning gadget...") { session -> epicAccountRepo.assignGadgetToLoadout(session.accessToken, session.accountId, loadoutId, slotIndex, gadgetId, getRvn()) }
    }

    fun assignWorkerToSquad(squadId: String, slotIndex: Int, workerId: String) {
        executeAction("Assigning survivor...") { session -> epicAccountRepo.assignWorkerToSquad(session.accessToken, session.accountId, squadId, slotIndex, workerId, getRvn()) }
    }

    fun claimQuestReward(questId: String) {
        executeAction("Claiming reward...") { session -> epicAccountRepo.claimQuestReward(session.accessToken, session.accountId, questId, getRvn()) }
    }

    fun rerollDailyQuest(questId: String) {
        executeAction("Rerolling quest...") { session -> epicAccountRepo.rerollDailyQuest(session.accessToken, session.accountId, questId, getRvn()) }
    }

    fun setAutoRecycleJunk(enabled: Boolean) {
        viewModelScope.launch {
            val session = authRepo.ensureActiveSession().getOrNull() ?: return@launch
            authRepo.updateStwAutoRecycleJunk(session.accountId, enabled)
            _autoRecycleJunk.value = enabled
        }
    }

    fun setAutoClaimLlamas(enabled: Boolean) {
        viewModelScope.launch {
            val session = authRepo.ensureActiveSession().getOrNull() ?: return@launch
            authRepo.updateStwAutoClaimLlamas(session.accountId, enabled)
            _autoClaimLlamas.value = enabled
        }
    }

    fun recycleJunkItems() {
        viewModelScope.launch {
            _actionResult.value = StwActionResult.Loading
            val session = authRepo.ensureActiveSession().getOrNull() ?: run {
                _actionResult.value = StwActionResult.Error("Session expired")
                return@launch
            }

            val result = stwAutoRepo.runAutoRecycleJunk(getApplication(), session)
            if (result.isSuccess) {
                _actionResult.value = StwActionResult.Success(result.getOrNull() ?: "Done")
                refreshAll()
            } else {
                _actionResult.value = StwActionResult.Error(result.exceptionOrNull()?.message ?: "Failed")
            }
        }
    }

    fun purchaseEligibleStorefrontItems() {
        viewModelScope.launch {
            _actionResult.value = StwActionResult.Loading
            val session = authRepo.ensureActiveSession().getOrNull() ?: run {
                _actionResult.value = StwActionResult.Error("Session expired")
                return@launch
            }
            
            val result = stwAutoRepo.runAutoClaimLlamas(session)
            if (result.isSuccess) {
                _actionResult.value = StwActionResult.Success(result.getOrNull() ?: "Done")
                refreshAll()
            } else {
                _actionResult.value = StwActionResult.Error(result.exceptionOrNull()?.message ?: "Failed")
            }
        }
    }

    private fun <T> executeAction(message: String, action: suspend (AuthEntity) -> Result<T>) {
        viewModelScope.launch {
            _actionResult.value = StwActionResult.Loading
            val session = authRepo.ensureActiveSession().getOrNull() ?: run {
                _actionResult.value = StwActionResult.Error("Session expired")
                return@launch
            }
            
            val result = action(session)
            if (result.isSuccess) {
                _actionResult.value = StwActionResult.Success(message.replace("...", " completed!"))
                refreshAll()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Action failed"
                _actionResult.value = StwActionResult.Error(error)
            }
        }
    }
}
