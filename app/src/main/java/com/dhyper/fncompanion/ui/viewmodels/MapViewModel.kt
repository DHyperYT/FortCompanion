package com.dhyper.fncompanion.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhyper.fncompanion.data.models.MapData
import com.dhyper.fncompanion.data.repository.FortniteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class FortniteMapMode(val displayName: String, val subtitle: String) {
    BATTLE_ROYALE("Battle Royale", "Current Season"),
    RELOAD("Fortnite Reload", ""),
    BLITZ("Blitz Royale", ""),
    OG("Fortnite OG", "")
}

data class MapVariant(
    val name: String,
    val imageUrl: String,
    val poiNames: List<String> = emptyList(),
    val fullPois: List<com.dhyper.fncompanion.data.models.PointOfInterest> = emptyList()
)

sealed class MapUiState {
    object Loading : MapUiState()
    data class Success(
        val selectedMode: FortniteMapMode = FortniteMapMode.BATTLE_ROYALE,
        val brMapData: MapData? = null,
        val selectedVariantIndex: Int = 0,
        val showPois: Boolean = true
    ) : MapUiState() {
        fun getVariants(): List<MapVariant> {
            return when (selectedMode) {
                FortniteMapMode.BATTLE_ROYALE -> listOf(
                    MapVariant(
                        name = "Current Island",
                        imageUrl = brMapData?.images?.pois ?: "https://fortnite-api.com/images/map_en.png",
                        poiNames = brMapData?.pois?.map { it.name } ?: emptyList(),
                        fullPois = brMapData?.pois ?: emptyList()
                    )
                )
                FortniteMapMode.RELOAD -> getReloadVariants()
                FortniteMapMode.BLITZ -> getBlitzVariants()
                FortniteMapMode.OG -> getOgHistoryVariants()
            }
        }

        fun getCurrentVariant(): MapVariant = getVariants().getOrElse(selectedVariantIndex) { getVariants().first() }
    }
    data class Error(val message: String) : MapUiState()
}

class MapViewModel(
    private val repository: FortniteRepository = FortniteRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadMap()
    }

    fun loadMap() {
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading
            val mapResult = repository.fetchMap()
            val brData = mapResult.getOrNull()

            _uiState.value = MapUiState.Success(
                selectedMode = FortniteMapMode.BATTLE_ROYALE,
                brMapData = brData,
                showPois = true
            )
        }
    }

    fun selectMapMode(mode: FortniteMapMode) {
        val current = _uiState.value
        if (current is MapUiState.Success) {
            _uiState.value = current.copy(selectedMode = mode, selectedVariantIndex = 0)
        }
    }

    fun selectVariant(index: Int) {
        val current = _uiState.value
        if (current is MapUiState.Success) {
            _uiState.value = current.copy(selectedVariantIndex = index)
        }
    }

    fun togglePois() {
        val current = _uiState.value
        if (current is MapUiState.Success) {
            _uiState.value = current.copy(showPois = !current.showPois)
        }
    }
}

