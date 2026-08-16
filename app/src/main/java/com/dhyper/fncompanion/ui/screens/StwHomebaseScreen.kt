package com.dhyper.fncompanion.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dhyper.fncompanion.data.models.*
import com.dhyper.fncompanion.data.repository.StwMetadataRepository
import com.dhyper.fncompanion.ui.theme.*
import com.dhyper.fncompanion.ui.viewmodels.StwViewModel
import com.dhyper.fncompanion.ui.viewmodels.StwUiState
import com.dhyper.fncompanion.ui.viewmodels.StwActionResult
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StwHomebaseScreen(
    viewModel: StwViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val homebaseData by viewModel.homebaseData.collectAsState()
    val commanderProgress by viewModel.commanderLevelProgress.collectAsState()
    val xpToNext by viewModel.xpToNextLevel.collectAsState()
    val displayName by viewModel.displayName.collectAsState()
    val isFounder by viewModel.isFounder.collectAsState()
    
    val navStack = remember { mutableStateListOf("DASHBOARD") }
    val currentSection = navStack.last()

    var selectedHero by remember { mutableStateOf<StwHero?>(null) }
    var selectedSchematic by remember { mutableStateOf<StwSchematic?>(null) }
    var selectedSurvivor by remember { mutableStateOf<StwSurvivor?>(null) }
    var selectedDefender by remember { mutableStateOf<StwDefender?>(null) }
    var selectedLoadout by remember { mutableStateOf<StwHeroLoadout?>(null) }
    
    val selectedItemIds = remember { mutableStateListOf<String>() }
    var isMultiSelectMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refreshAll() }

    selectedHero?.let { hero ->
        ModalBottomSheet(onDismissRequest = { selectedHero = null }, containerColor = SleekSurfaceVariant) {
            StwHeroDetailContent(hero)
        }
    }
    selectedSchematic?.let { schematic ->
        ModalBottomSheet(onDismissRequest = { selectedSchematic = null }, containerColor = SleekSurfaceVariant) {
            StwSchematicDetailContent(schematic)
        }
    }
    selectedSurvivor?.let { survivor ->
        ModalBottomSheet(onDismissRequest = { selectedSurvivor = null }, containerColor = SleekSurfaceVariant) {
            StwSurvivorDetailContent(survivor)
        }
    }
    selectedDefender?.let { defender ->
        ModalBottomSheet(onDismissRequest = { selectedDefender = null }, containerColor = SleekSurfaceVariant) {
            StwDefenderDetailContent(defender)
        }
    }
    selectedLoadout?.let { loadout ->
        ModalBottomSheet(onDismissRequest = { selectedLoadout = null }, containerColor = SleekSurfaceVariant) {
            StwHeroLoadoutDetailContent(loadout)
        }
    }

    var showRecycleConfirm by remember { mutableStateOf<List<Pair<String, String>>?>(null) }
    var showJunkConfirm by remember { mutableStateOf(false) }
    var showShopConfirm by remember { mutableStateOf(false) }

    showRecycleConfirm?.let { items ->
        AlertDialog(
            onDismissRequest = { showRecycleConfirm = null },
            title = { Text("Recycle / Retire Items", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text("Are you sure you want to recycle/retire ${items.size} item(s)?", fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    items.take(5).forEach { 
                        Text("• ${it.first}", fontSize = 12.sp, color = SleekTextMuted)
                    }
                    if (items.size > 5) Text("...and ${items.size - 5} more", fontSize = 12.sp, color = SleekTextMuted)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ids = items.map { it.second }
                        viewModel.recycleBackpackItems(ids)
                        
                        showRecycleConfirm = null
                        isMultiSelectMode = false
                        selectedItemIds.clear()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { showRecycleConfirm = null }) { Text("Cancel") } },
            containerColor = SleekSurface
        )
    }

    if (showJunkConfirm) {
        AlertDialog(
            onDismissRequest = { showJunkConfirm = false },
            title = { Text("Auto-Recycle Junk", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text("This will recycle all Common/Uncommon weapons and traps, and destroy Tier 1 crafting materials.", fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Inventory will be scanned for items qualifying as 'Junk'.", fontSize = 12.sp, color = SleekTextMuted)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.recycleJunkItems()
                        showJunkConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Run Junk Cleaner") }
            },
            dismissButton = { TextButton(onClick = { showJunkConfirm = false }) { Text("Cancel") } },
            containerColor = SleekSurface
        )
    }

    if (showShopConfirm) {
        AlertDialog(
            onDismissRequest = { showShopConfirm = false },
            title = { Text("Auto-Purchase Storefront", fontWeight = FontWeight.Black) },
            text = {
                Text("Scan the STW Item Shop for free items (like 0-cost X-Ray Llamas) and claim them automatically?", fontSize = 14.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.purchaseEligibleStorefrontItems()
                        showShopConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FortniteGold)
                ) { Text("Check Shop") }
            },
            dismissButton = { TextButton(onClick = { showShopConfirm = false }) { Text("Cancel") } },
            containerColor = SleekSurface
        )
    }

    Scaffold(
        containerColor = SleekBackground,
        topBar = { 
            StwTopBar(
                title = currentSection, 
                onBack = { if (navStack.size > 1) navStack.removeAt(navStack.lastIndex) },
                onNavigateSettings = { navStack.add("SETTINGS") },
                displayName = displayName
            ) 
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val state = uiState) {
                is StwUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                is StwUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(48.dp)); Spacer(Modifier.height(16.dp)); Text("Error: ${state.message}", color = Color.White, textAlign = TextAlign.Center); Spacer(Modifier.height(16.dp)); Button(onClick = { viewModel.refreshAll() }) { Text("Retry") } } }
                is StwUiState.Success -> {
                    homebaseData?.let { data ->
                        AnimatedContent(targetState = currentSection, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "StwNav") { section ->
                            when (section) {
                                "DASHBOARD" -> StwDashboard(data, onNavigate = { navStack.add(it) }, commanderProgress, xpToNext, viewModel, onShowJunk = { showJunkConfirm = true }, onShowShop = { showShopConfirm = true }, displayName = displayName)
                                "ARMORY" -> StwArmorySection(data, onNavigate = { navStack.add(it) })
                                "PEOPLE" -> StwPeopleSection(data, onNavigate = { navStack.add(it) })
                                "COMMAND" -> StwCommandSection(data, onNavigate = { navStack.add(it) })
                                "QUESTS" -> StwDailyQuestsSection(data, viewModel, isFounder)
                                "HEROES" -> StwItemListUnified(data.heroes.sortedByDescending { it.rating }) { _, hero -> 
                                    StwHeroRow(hero = hero, onClick = { selectedHero = hero }) 
                                }
                                "SURVIVORS" -> StwItemListUnified(data.survivors.sortedByDescending { it.rating }) { _, survivor -> 
                                    StwSurvivorRow(survivor = survivor, onClick = { selectedSurvivor = survivor }) 
                                }
                                "DEFENDERS" -> StwItemListUnified(data.defenders.sortedByDescending { it.rating }) { _, defender -> 
                                    StwDefenderRow(defender = defender, onClick = { selectedDefender = defender }) 
                                }
                                "SCHEMATICS" -> StwItemListUnified(data.schematics.sortedByDescending { it.rating }) { _, schematic -> 
                                    StwSchematicRow(schematic = schematic, onClick = { selectedSchematic = schematic }) 
                                }
                                "BACKPACK" -> StwBackpackList(data.inventory.backpack, isMultiSelectMode, { isMultiSelectMode = it }, selectedItemIds, true) { showRecycleConfirm = it.map { item -> item.name to item.id } }
                                "EVENT BACKPACK" -> StwBackpackList(data.inventory.eventBackpack, isMultiSelectMode, { isMultiSelectMode = it }, selectedItemIds, true) { showRecycleConfirm = it.map { item -> item.name to item.id } }
                                "VENTURE BACKPACK" -> StwBackpackList(data.inventory.ventureBackpack, isMultiSelectMode, { isMultiSelectMode = it }, selectedItemIds, false) { }
                                "STORAGE" -> StwBackpackList(data.inventory.storage, isMultiSelectMode, { isMultiSelectMode = it }, selectedItemIds, false) { }
                                "RESOURCES" -> StwResourcesGrid(data.resources)
                                "LLAMAS" -> StwLlamasGrid(data.llamas)
                                "SQUADS" -> StwSquadsList(data.squads) { survivor -> 
                                    selectedSurvivor = survivor
                                }
                                "LOADOUTS" -> StwItemListUnified(data.loadouts) { _, loadout ->
                                    StwHeroLoadoutRow(loadout) { selectedLoadout = loadout }
                                }
                                "ACHIEVEMENTS" -> StwAchievementsList(data.achievements)
                                "SETTINGS" -> StwSettingsSection(viewModel, onShowJunk = { showJunkConfirm = true }, onShowShop = { showShopConfirm = true })
                            }
                        }
                    } ?: Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Profile data unavailable", color = Color.White) }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun StwTopBar(title: String, onBack: () -> Unit, onNavigateSettings: () -> Unit, displayName: String) {
    Column(modifier = Modifier.fillMaxWidth().background(SleekSurfaceVariant)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            if (title != "DASHBOARD") { IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = SleekTextPrimary, modifier = Modifier.size(18.dp)) } }
            else { Spacer(Modifier.width(4.dp)) }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = SleekTextPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            
            if (title == "DASHBOARD") {
                IconButton(onClick = onNavigateSettings, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Settings, null, tint = SleekTextPrimary, modifier = Modifier.size(18.dp))
                }
            }
            
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                Text(displayName.uppercase(), color = SleekTextMuted, fontSize = 9.sp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StwDashboard(data: StwHomebaseData, onNavigate: (String) -> Unit, commanderProgress: Float, xpToNext: Long, viewModel: StwViewModel, onShowJunk: () -> Unit, onShowShop: () -> Unit, displayName: String) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(8.dp))
        
        StwSummaryHeader(data, commanderProgress, xpToNext, displayName)
        Spacer(Modifier.height(16.dp))
        StwQuickResources(data)
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardTile(Modifier.weight(1f), "ARMORY", "file:///android_asset/armory.png", FortniteGold) { onNavigate("ARMORY") }
            DashboardTile(Modifier.weight(1f), "PEOPLE", "file:///android_asset/people.png", SleekEmerald) { onNavigate("PEOPLE") }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardTile(Modifier.weight(1f), "COMMAND", "file:///android_asset/command.png", MaterialTheme.colorScheme.primary) { onNavigate("COMMAND") }
            DashboardTile(Modifier.weight(1f), "QUESTS", "file:///android_asset/quests.png", Color.Magenta) { onNavigate("QUESTS") }
        }
        Spacer(Modifier.height(24.dp))
        data.ventures?.let { StwVenturesCard(it) }
        Spacer(Modifier.height(12.dp))
        StwFortStatsCard(data.research)
        Spacer(Modifier.height(12.dp))
        StwAchievementsSummary(data.achievements) { onNavigate("ACHIEVEMENTS") }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun StwSummaryHeader(data: StwHomebaseData, commanderProgress: Float, xpToNext: Long, displayName: String) {
    val activeLoadout = data.loadouts.find { it.isActive }
    val commander = activeLoadout?.commander
    val commanderLevel = data.commanderLevel
    val cbLevel = data.collectionBook?.level ?: 0

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurface), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), Color.Transparent))).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                    if (commander != null) {
                        AsyncImage(model = resolvePennyUrl(commander.iconUrl), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Person, null, tint = SleekTextMuted, modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("COMMANDER LVL $commanderLevel", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(displayName.uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Book, null, tint = FortniteGold, modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Collection Book: $cbLevel", fontSize = 11.sp, color = SleekTextMuted)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StwMiniStat("Matches", data.matchesPlayed.toString())
                StwMiniStat("Zones", data.zonesCompleted.toString())
            }

            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { commanderProgress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = SleekBackground
            )
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Commander XP Progress", fontSize = 10.sp, color = SleekTextMuted)
                Text(if (xpToNext > 0) "Next Level: ${"%,d".format(xpToNext)} XP" else "Max Level", fontSize = 10.sp, color = SleekTextMuted)
            }
        }
    }
}

