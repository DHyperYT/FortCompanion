package com.dhyper.fncompanion.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dhyper.fncompanion.data.db.AuthEntity
import com.dhyper.fncompanion.data.models.PennyProfileResponse
import com.dhyper.fncompanion.data.models.StwHomebaseData
import com.dhyper.fncompanion.data.models.StwMissionAlert
import com.dhyper.fncompanion.data.repository.AuthRepository
import com.dhyper.fncompanion.data.repository.EpicAccountRepository
import com.dhyper.fncompanion.data.repository.PennyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StwUiState {
    object Unauthenticated : StwUiState()
    object Loading : StwUiState()
    data class Success(
        val homebase: StwHomebaseData?,
        val alerts: List<StwMissionAlert> = emptyList(),
        val pennyProfile: PennyProfileResponse? = null
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
    private val pennyRepo: PennyRepository = PennyRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<StwUiState>(StwUiState.Loading)
    val uiState: StateFlow<StwUiState> = _uiState.asStateFlow()

    private val _homebaseData = MutableStateFlow<StwHomebaseData?>(null)
    val homebaseData: StateFlow<StwHomebaseData?> = _homebaseData.asStateFlow()

    private val _pennyProfile = MutableStateFlow<PennyProfileResponse?>(null)
    val pennyProfile: StateFlow<PennyProfileResponse?> = _pennyProfile.asStateFlow()

    private val _actionResult = MutableStateFlow<StwActionResult>(StwActionResult.Idle)
    val actionResult: StateFlow<StwActionResult> = _actionResult.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.value = StwUiState.Loading
            val session = authRepo.ensureActiveSession().getOrNull()
            if (session == null) {
                _uiState.value = StwUiState.Unauthenticated
                return@launch
            }

            try {
                val homebaseResult = epicAccountRepo.fetchStwHomebaseData(getApplication(), session.accessToken, session.accountId)
                val alertsResult = epicAccountRepo.fetchStwWorldInfoFull(getApplication(), session.accessToken)
                val pennyResult = pennyRepo.getProfile(session.accountId)

                if (pennyResult.isSuccess || homebaseResult.isSuccess) {
                    val hb = homebaseResult.getOrNull()
                    val penny = pennyResult.getOrNull()
                    _homebaseData.value = hb
                    _pennyProfile.value = penny
                    _uiState.value = StwUiState.Success(
                        homebase = hb,
                        alerts = alertsResult.getOrDefault(emptyList()),
                        pennyProfile = penny
                    )
                } else {
                    _uiState.value = StwUiState.Error(pennyResult.exceptionOrNull()?.message ?: "Failed to fetch profile")
                }
            } catch (e: Exception) {
                _uiState.value = StwUiState.Error(e.localizedMessage ?: "Unknown STW error")
            }
        }
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
        executeAction("Recycling items...") { session -> epicAccountRepo.recycleItems(session.accessToken, session.accountId, itemIds, getRvn()) }
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