fun getReloadVariants(): List<MapVariant> {
    return listOf(
        MapVariant("Venture", "file:///android_asset/maps/rl_venture.png", listOf("Tilted Towers", "Pleasant Park", "Retail Row", "Lil' Loot Lake", "Lone Lodge", "Dusty Docks", "Snobby Shoals", "Lazy Laps", "Sandy Sheets")),
        MapVariant("Oasis", "file:///android_asset/maps/rl_oasis.png", listOf("Paradise Palms", "Sunburnt Shafts", "Lizard Links", "Adobe Abodes", "Snobby Sands", "Guaco Town", "Fossil Fields", "Shady Springs", "Twisted Trailers")),
        MapVariant("Slurp Rush", "file:///android_asset/maps/rl_slurp.png", listOf("Steamy Stacks", "Slurpy Swamp", "Dirty Docks", "Boomin' Base", "Fort Crumpet", "Lockdown Lighthouse", "Logjam Logging", "Stilt Town")),
        MapVariant("Springfield", "file:///android_asset/maps/rl_springfield.png", listOf("Evergreen Terrace", "Nuclear Power Plant", "Donut District", "Burns Manor", "Cletus' Corn Hole", "Kamp Krusty", "Springfield Town Square", "Springfield Slurpworks", "Corrupted Corners", "The Confidential")),
        MapVariant("Elite Stronghold", "file:///android_asset/maps/rl_stronghold.png", listOf("Chiseled Cubes", "Elite Armory", "Elite Experiments", "Hostile Hold", "Top Tier Training")),
        MapVariant("Squid Grounds", "file:///android_asset/maps/rl_squid.png", listOf("The Labyrinth", "Red Greens", "Square Meals", "Affluent Arrivals", "Costly Condos", "Designer Docks", "Hectic Hedges", "Player's Power")),
        MapVariant("Surf City", "file:///android_asset/maps/rl_surf.png", listOf("Battlewood Boulevard", "Sus Studios", "Divey Dam", "Breaker Beach", "Cashmere Cliffs", "Sweaty Shores")),
        MapVariant("Nitemare Island", "file:///android_asset/maps/rl_nitemare.png", listOf("Spooky Suburbs", "Retail Ruin", "Leech Lake", "Creepy Cabins", "Estranged Estate", "Gravey Gates", "Haunted Hamlet", "Punishment Patch", "Revenge Reels"))
    )
}

fun getBlitzVariants(): List<MapVariant> {
    return listOf(
        MapVariant("Venture", "file:///android_asset/maps/bz_venture.png", listOf("Sandy Sheets", "Lazy Laps", "Retail Row", "Pleasant Park", "Snobby Shoals")),
        MapVariant("Asteria Awakened", "file:///android_asset/maps/bz_asteria.png", listOf("Blitz MEGA City", "Oathbound Citadel", "Anvil Park", "Brutal Bulwark", "Slap Factory")),
        MapVariant("Stranger Things", "file:///android_asset/maps/bz_stranger.png", listOf("Hawkins Heights", "Library Landing", "Stranger Suburbs", "Gnarly Farms")),
        MapVariant("Starfall Island", "file:///android_asset/maps/bz_starfall.png", listOf("Boutique Bay", "Holloway House", "Snooty Strip", "Flowberry Fields", "Ritzy Redux")),
        MapVariant("Stark Island", "file:///android_asset/maps/bz_stark.png", listOf("Stark Industries", "Stark Academy", "Stark Cabin"))
    )
}