@Composable
fun StwMiniStat(label: String, value: String, icon: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) {
            AsyncImage(model = resolvePennyUrl(icon), contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(2.dp))
        }
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text(label.uppercase(), fontSize = 8.sp, color = SleekTextMuted, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StwQuickResources(data: StwHomebaseData) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ResourceItem(data.vbucks, "V-Bucks", "file:///android_asset/vbucks.png")
        ResourceItem(data.xrayTickets, "X-Ray", "file:///android_asset/xray.png")
        ResourceItem(data.gold, "Gold", "/images/resources/gold.png")
        
        val peopleXp = data.resources.find { it.templateId.lowercase().contains("peoplexp") }?.quantity ?: 0L
        val schematicXp = data.resources.find { it.templateId.lowercase().contains("schematicxp") }?.quantity ?: 0L
        
        ResourceItem(peopleXp, "People XP", "/images/resources/hero_xp.png")
        ResourceItem(schematicXp, "Schematic XP", "/images/resources/schematic_xp.png")
    }
}

@Composable
fun ResourceItem(qty: Long, label: String, icon: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = resolvePennyUrl(icon), contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        val text = when {
            qty >= 1_000_000 -> "%.1fM".format(qty / 1_000_000f)
            qty >= 1_000 -> "%.1fK".format(qty / 1_000f)
            else -> qty.toString()
        }
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun StwVenturesCard(ventures: StwVentures) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("VENTURES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(ventures.seasonName.uppercase(), fontSize = 10.sp, color = Color.Yellow, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Level ${ventures.level}", color = Color.White, fontWeight = FontWeight.Bold)
                Text("${ventures.rewardsClaimed} Rewards", color = FortniteGold, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            val progress = if (ventures.nextLevelXp > 0) ventures.xp.toFloat() / ventures.nextLevelXp.toFloat() else 1f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = SleekBackground,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Text("${"%,d".format(ventures.nextLevelXp - ventures.xp)} XP to level up", fontSize = 10.sp, color = SleekTextMuted, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun StwFortStatsCard(research: StwResearch) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurface), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
        Column(Modifier.padding(16.dp)) {
            Text("F.O.R.T. STATS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekTextMuted)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatTile("Fortitude", research.fortitude, "/images/base/fortitude.png")
                StatTile("Offense", research.offense, "/images/base/offense.png")
                StatTile("Resistance", research.resistance, "/images/base/resistance.png")
                StatTile("Tech", research.technology, "/images/base/tech.png")
            }
        }
    }
}

@Composable
fun StatTile(label: String, value: Int, icon: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(model = resolvePennyUrl(icon), contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(value.toString(), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text(label.uppercase(), fontSize = 8.sp, color = SleekTextMuted, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StwArmorySection(data: StwHomebaseData, onNavigate: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp).verticalScroll(rememberScrollState())) {
        CommandMenuTile("SCHEMATICS", "${data.schematics.size} schematics", "file:///android_asset/schematics.png", FortniteGold) { onNavigate("SCHEMATICS") }
        CommandMenuTile("BACKPACK", "Carried items and materials", "file:///android_asset/backpack.png", MaterialTheme.colorScheme.primary) { onNavigate("BACKPACK") }
        CommandMenuTile("VENTURE BACKPACK", "Venture season items", "file:///android_asset/ventures.png", Color(0xFFFFC107)) { onNavigate("VENTURE BACKPACK") }
        CommandMenuTile("STORAGE", "Storm Shield storage", "file:///android_asset/storage.png", SleekEmerald) { onNavigate("STORAGE") }
    }
}

@Composable
fun StwPeopleSection(data: StwHomebaseData, onNavigate: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp).verticalScroll(rememberScrollState())) {
        CommandMenuTile("HEROES", "${data.heroes.size} items", "file:///android_asset/heroes.png", MaterialTheme.colorScheme.primary) { onNavigate("HEROES") }
        CommandMenuTile("SURVIVORS", "${data.survivors.size} items", "file:///android_asset/survivors.png", SleekEmerald) { onNavigate("SURVIVORS") }
        CommandMenuTile("DEFENDERS", "${data.defenders.size} items", "file:///android_asset/defenders.png", Color.LightGray) { onNavigate("DEFENDERS") }
    }
}

@Composable
fun StwCommandSection(data: StwHomebaseData, onNavigate: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp).verticalScroll(rememberScrollState())) {
        CommandMenuTile("LOADOUTS", "${data.loadouts.size} hero loadouts", "file:///android_asset/command.png", MaterialTheme.colorScheme.primary) { onNavigate("LOADOUTS") }
        CommandMenuTile("SQUADS", "Manage survivor teams", "file:///android_asset/survivors.png", SleekEmerald) { onNavigate("SQUADS") }
    }
}

@Composable
fun StwDailyQuestsSection(data: StwHomebaseData, viewModel: StwViewModel, isFounder: Boolean) {
    val dailyQuests = data.dailyQuests

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (dailyQuests.isNotEmpty()) {
            item { Text("DAILY QUESTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
            items(dailyQuests) { quest ->
                StwQuestItemRow(quest, viewModel, isFounder)
            }
        } else {
            item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { Text("No daily quests active", color = SleekTextMuted) } }
        }
    }
}