fun getOgHistoryVariants(): List<MapVariant> {
    return listOf(
        MapVariant("Season 1", "file:///android_asset/maps/og_s1.png", listOf("Anarchy Acres", "Dusty Depot", "Fatal Fields", "Flush Factory", "Greasy Grove", "Lonely Lodge", "Loot Lake", "Moisty Mire", "Pleasant Park", "Retail Row", "Salty Springs", "Tomato Town", "Wailing Woods")),
        MapVariant("Season 2", "file:///android_asset/maps/og_s2.png", listOf("Anarchy Acres", "Dusty Depot", "Fatal Fields", "Flush Factory", "Greasy Grove", "Haunted Hills", "Junk Junction", "Lonely Lodge", "Loot Lake", "Moisty Mire", "Pleasant Park", "Retail Row", "Salty Springs", "Shifty Shafts", "Snobby Shores", "Tilted Towers", "Tomato Town", "Wailing Woods")),
        MapVariant("Season 3", "file:///android_asset/maps/og_s3.png", listOf("Anarchy Acres", "Dusty Depot", "Fatal Fields", "Flush Factory", "Greasy Grove", "Haunted Hills", "Junk Junction", "Lonely Lodge", "Loot Lake", "Lucky Landing", "Moisty Mire", "Pleasant Park", "Retail Row", "Salty Springs", "Shifty Shafts", "Snobby Shores", "Tilted Towers", "Tomato Town", "Wailing Woods")),
        MapVariant("Season 4", "file:///android_asset/maps/og_s4.png", listOf("Anarchy Acres", "Dusty Divot", "Fatal Fields", "Flush Factory", "Greasy Grove", "Haunted Hills", "Junk Junction", "Lonely Lodge", "Loot Lake", "Lucky Landing", "Moisty Mire", "Pleasant Park", "Retail Row", "Risky Reels", "Salty Springs", "Shifty Shafts", "Snobby Shores", "Tilted Towers", "Tomato Town", "Wailing Woods")),
        MapVariant("Season 5", "file:///android_asset/maps/og_s5.png", listOf("Dusty Divot", "Fatal Fields", "Flush Factory", "Greasy Grove", "Haunted Hills", "Junk Junction", "Lazy Links", "Lonely Lodge", "Loot Lake", "Lucky Landing", "Paradise Palms", "Pleasant Park", "Retail Row", "Risky Reels", "Salty Springs", "Shifty Shafts", "Snobby Shores", "Tilted Towers", "Tomato Temple", "Wailing Woods")),
        MapVariant("Season 6", "file:///android_asset/maps/og_s6.png", listOf("Dusty Divot", "Fatal Fields", "Flush Factory", "Greasy Grove", "Haunted Hills", "Junk Junction", "Lazy Links", "Leaky Lake", "Lonely Lodge", "Lucky Landing", "Paradise Palms", "Pleasant Park", "Retail Row", "Risky Reels", "Salty Springs", "Shifty Shafts", "Snobby Shores", "Tilted Towers", "Tomato Temple", "Wailing Woods")),
        MapVariant("Season 7", "file:///android_asset/maps/og_s7.png", listOf("Dusty Divot", "Fatal Fields", "Frosty Flights", "Happy Hamlet", "Haunted Hills", "Junk Junction", "Lazy Links", "Lonely Lodge", "Loot Lake", "Lucky Landing", "Paradise Palms", "Pleasant Park", "Polar Peak", "Retail Row", "Salty Springs", "Shifty Shafts", "Snobby Shores", "The Block", "Tilted Towers", "Tomato Temple", "Wailing Woods")),
        MapVariant("Season 8", "file:///android_asset/maps/og_s8.png", listOf("Dusty Divot", "Fatal Fields", "Frosty Flights", "Happy Hamlet", "Haunted Hills", "Junk Junction", "Lazy Lagoon", "Lonely Lodge", "Loot Lake", "Lucky Landing", "Paradise Palms", "Pleasant Park", "Polar Peak", "Retail Row", "Salty Springs", "Shifty Shafts", "Snobby Shores", "Sunny Steps", "The Block", "Tilted Towers", "Tomato Temple")),
        MapVariant("Season 9", "file:///android_asset/maps/og_s9.png", listOf("Dusty Divot", "Fatal Fields", "Frosty Flights", "Happy Hamlet", "Haunted Hills", "Junk Junction", "Lazy Lagoon", "Lonely Lodge", "Loot Lake", "Lucky Landing", "Mega Mall", "Neo Tilted", "Paradise Palms", "Pleasant Park", "Polar Peak", "Pressure Plant", "Salty Springs", "Shifty Shafts", "Snobby Shores", "Sunny Steps", "The Block")),
        MapVariant("Season X", "file:///android_asset/maps/og_sx.png", listOf("Dusty Depot", "Fatal Fields", "Frosty Flights", "Gotham City", "Greasy Grove", "Happy Hamlet", "Haunted Hills", "Junk Junction", "Lonely Lodge", "Loot Lake", "Lucky Landing", "Moisty Palms", "Pandora", "Pleasant Park", "Polar Peak", "Pressure Plant", "Retail Row", "Salty Springs", "Shifty Shafts", "Snobby Shores", "Starry Suburbs", "Sunny Steps", "The Block", "Tilted Town"))
    )
}