@Composable
fun StwQuestItemRow(quest: FortniteQuest, viewModel: StwViewModel, isFounder: Boolean = false) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurface)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (quest.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (quest.isCompleted) SleekEmerald else SleekTextMuted)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(quest.name, fontWeight = FontWeight.Bold, color = Color.White)
                    quest.description?.let { Text(it, fontSize = 11.sp, color = SleekTextMuted) }
                }

                if (quest.rewardMtx > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
                        if (isFounder) {
                            AsyncImage(model = "file:///android_asset/vbucks.png", contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(2.dp))
                        }
                        AsyncImage(model = "file:///android_asset/xray.png", contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${quest.rewardMtx}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
                
                if (!quest.isCompleted) {
                    IconButton(onClick = { viewModel.rerollDailyQuest(quest.id) }) {
                        Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
            
            if (!quest.isCompleted && quest.target > 0) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { quest.progress.toFloat() / quest.target.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(4.dp).padding(start = 36.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = SleekBackground,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Text("${quest.progress} / ${quest.target}", fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(start = 36.dp, top = 2.dp))
            }
        }
    }
}

@Composable
fun <T> StwItemListUnified(list: List<T>, itemContent: @Composable (Int, T) -> Unit) {
    if (list.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No items found", color = SleekTextMuted) }
    else {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(list) { index, item -> itemContent(index, item) }
        }
    }
}

@Composable
fun StwBackpackList(
    items: List<StwInventoryItem>,
    isMultiSelectMode: Boolean,
    onMultiSelectModeChange: (Boolean) -> Unit,
    selectedItems: MutableList<String>,
    canRecycle: Boolean = true,
    onRecycleRequest: (List<StwInventoryItem>) -> Unit
) {
    var selectedTab by remember { mutableStateOf("RANGED") }
    val visibleItems = items.filter { !StwMetadataRepository.isInternalItem(it.templateId) }
    val ranged = visibleItems.filter { it.type == "Ranged" }
    val melee = visibleItems.filter { it.type == "Melee" }
    val traps = visibleItems.filter { it.type == "Trap" }
    val resources = visibleItems.filter { it.type == "Material" || it.type == "Item" || it.type == "Ammo" }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(SleekSurfaceVariant).padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
             BackpackTabIcon(Icons.Default.AdsClick, "RANGED", selectedTab == "RANGED") { selectedTab = "RANGED" }
             BackpackTabIcon(Icons.Default.Hardware, "MELEE", selectedTab == "MELEE") { selectedTab = "MELEE" }
             BackpackTabIcon(Icons.Default.GridView, "TRAPS", selectedTab == "TRAPS") { selectedTab = "TRAPS" }
             BackpackTabIcon(Icons.Default.Inventory2, "RESOURCES", selectedTab == "RESOURCES") { selectedTab = "RESOURCES" }
        }

        if (isMultiSelectMode || selectedItems.isNotEmpty()) {
            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${selectedItems.size} items selected", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    if (canRecycle) {
                        TextButton(onClick = { onRecycleRequest(visibleItems.filter { it.id in selectedItems }) }) {
                            Text("RECYCLE", color = Color.Red, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = { 
                        onMultiSelectModeChange(false)
                        selectedItems.clear()
                    }) { Icon(Icons.Default.Close, null, tint = SleekTextMuted, modifier = Modifier.size(16.dp)) }
                }
            }
        }
        
        val filteredList = when(selectedTab) {
            "RANGED" -> ranged
            "MELEE" -> melee
            "TRAPS" -> traps
            "RESOURCES" -> resources
            else -> emptyList()
        }.sortedWith(compareByDescending<StwInventoryItem> { it.rating }.thenByDescending { it.level }.thenByDescending { it.quantity })

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filteredList) { item ->
                StwBackpackRow(
                    item = item,
                    isSelected = selectedItems.contains(item.id),
                    isMultiSelectMode = isMultiSelectMode,
                    canRecycle = canRecycle,
                    onToggleSelect = { if (selectedItems.contains(item.id)) selectedItems.remove(item.id) else selectedItems.add(item.id) },
                    onLongPress = { if (canRecycle) { onMultiSelectModeChange(true); selectedItems.add(item.id) } },
                    onRecycleClick = { onRecycleRequest(listOf(item)) }
                )
            }
        }
    }
}

@Composable
fun BackpackTabIcon(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.alpha(if (isSelected) 1f else 0.4f)) {
        Icon(icon, null, tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun StwBackpackRow(
    item: StwInventoryItem,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    canRecycle: Boolean = true,
    onToggleSelect: () -> Unit,
    onLongPress: () -> Unit,
    onRecycleClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { if (isMultiSelectMode) onToggleSelect() }, onLongClick = onLongPress), colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else SleekSurface), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else SleekSurfaceBorder)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isMultiSelectMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() }, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary, uncheckedColor = SleekTextMuted))
                Spacer(Modifier.width(8.dp))
            }
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(getRarityColor(item.rarity).copy(alpha = 0.1f)).border(1.dp, getRarityColor(item.rarity).copy(alpha = 0.5f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                AsyncImage(model = resolvePennyUrl(item.iconUrl), contentDescription = null, modifier = Modifier.fillMaxSize().padding(4.dp), contentScale = ContentScale.Fit)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Lvl ${item.level}", fontSize = 10.sp, color = SleekTextMuted)
                    if (item.rating > 0) { Spacer(Modifier.width(8.dp)); Text("PL ${item.rating}", fontSize = 10.sp, color = FortniteGold, fontWeight = FontWeight.Bold) }
                }
            }
            if (!isMultiSelectMode && canRecycle) { IconButton(onClick = onRecycleClick) { Icon(Icons.Default.Delete, "Recycle", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(18.dp)) } }
            Column(horizontalAlignment = Alignment.End) {
                Text("x${item.quantity}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                item.durability?.let { val pct = it.coerceIn(0f, 100f).toInt(); Text("$pct%", fontSize = 9.sp, color = if (pct < 20) Color.Red else SleekEmerald) }
            }
        }
    }
}

fun getRarityColor(rarity: String): Color {
    val r = rarity.lowercase()
    return when {
        r.contains("mythic") || r.contains("ur") -> Color(0xFFD33133)
        r.contains("legendary") || r.contains("sr") -> Color(0xFFD2AC67)
        r.contains("epic") || r.contains("vr") -> Color(0xFFB15DFF)
        r.contains("rare") || r.contains("r") -> Color(0xFF49A0FF)
        r.contains("uncommon") || r.contains("uc") -> Color(0xFF60AA3D)
        r.contains("common") || r.contains("c") -> Color(0xFFB3B3B3)
        else -> Color.Gray
    }
}

@Composable
fun StwItemRowTemplate(
    name: String, 
    subtext: String, 
    rarity: String, 
    imageUrl: String?, 
    classIcon: String? = null,
    extraIcon: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, SleekSurfaceBorder)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(getRarityColor(rarity).copy(alpha = 0.1f)).border(1.dp, getRarityColor(rarity).copy(alpha = 0.5f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                AsyncImage(model = resolvePennyUrl(imageUrl), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Spacer(Modifier.width(12.dp))
            if (classIcon != null) {
                AsyncImage(model = resolvePennyUrl(classIcon), contentDescription = null, modifier = Modifier.size(20.dp).alpha(0.8f))
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, color = SleekTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtext, fontSize = 12.sp, color = SleekTextMuted)
            }
            if (extraIcon != null) {
                AsyncImage(model = resolvePennyUrl(extraIcon), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }

            Text(rarity.take(3).uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = getRarityColor(rarity))
        }
    }
}

@Composable
fun StwHeroRow(hero: StwHero, onClick: () -> Unit) {
    StwItemRowTemplate(
        name = hero.name,
        subtext = "PL ${hero.rating} • Lvl ${hero.level}",
        rarity = hero.rarity,
        imageUrl = hero.iconUrl,
        classIcon = "file:///android_asset/${hero.classType}.png",
        onClick = onClick
    )
}

@Composable
fun StwSurvivorRow(survivor: StwSurvivor, onClick: () -> Unit) {
    StwItemRowTemplate(name = survivor.name, subtext = "PL ${survivor.rating} • Lvl ${survivor.level} ${survivor.personality ?: ""}", rarity = survivor.rarity, imageUrl = survivor.iconUrl, onClick = onClick)
}

@Composable
fun StwDefenderRow(defender: StwDefender, onClick: () -> Unit) {
    StwItemRowTemplate(name = defender.name, subtext = "PL ${defender.rating} • Lvl ${defender.level} ${defender.type}", rarity = defender.rarity, imageUrl = defender.iconUrl, onClick = onClick)
}

@Composable
fun StwSchematicRow(schematic: StwSchematic, onClick: () -> Unit) {
    StwItemRowTemplate(name = schematic.name, subtext = "PL ${schematic.rating} • Lvl ${schematic.level} ${schematic.type}", rarity = schematic.rarity, imageUrl = schematic.iconUrl, onClick = onClick)
}

@Composable
fun StwHeroDetailContent(hero: StwHero) {
    Column(Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = resolvePennyUrl(hero.iconUrl), contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(16.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = "file:///android_asset/${hero.classType}.png", contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(hero.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = SleekTextPrimary)
                }
                Text("${hero.rarity.uppercase()} ${hero.classType.uppercase()}", color = getRarityColor(hero.rarity), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                hero.gender?.let { Text(it.uppercase(), color = SleekTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DetailStatCard("Level", hero.level.toString(), Modifier.weight(1f))
            DetailStatCard("Power", hero.rating.toString(), Modifier.weight(1f))
        }
        if (hero.abilities.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("ABILITIES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            hero.abilities.forEach { Text("• $it", color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp)) }
        }
        if (hero.perks.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("PERKS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            hero.perks.forEach { Text("• $it", color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp)) }
        }
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
fun StwSchematicDetailContent(schematic: StwSchematic) {
    Column(Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = resolvePennyUrl(schematic.iconUrl), contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(schematic.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = SleekTextPrimary)
                Text(schematic.rarity.uppercase(), color = getRarityColor(schematic.rarity), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                schematic.damageType?.let { Text(it.uppercase(), color = FortniteGold, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DetailStatCard("Power", schematic.rating.toString(), Modifier.weight(1f))
            DetailStatCard("Level", schematic.level.toString(), Modifier.weight(1f))
        }
        schematic.durability?.let { Spacer(Modifier.height(16.dp)); DetailInfoRow("Durability", "${it.toInt()}%") }
        if (schematic.perks.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("PERKS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            schematic.perks.forEach { Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(it.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text(it.rarity.uppercase(), color = getRarityColor(it.rarity), fontSize = 10.sp, fontWeight = FontWeight.Black) } }
        } else if (schematic.alterations.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("ALTERATIONS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            schematic.alterations.forEach { Text("• ${it.replace("Alteration:", "").replace("aid_att_", "").replace("_", " ").uppercase()}", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp)) }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun StwSurvivorDetailContent(survivor: StwSurvivor) {
    Column(Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = resolvePennyUrl(survivor.iconUrl), contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(survivor.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = SleekTextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(survivor.rarity.uppercase(), color = getRarityColor(survivor.rarity), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    if (survivor.isLead) { Spacer(Modifier.width(8.dp)); Box(Modifier.background(FortniteGold, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("LEAD", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black) } }
                }
                survivor.gender?.let { Text(it.uppercase(), color = SleekTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DetailStatCard("Level", survivor.level.toString(), Modifier.weight(1f))
            DetailStatCard("Power", survivor.rating.toString(), Modifier.weight(1f))
        }
        survivor.personality?.let { Spacer(Modifier.height(24.dp)); Text("PERSONALITY", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary); Text(it, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        survivor.setBonus?.let { Spacer(Modifier.height(16.dp)); Text("SET BONUS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary); Text(it, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        if (survivor.isLead && survivor.synergy != null) { Spacer(Modifier.height(16.dp)); Text("LEADER SYNERGY", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary); Text(survivor.synergy, color = SleekEmerald, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun StwDefenderDetailContent(defender: StwDefender) {
    Column(Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = resolvePennyUrl(defender.iconUrl), contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(defender.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = SleekTextPrimary)
                Text("${defender.rarity.uppercase()} ${defender.type.uppercase()}", color = getRarityColor(defender.rarity), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DetailStatCard("Level", defender.level.toString(), Modifier.weight(1f))
            DetailStatCard("Power", defender.rating.toString(), Modifier.weight(1f))
        }
        if (defender.perks.isNotEmpty()) { Spacer(Modifier.height(24.dp)); Text("PERKS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); defender.perks.forEach { Text("• $it", color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp)) } }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun StwHeroLoadoutRow(loadout: StwHeroLoadout, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = SleekSurface), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, if (loadout.isActive) MaterialTheme.colorScheme.primary else SleekSurfaceBorder)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(getRarityColor(loadout.commander?.rarity ?: "Common").copy(alpha = 0.1f)).border(1.dp, getRarityColor(loadout.commander?.rarity ?: "Common").copy(alpha = 0.5f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                AsyncImage(model = resolvePennyUrl(loadout.commander?.iconUrl), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(loadout.name.uppercase(), fontWeight = FontWeight.Black, color = SleekTextPrimary)
                Text(loadout.commander?.name ?: "No Commander", fontSize = 12.sp, color = Color.White)
                if (loadout.teamPerkName != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = resolvePennyUrl(loadout.teamPerkIcon ?: getStwTeamPerkIcon(loadout.teamPerkName)), contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(loadout.teamPerkName, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (loadout.isActive) { Box(Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) { Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black) } }
        }
    }
}

@Composable
fun StwHeroLoadoutDetailContent(loadout: StwHeroLoadout) {
    Column(Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text(loadout.name.uppercase(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = SleekTextPrimary)
        Spacer(Modifier.height(16.dp))
        Text("COMMANDER", fontSize = 11.sp, fontWeight = FontWeight.Black, color = SleekTextMuted)
        StwHeroBasicTile(loadout.commander)
        Spacer(Modifier.height(24.dp))
        Text("SUPPORT TEAM", fontSize = 11.sp, fontWeight = FontWeight.Black, color = SleekTextMuted)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { loadout.support.forEach { StwHeroBasicTile(it) } }
        Spacer(Modifier.height(24.dp))
        Text("TEAM PERK", fontSize = 11.sp, fontWeight = FontWeight.Black, color = SleekTextMuted)
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = SleekSurface), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = resolvePennyUrl(loadout.teamPerkIcon ?: getStwTeamPerkIcon(loadout.teamPerkName)), contentDescription = null, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Text(loadout.teamPerkName ?: "No Team Perk", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("GADGETS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = SleekTextMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            StwGadgetTile(loadout.gadgetNames.getOrNull(0), loadout.gadgetIcons.getOrNull(0), Modifier.weight(1f))
            StwGadgetTile(loadout.gadgetNames.getOrNull(1), loadout.gadgetIcons.getOrNull(1), Modifier.weight(1f))
        }
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
fun StwHeroBasicTile(hero: StwHero?) {
    hero?.let {
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = SleekSurface), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(getRarityColor(it.rarity).copy(alpha = 0.1f)).border(1.dp, getRarityColor(it.rarity).copy(alpha = 0.5f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    AsyncImage(model = resolvePennyUrl(it.iconUrl), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(it.name, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("PL ${it.rating} • ${it.classType}", fontSize = 11.sp, color = getRarityColor(it.rarity))
                }
            }
        }
    } ?: run {
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = SleekBackground.copy(alpha = 0.5f)), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, SleekSurfaceBorder.copy(alpha = 0.3f))) {
            Box(Modifier.padding(12.dp).height(40.dp), contentAlignment = Alignment.CenterStart) { Text("EMPTY SLOT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekTextMuted) }
        }
    }
}

@Composable
fun StwGadgetTile(name: String?, iconUrl: String?, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = SleekSurface), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, SleekSurfaceBorder.copy(alpha = 0.5f))) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(model = resolvePennyUrl(iconUrl ?: getStwGadgetIcon(name)), contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(name ?: "Empty", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun DetailStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = SleekSurface), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White); Text(label, fontSize = 12.sp, color = SleekTextMuted) }
    }
}

@Composable
fun DetailInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = SleekTextMuted)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
    }
}

@Composable
fun StwStormShieldStatusCard(outposts: List<StwOutpost>) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurface), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
        Column(Modifier.padding(16.dp)) {
            Text("STORM SHIELD STATUS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val zones = listOf("Stonewood", "Plankerton", "Canny Valley", "Twine Peaks")
                zones.forEach { zoneName ->
                    val outpost = outposts.find { it.name.contains(zoneName, true) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(text = if (outpost != null) "LVL ${outpost.level}" else "--", fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (outpost != null) Color.White else SleekTextMuted)
                        Text(text = zoneName.substringBefore(" ").uppercase(), fontSize = 8.sp, color = if (outpost != null) FortniteGold else SleekTextMuted, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StwZoneStormShieldDetail(outposts: List<StwOutpost>, zoneName: String) {
    val outpost = outposts.find { it.name.contains(zoneName, ignoreCase = true) }
    if (outpost == null) { Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Storm Shield data for $zoneName not found", color = SleekTextMuted) } }
    else {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurface), border = BorderStroke(1.dp, SleekSurfaceBorder)) { Column(Modifier.padding(16.dp)) { Text(outpost.name.uppercase(), fontWeight = FontWeight.Black, color = Color.White, fontSize = 22.sp); Text("SSD LEVEL ${outpost.level}", fontSize = 14.sp, color = FortniteGold, fontWeight = FontWeight.Bold); if (outpost.enduranceWave > 0) { Spacer(Modifier.height(8.dp)); Text("Highest Endurance: Wave ${outpost.enduranceWave}", fontSize = 12.sp, color = SleekEmerald) } } } }
            if (outpost.amplifiers.isNotEmpty()) { item { Text("AMPLIFIERS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }; items(outpost.amplifiers) { amp -> Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Security, null, tint = SleekEmerald, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Column { Text(amp.buildingTag.substringAfterLast(".").replace("_", " ").uppercase(), fontWeight = FontWeight.Bold, color = Color.White); Text("Placed at ${amp.placedTag.substringAfterLast(".")}", fontSize = 11.sp, color = SleekTextMuted) } } } } }
        }
    }
}

@Composable
fun StwAccountActionsCard(viewModel: StwViewModel, onShowJunk: () -> Unit, onShowShop: () -> Unit) {
    val actionResult by viewModel.actionResult.collectAsState()
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurface), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("MANUAL ACTIONS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (actionResult is StwActionResult.Loading) { CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onShowJunk, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)), shape = RoundedCornerShape(8.dp), enabled = actionResult !is StwActionResult.Loading) { Text("RECYCLE JUNK", fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.Bold) }
                Button(onClick = onShowShop, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = FortniteGold.copy(alpha = 0.2f)), shape = RoundedCornerShape(8.dp), enabled = actionResult !is StwActionResult.Loading) { Text("CLAIM LLAMAS", fontSize = 10.sp, color = FortniteGold, fontWeight = FontWeight.Bold) }
            }
            AnimatedVisibility(visible = actionResult !is StwActionResult.Idle) { Column { Spacer(Modifier.height(12.dp)); Surface(color = when (actionResult) { is StwActionResult.Success -> SleekEmerald.copy(alpha = 0.1f); is StwActionResult.Error -> Color.Red.copy(alpha = 0.1f); else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) }, shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth()) { Text(text = when (val res = actionResult) { is StwActionResult.Success -> "✓ ${res.message}"; is StwActionResult.Error -> "✗ ${res.message}"; is StwActionResult.Loading -> "Working..."; else -> "" }, modifier = Modifier.padding(8.dp), fontSize = 11.sp, color = when (actionResult) { is StwActionResult.Success -> SleekEmerald; is StwActionResult.Error -> Color.Red; else -> MaterialTheme.colorScheme.primary }, fontWeight = FontWeight.Bold) }; if (actionResult is StwActionResult.Success || actionResult is StwActionResult.Error) { TextButton(onClick = { viewModel.clearActionResult() }, modifier = Modifier.align(Alignment.End)) { Text("Dismiss", fontSize = 10.sp, color = SleekTextMuted) } } } }
        }
    }
}

@Composable
fun StwSettingsSection(viewModel: StwViewModel, onShowJunk: () -> Unit, onShowShop: () -> Unit) {
    val autoRecycle by viewModel.autoRecycleJunk.collectAsState()
    val autoClaim by viewModel.autoClaimLlamas.collectAsState()
    val vbucksEnabled by viewModel.vbucksAlertsEnabled.collectAsState()
    val vbucksTime by viewModel.vbucksAlertTime.collectAsState()
    val autoTime by viewModel.stwAutomationTime.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current

    val vbucksTimePicker = android.app.TimePickerDialog(
        context,
        { _, hour, min ->
            val time = String.format(java.util.Locale.US, "%02d:%02d", hour, min)
            viewModel.updateVBucksAlertTime(time)
            com.dhyper.fncompanion.worker.VBucksAlertReceiver.scheduleNextAlarm(context)
        },
        vbucksTime.split(":").getOrNull(0)?.toIntOrNull() ?: 0,
        vbucksTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0,
        true
    )

    val autoTimePicker = android.app.TimePickerDialog(
        context,
        { _, hour, min ->
            val time = String.format(Locale.US, "%02d:%02d", hour, min)
            viewModel.updateAutomationTime(time)
            com.dhyper.fncompanion.worker.StwAutomationReceiver.scheduleNextAlarm(context)
        },
        autoTime.split(":").getOrNull(0)?.toIntOrNull() ?: 0,
        autoTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0,
        true
    )

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("STW SETTINGS", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
        
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurface), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = FortniteGold, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Account linking is required for these actions to run correctly.", fontSize = 12.sp, color = SleekTextMuted)
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurface), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("AUTOMATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                StwPreferenceSwitch(
                    label = "Auto Recycle Junk",
                    subtitle = "Recycle Common/Uncommon and T1 mats.",
                    checked = autoRecycle,
                    onCheckedChange = { viewModel.setAutoRecycleJunk(it) }
                )
                
                Divider(color = SleekSurfaceBorder)
                
                StwPreferenceSwitch(
                    label = "Auto Claim Free Llamas",
                    subtitle = "Claim 0-cost llamas from shop.",
                    checked = autoClaim,
                    onCheckedChange = { viewModel.setAutoClaimLlamas(it) }
                )

                Button(
                    onClick = { autoTimePicker.show() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekBackground.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SleekSurfaceBorder)
                ) {
                    Icon(Icons.Default.Alarm, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Automation Time: $autoTime", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurface), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("NOTIFICATIONS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                StwPreferenceSwitch(
                    label = "V-Bucks Alerts",
                    subtitle = "Get notified when V-Bucks missions are active.",
                    checked = vbucksEnabled,
                    onCheckedChange = { 
                        viewModel.updateVBucksAlerts(it)
                        com.dhyper.fncompanion.worker.VBucksAlertReceiver.scheduleNextAlarm(context)
                    }
                )

                if (vbucksEnabled) {
                    Button(
                        onClick = { vbucksTimePicker.show() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekBackground.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SleekSurfaceBorder)
                    ) {
                        Icon(Icons.Default.Alarm, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Check Time: $vbucksTime", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        StwAccountActionsCard(viewModel, onShowJunk, onShowShop)
        
        Spacer(Modifier.weight(1f))
        Text("Automation runs daily at your set automation time. V-Bucks checks run at their own set time.", fontSize = 11.sp, color = SleekTextMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun StwPreferenceSwitch(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Bold, color = SleekTextPrimary, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp, color = SleekTextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        )
    }
}

@Composable
fun DashboardTile(modifier: Modifier = Modifier, label: String, icon: String, color: Color, onClick: () -> Unit) {
    Card(modifier = modifier.height(90.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = SleekSurface), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(model = resolvePennyUrl(icon), contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black, color = SleekTextPrimary)
        }
    }
}

@Composable
fun CommandMenuTile(title: String, subtitle: String, icon: Any, color: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = SleekSurface), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                if (icon is ImageVector) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                } else {
                    AsyncImage(model = icon, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp); Text(subtitle, fontSize = 11.sp, color = SleekTextMuted) }
            Icon(Icons.Default.ChevronRight, null, tint = SleekTextMuted)
        }
    }
}

@Composable
fun StwResourcesGrid(resources: List<StwResource>) {
    val list = resources.filter { it.quantity > 0 }
        .sortedWith(compareBy({ it.type }, { it.rarity }, { it.name }))
    
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(list) { res ->
            Card(colors = CardDefaults.cardColors(containerColor = SleekSurface), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = resolvePennyUrl(res.iconUrl), contentDescription = null, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(res.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text(res.type.name, fontSize = 10.sp, color = SleekTextMuted)
                    }
                    Text("x${res.quantity}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun StwLlamasGrid(llamas: List<StwLlama>) {
    val grouped = llamas.groupBy { it.templateId }.values.toList()
    if (grouped.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No llamas unopened", color = SleekTextMuted) }
    else {
        LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(grouped) { group ->
                val llama = group.first()
                val count = group.size
                Card(colors = CardDefaults.cardColors(containerColor = SleekSurface), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(model = resolvePennyUrl(llama.iconUrl ?: "/images/resources/llama.png"), contentDescription = null, modifier = Modifier.size(48.dp))
                        Text(count.toString(), fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                        Text(llama.name, fontSize = 9.sp, color = SleekTextMuted, textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun StwSquadsList(squads: List<StwSurvivorSquad>, onSurvivorClick: (StwSurvivor) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        items(squads) { squad ->
            StwSquadCard(squad, onSurvivorClick)
        }
    }
}

@Composable
fun StwSquadCard(squad: StwSurvivorSquad, onSurvivorClick: (StwSurvivor) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurface), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val squadSlug = when(squad.id.substringAfterLast("_").lowercase()) {
                    "fireteamalpha" -> "fireteam_alpha"
                    "closeassaultsquad" -> "close_assault_squad"
                    "emtsquad" -> "emt_squad"
                    "trainingteam" -> "training_team"
                    "scoutingparty" -> "scouting_party"
                    "gadgeteers" -> "gadgeteers"
                    "thethinktank" -> "the_think_tank"
                    "corpsofengineering" -> "corps_of_engineering"
                    else -> squad.id.substringAfterLast("_").lowercase()
                }
                AsyncImage(
                    model = "file:///android_asset/squads/$squadSlug.png",
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(squad.name.uppercase(), fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp)
                    val memberCount = (if (squad.leadSlot != null) 1 else 0) + squad.memberSlots.filterNotNull().size
                    Text("$memberCount/8 MEMBERS", fontSize = 10.sp, color = if (memberCount >= 8) SleekEmerald else FortniteGold, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth()) { StwSurvivorTile(squad.leadSlot, isLead = true, modifier = Modifier.weight(1f)) { squad.leadSlot?.let { onSurvivorClick(it) } }; Spacer(Modifier.weight(3f)) }
                Spacer(Modifier.height(8.dp))
                val members = squad.memberSlots
                for (row in 0..1) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (col in 0..3) {
                            val idx = row * 4 + col
                            if (idx < 7) {
                                val survivor = members.getOrNull(idx)
                                StwSurvivorTile(survivor, isLead = false, leadPersonality = squad.leadSlot?.personality, modifier = Modifier.weight(1f)) { survivor?.let { onSurvivorClick(it) } }
                            } else { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StwSurvivorTile(survivor: StwSurvivor?, isLead: Boolean, leadPersonality: String? = null, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val rarityColor = getRarityColor(survivor?.rarity ?: "Common")
    Box(modifier = modifier.aspectRatio(0.8f).clip(RoundedCornerShape(8.dp)).background(rarityColor.copy(alpha = 0.15f)).border(1.dp, if (isLead) rarityColor else rarityColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).clickable(enabled = survivor != null) { onClick() }, contentAlignment = Alignment.Center) {
        if (survivor != null) {
            AsyncImage(model = resolvePennyUrl(survivor.iconUrl), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.6f)).padding(vertical = 2.dp), contentAlignment = Alignment.Center) { Text("PL ${survivor.rating}", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White) }
            val matches = !isLead && survivor.personality == leadPersonality
            if (matches || isLead) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(16.dp)
                        .background(if (matches) SleekEmerald else Color.Black.copy(alpha = 0.7f), CircleShape)
                        .border(1.dp, if (matches) SleekEmerald else Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        survivor.personality?.take(1) ?: "",
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.offset(y = (-0.5).dp) // Fine-tune centering
                    )
                }
            }
        } else { Icon(Icons.Default.Add, null, tint = SleekSurfaceBorder, modifier = Modifier.size(20.dp)) }
    }
}

@Composable
fun StwAchievementsSummary(achievements: List<StwAchievement>, onClick: () -> Unit) {
    if (achievements.isEmpty()) return
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = SleekSurface), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ACHIEVEMENTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekTextMuted)
                Icon(Icons.Default.ChevronRight, null, tint = SleekTextMuted)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                achievements.take(8).forEach { ach -> AsyncImage(model = resolvePennyUrl(ach.iconUrl), contentDescription = null, modifier = Modifier.size(40.dp)) }
            }
        }
    }
}

@Composable
fun StwAchievementsList(achievements: List<StwAchievement>) {
    if (achievements.isEmpty()) { Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No achievements found", color = SleekTextMuted) } }
    else {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(achievements) { ach ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurface)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = resolvePennyUrl(ach.iconUrl), contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(ach.name, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${"%,d".format(ach.progress)} / ${"%,d".format(ach.target)}", fontSize = 12.sp, color = SleekTextMuted)
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(progress = { if (ach.target > 0) ach.progress.toFloat() / ach.target.toFloat() else 0f }, modifier = Modifier.fillMaxWidth().height(4.dp), color = MaterialTheme.colorScheme.primary, trackColor = SleekBackground, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                        }
                    }
                }
            }
        }
    }
}

fun resolvePennyUrl(url: String?): String? {
    if (url == null || url.isBlank()) return null
    var cleanUrl = url.trim()
    if (cleanUrl.startsWith("http") || cleanUrl.startsWith("file://")) return cleanUrl
    cleanUrl = cleanUrl.removePrefix("/")
    return "https://pennydb.plingindigo.org/$cleanUrl"
}

fun getStwTeamPerkIcon(name: String?): String? {
    if (name == null) return null
    val slug = name.trim().lowercase().replace(" ", "_").replace(".", "").replace("\"", "")
    return "https://pennydb.plingindigo.org/images/team_perks/$slug.png"
}

fun getStwGadgetIcon(name: String?): String? {
    if (name == null) return null
    val slug = name.trim().lowercase().replace(" ", "_").replace(".", "").replace("\"", "")
    return "https://pennydb.plingindigo.org/images/gadgets/$slug.png"
}
