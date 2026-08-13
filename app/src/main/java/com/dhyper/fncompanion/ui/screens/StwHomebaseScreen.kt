package com.dhyper.fncompanion.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StwHomebaseScreen(
    viewModel: StwViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val pennyProfile by viewModel.pennyProfile.collectAsState()
    
    // Navigation Stack
    val navStack = remember { mutableStateListOf("DASHBOARD") }
    val currentSection = navStack.last()

    // Selection States (Penny Models Only)
    var selectedPennyHero by remember { mutableStateOf<PennyHero?>(null) }
    var selectedPennySchematic by remember { mutableStateOf<PennySchematic?>(null) }
    var selectedPennySurvivor by remember { mutableStateOf<PennySurvivor?>(null) }
    var selectedPennyDefender by remember { mutableStateOf<PennyDefender?>(null) }
    var selectedLoadout by remember { mutableStateOf<PennyLoadout?>(null) }

    LaunchedEffect(Unit) { viewModel.refreshAll() }

    // Detail Overlays
    if (selectedPennyHero != null) ModalBottomSheet(onDismissRequest = { selectedPennyHero = null }, containerColor = SleekSurfaceVariant) { PennyHeroDetailContent(selectedPennyHero!!) }
    if (selectedPennySchematic != null) ModalBottomSheet(onDismissRequest = { selectedPennySchematic = null }, containerColor = SleekSurfaceVariant) { PennySchematicDetailContent(selectedPennySchematic!!) }
    if (selectedPennySurvivor != null) ModalBottomSheet(onDismissRequest = { selectedPennySurvivor = null }, containerColor = SleekSurfaceVariant) { PennySurvivorDetailContent(selectedPennySurvivor!!) }
    if (selectedPennyDefender != null) ModalBottomSheet(onDismissRequest = { selectedPennyDefender = null }, containerColor = SleekSurfaceVariant) { PennyDefenderDetailContent(selectedPennyDefender!!) }
    if (selectedLoadout != null) ModalBottomSheet(onDismissRequest = { selectedLoadout = null }, containerColor = SleekSurfaceVariant) { PennyLoadoutDetailContent(selectedLoadout!!) }

    Scaffold(
        containerColor = SleekBackground,
        topBar = { 
            val summary = pennyProfile?.profileSummary
            PennyTopBar(
                title = currentSection, 
                summary = summary,
                onBack = { if (navStack.size > 1) navStack.removeAt(navStack.lastIndex) }
            ) 
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val state = uiState) {
                is StwUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = SleekCyan) }
                is StwUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(48.dp)); Spacer(Modifier.height(16.dp)); Text("Error: ${state.message}", color = Color.White, textAlign = TextAlign.Center); Spacer(Modifier.height(16.dp)); Button(onClick = { viewModel.refreshAll() }) { Text("Retry") } } }
                is StwUiState.Success -> {
                    pennyProfile?.let { data ->
                        AnimatedContent(targetState = currentSection, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "StwNav") { section ->
                            when (section) {
                                "DASHBOARD" -> PennyDashboard(data, onNavigate = { navStack.add(it) })
                                "ARMORY" -> PennyArmorySection(data, onNavigate = { navStack.add(it) })
                                "PEOPLE" -> PennyPeopleSection(data, onNavigate = { navStack.add(it) })
                                "COMMAND" -> PennyCommandSection(data, onNavigate = { navStack.add(it) })
                                "QUESTS" -> PennyQuestsSection(data)
                                "HEROES" -> PennyItemList(data.heroes ?: emptyMap()) { _, item -> PennyHeroRow(item) { selectedPennyHero = item } }
                                "SURVIVORS" -> PennyItemList(data.survivors ?: emptyMap()) { _, item -> PennySurvivorRow(item) { selectedPennySurvivor = item } }
                                "DEFENDERS" -> PennyItemList(data.defenders ?: emptyMap()) { _, item -> PennyDefenderRow(item) { selectedPennyDefender = item } }
                                "SCHEMATICS" -> PennyItemList(data.schematics ?: emptyMap()) { _, item -> PennySchematicRow(item) { selectedPennySchematic = item } }
                                "RESOURCES" -> PennyResourcesGrid(data.resourcesSummary?.resources ?: emptyMap())
                                "LLAMAS" -> PennyLlamasGrid(data.resourcesSummary?.llamas ?: emptyMap())
                                "SQUADS" -> PennySquadsList(data.squads ?: emptyMap())
                                "LOADOUTS" -> PennyLoadoutsList(data.loadouts?.loadouts ?: emptyList(), data.loadouts?.currentLoadoutGuid) { selectedLoadout = it }
                                "ACHIEVEMENTS" -> PennyAchievementsList(data.achievements ?: emptyMap())
                                "EXPEDITIONS" -> PennyExpeditionsList(data.expeditions ?: emptyMap())
                                "CONNECTED ACCOUNTS" -> PennyConnectedAccounts(data.alternateAccounts ?: emptyList())
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
fun PennyTopBar(title: String, summary: PennyProfileSummary?, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(SleekSurfaceVariant).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (title != "DASHBOARD") { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = SleekTextPrimary) } }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = SleekTextPrimary, modifier = Modifier.weight(1f))
            summary?.let {
                Column(horizontalAlignment = Alignment.End) {
                    Text("PL ${it.powerLevel ?: 1.0}", color = FortniteGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(it.displayName ?: "", color = SleekTextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun PennyDashboard(data: PennyProfileResponse, onNavigate: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(16.dp))
        PennySummaryHeader(data)
        Spacer(Modifier.height(16.dp))
        PennyQuickResources(data.resourcesSummary?.resources)
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardTile(Modifier.weight(1f), "ARMORY", Icons.Default.Hardware, FortniteGold) { onNavigate("ARMORY") }
            DashboardTile(Modifier.weight(1f), "PEOPLE", Icons.Default.Groups, SleekEmerald) { onNavigate("PEOPLE") }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardTile(Modifier.weight(1f), "COMMAND", Icons.Default.Shield, SleekCyan) { onNavigate("COMMAND") }
            DashboardTile(Modifier.weight(1f), "QUESTS", Icons.Default.Assignment, Color.Magenta) { onNavigate("QUESTS") }
        }
        Spacer(Modifier.height(24.dp))
        PennyVenturesCard(data.venturesData)
        Spacer(Modifier.height(12.dp))
        PennyFortStatsCard(data.fortStats)
        Spacer(Modifier.height(12.dp))
        PennyAchievementsSummary(data.achievements) { onNavigate("ACHIEVEMENTS") }
        Spacer(Modifier.height(12.dp))
        CommandMenuTile("CONNECTED ACCOUNTS", "External linking info", Icons.Default.Link, SleekCyan) { onNavigate("CONNECTED ACCOUNTS") }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun PennySummaryHeader(data: PennyProfileResponse) {
    val summary = data.profileSummary
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurface), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).background(Brush.radialGradient(listOf(FortniteGold.copy(alpha = 0.2f), Color.Transparent)), CircleShape).border(2.dp, FortniteGold, CircleShape), contentAlignment = Alignment.Center) {
                Text("${summary?.powerLevel?.toInt() ?: 1}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("COMMANDER LVL ${summary?.commanderLevel ?: 1}", fontSize = 12.sp, color = SleekCyan, fontWeight = FontWeight.Bold)
                Text(summary?.displayName?.uppercase() ?: "PLAYER", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text("Collection Book Level: ${summary?.collectionBookLevel ?: 0}", fontSize = 11.sp, color = SleekTextMuted)
            }
        }
    }
}

@Composable
fun PennyQuickResources(resources: Map<String, PennyResource>?) {
    resources?.let { resMap ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val keys = listOf("gold", "people_xp", "schematic_xp", "vbucks", "x_ray_tickets")
            keys.forEach { key ->
                val r = resMap[key] ?: resMap.values.find { it.name?.lowercase()?.replace(" ", "_")?.contains(key) == true }
                r?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = if (it.image?.startsWith("/") == true) "https://pennydb.net${it.image}" else it.image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        val qty = it.quantity ?: 0L
                        val text = when {
                            qty >= 1_000_000 -> "%.1fM".format(qty / 1_000_000f)
                            qty >= 1_000 -> "%.1fK".format(qty / 1_000f)
                            else -> qty.toString()
                        }
                        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PennyVenturesCard(ventures: PennyVenturesData?) {
    var showQuests by remember { mutableStateOf(false) }
    ventures?.let {
        Card(modifier = Modifier.fillMaxWidth().clickable { if (!it.quests.isNullOrEmpty()) showQuests = !showQuests }, colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("VENTURES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekCyan)
                    Text("NEXT: ${it.nextReward ?: ""}", fontSize = 10.sp, color = Color.Yellow, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Level ${it.currentVentureLevel ?: 1}", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("PL ${it.venturePowerLevel ?: 1}", color = FortniteGold, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (it.currentLevelProgress?.replace("%", "")?.toFloatOrNull() ?: 0f) / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = SleekCyan,
                    trackColor = SleekBackground,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Text("${it.xpNeeded ?: 0} XP to level up", fontSize = 10.sp, color = SleekTextMuted, modifier = Modifier.padding(top = 4.dp))
                
                if (showQuests && !it.quests.isNullOrEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("VENTURE QUESTS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                    val questMap = it.quests
                    val names = questMap.keys.filter { k -> k.endsWith("_name") }.sorted()
                    names.forEach { nameKey ->
                        val base = nameKey.substringBefore("_name")
                        val name = questMap[nameKey]
                        val desc = questMap["${base}_description"]
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Text(name ?: "", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekCyan)
                            Text(desc ?: "", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PennyFortStatsCard(stats: Map<String, PennyFortStat>?) {
    stats?.let {
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurface), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
            Column(Modifier.padding(16.dp)) {
                Text("F.O.R.T. STATS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekTextMuted)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    it.values.forEach { stat ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stat.quantity.toString(), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text(stat.name?.uppercase() ?: "", fontSize = 9.sp, color = SleekTextMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PennyArmorySection(data: PennyProfileResponse, onNavigate: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        CommandMenuTile("SCHEMATICS", "${data.schematics?.size ?: 0} crafting plans", Icons.Default.Build, FortniteGold) { onNavigate("SCHEMATICS") }
        CommandMenuTile("RESOURCES", "${data.resourcesSummary?.resources?.size ?: 0} items", Icons.Default.Inventory2, SleekCyan) { onNavigate("RESOURCES") }
        CommandMenuTile("LLAMAS", "${data.resourcesSummary?.llamas?.size ?: 0} unopened loot", Icons.Default.CardGiftcard, Color.Magenta) { onNavigate("LLAMAS") }
        
        data.profileSummary?.unslotCost?.let { cost ->
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurface), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.Yellow)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("UNSLOT COST", fontSize = 10.sp, color = SleekTextMuted, fontWeight = FontWeight.Bold)
                        Text("$cost Legendary Flux", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PennyPeopleSection(data: PennyProfileResponse, onNavigate: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        CommandMenuTile("HEROES", "${data.heroes?.size ?: 0} items", Icons.Default.Person, SleekCyan) { onNavigate("HEROES") }
        CommandMenuTile("SURVIVORS", "${data.survivors?.size ?: 0} items", Icons.Default.RecordVoiceOver, SleekEmerald) { onNavigate("SURVIVORS") }
        CommandMenuTile("DEFENDERS", "${data.defenders?.size ?: 0} items", Icons.Default.Shield, Color.LightGray) { onNavigate("DEFENDERS") }
        CommandMenuTile("SQUADS", "Manage survivor teams", Icons.Default.Groups, SleekEmerald) { onNavigate("SQUADS") }
        
        data.survivorBonusOverview?.let { bonus ->
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurface), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
                Column(Modifier.padding(16.dp)) {
                    Text("ACTIVE BONUSES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekCyan)
                    Spacer(Modifier.height(12.dp))
                    bonus.activeBonuses?.forEach { b ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(b.bonusName ?: "", fontSize = 13.sp, color = Color.White)
                            Text("+${b.totalBonusPct}%", fontSize = 13.sp, color = SleekEmerald, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PennyCommandSection(data: PennyProfileResponse, onNavigate: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        CommandMenuTile("LOADOUTS", "${data.loadouts?.loadouts?.size ?: 0} hero loadouts", Icons.Default.ViewCarousel, SleekCyan) { onNavigate("LOADOUTS") }
        CommandMenuTile("EXPEDITIONS", "${data.expeditions?.size ?: 0} available", Icons.Default.Explore, Color.Yellow) { onNavigate("EXPEDITIONS") }
    }
}

@Composable
fun PennyQuestsSection(data: PennyProfileResponse) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val categories = listOf(
            "DAILY MISSIONS" to data.dailyMissionData?.values?.toList(),
            "ACTIVE QUESTS" to data.activeQuests?.values?.toList(),
            "VENTURE QUESTS" to data.liveVenturesQuests?.values?.toList(),
            "WEEKLY QUESTS" to data.liveWeeklyQuests?.values?.toList(),
            "WARGAMES" to data.liveWargamesQuests?.values?.toList(),
            "DUNGEONS" to data.liveDungeonsQuests?.values?.toList(),
            "STORM SHIELD" to data.liveStormshieldQuests?.values?.toList(),
            "COMPLETED (RECENT)" to data.completedQuests?.values?.take(10)
        )

        categories.forEach { (title, list) ->
            if (!list.isNullOrEmpty()) {
                item { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekCyan, modifier = Modifier.padding(top = 8.dp)) }
                items(list) { item ->
                    when (item) {
                        is PennyDailyMission -> PennyDailyMissionRow(item)
                        is PennyQuestItem -> PennyQuestItemRow(item, title.contains("COMPLETED"))
                    }
                }
            }
        }
    }
}

@Composable
fun PennyQuestItemRow(quest: PennyQuestItem, isCompleted: Boolean) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurface)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (isCompleted) SleekEmerald else SleekTextMuted)
                Spacer(Modifier.width(12.dp))
                Text(quest.name ?: "Unnamed Quest", fontWeight = FontWeight.Bold, color = Color.White)
            }
            quest.description?.let { desc ->
                Text(desc, fontSize = 11.sp, color = SleekTextMuted, modifier = Modifier.padding(start = 36.dp, top = 4.dp))
                
                // Extraction of [X/Y] progress from description
                val regex = Regex("\\[(\\d+)/(\\d+)]")
                val match = regex.find(desc)
                if (match != null) {
                    val current = match.groupValues[1].toFloatOrNull() ?: 0f
                    val total = match.groupValues[2].toFloatOrNull() ?: 1f
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { if (total > 0) current / total else 0f },
                        modifier = Modifier.fillMaxWidth().height(4.dp).padding(start = 36.dp),
                        color = SleekCyan,
                        trackColor = SleekBackground,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
            if (!isCompleted && !quest.completionData.isNullOrEmpty()) {
                Spacer(Modifier.height(4.dp))
                quest.completionData.forEach { (k, v) ->
                    Text("• ${k.replace("completion_", "").replace("_", " ").uppercase()}: $v", fontSize = 10.sp, color = SleekCyan, modifier = Modifier.padding(start = 36.dp))
                }
            }
        }
    }
}

@Composable
fun <T> PennyItemList(items: Map<String, T>, rowContent: @Composable (String, T) -> Unit) {
    if (items.isEmpty()) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No items found", color = SleekTextMuted) }
    else {
        val sortedList = items.entries.toList().sortedByDescending { 
            when (val v = it.value) {
                is PennyHero -> v.powerLevel ?: 0
                is PennySchematic -> v.powerLevel ?: 0
                is PennySurvivor -> v.powerLevel ?: 0
                is PennyDefender -> v.powerLevel ?: 0
                else -> 0
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { 
            items(sortedList) { entry -> rowContent(entry.key, entry.value) } 
        }
    }
}

@Composable
fun PennyHeroRow(hero: PennyHero, onClick: () -> Unit) { 
    val name = if (hero.name == null || hero.name == "Hero") hero.templateId ?: "Unknown Hero" else hero.name
    PennyItemRowTemplate(
        name = name, 
        subtext = "PL ${hero.powerLevel ?: 1} • Lvl ${hero.attributes?.level ?: 1} ${hero.heroClass ?: ""}", 
        rarity = hero.rarity ?: "Common",
        imageUrl = hero.imageLink,
        classIcon = hero.heroClassImage,
        onClick = onClick
    ) 
}

@Composable
fun PennySchematicRow(schematic: PennySchematic, onClick: () -> Unit) { 
    val name = if (schematic.name == null || schematic.name == "Schematic") schematic.templateId ?: "Unknown Schematic" else schematic.name
    PennyItemRowTemplate(
        name = name, 
        subtext = "PL ${schematic.powerLevel ?: 1}", 
        rarity = schematic.rarity ?: "Common",
        imageUrl = schematic.imageLink,
        classIcon = schematic.classImage,
        onClick = onClick
    ) 
}

@Composable
fun PennySurvivorRow(survivor: PennySurvivor, onClick: () -> Unit) { 
    val name = if (survivor.name == null || survivor.name == "Survivor") survivor.templateId ?: "Survivor" else survivor.name
    PennyItemRowTemplate(
        name = name, 
        subtext = "PL ${survivor.powerLevel ?: 1} • ${survivor.personality ?: ""}", 
        rarity = survivor.rarity ?: "Common",
        imageUrl = survivor.imageLink,
        extraIcon = survivor.personalityImage,
        onClick = onClick
    ) 
}

@Composable
fun PennyDefenderRow(defender: PennyDefender, onClick: () -> Unit) { 
    val name = if (defender.name == null || defender.name == "Defender") defender.templateId ?: "Defender" else defender.name
    PennyItemRowTemplate(
        name = name, 
        subtext = "PL ${defender.powerLevel ?: 1} • ${defender.defenderClass ?: ""}", 
        rarity = defender.rarity ?: "Common",
        imageUrl = defender.imageLink,
        classIcon = defender.classImage,
        onClick = onClick
    ) 
}

@Composable
fun PennyItemRowTemplate(
    name: String, 
    subtext: String, 
    rarity: String, 
    imageUrl: String?, 
    classIcon: String? = null,
    extraIcon: String? = null,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = SleekSurface), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(getRarityColor(rarity).copy(alpha = 0.1f)).border(1.dp, getRarityColor(rarity).copy(alpha = 0.5f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Spacer(Modifier.width(12.dp))
            if (classIcon != null) {
                AsyncImage(model = classIcon, contentDescription = null, modifier = Modifier.size(20.dp).alpha(0.8f))
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, color = SleekTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtext, fontSize = 12.sp, color = SleekTextMuted)
            }
            if (extraIcon != null) {
                AsyncImage(model = extraIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(rarity.take(3).uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = getRarityColor(rarity))
        }
    }
}

@Composable
fun PennyDailyMissionRow(mission: PennyDailyMission) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = if (mission.dailyReward?.startsWith("/") == true) "https://pennydb.net${mission.dailyReward}" else mission.dailyReward, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(mission.name ?: "", fontWeight = FontWeight.Bold, color = Color.White)
                Text(mission.description ?: "", fontSize = 11.sp, color = SleekTextMuted)
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (mission.currentTotal ?: 0).toFloat() / (mission.totalRequired ?: 1).toFloat() },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = SleekCyan,
                    trackColor = SleekBackground,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Text("${mission.currentTotal} / ${mission.totalRequired}", fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
fun PennyResourcesGrid(resources: Map<String, PennyResource>) {
    val list = resources.values.filter { (it.quantity ?: 0) > 0 }.toList().sortedByDescending { it.quantity }
    LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(list) { res ->
            Card(colors = CardDefaults.cardColors(containerColor = SleekSurface), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(model = if (res.image?.startsWith("/") == true) "https://pennydb.net${res.image}" else res.image, contentDescription = null, modifier = Modifier.size(32.dp))
                    val qty = res.quantity ?: 0L
                    val text = when {
                        qty >= 1_000_000 -> "%.1fM".format(qty / 1_000_000f)
                        qty >= 1_000 -> "%.1fK".format(qty / 1_000f)
                        else -> qty.toString()
                    }
                    Text(text, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                    Text(res.name ?: "", fontSize = 8.sp, color = SleekTextMuted, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun PennyLlamasGrid(llamas: Map<String, PennyResource>) {
    val list = llamas.values.filter { (it.quantity ?: 0) > 0 }.toList()
    if (list.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No unopened llamas", color = SleekTextMuted) }
    else {
        LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list) { llama ->
                Card(colors = CardDefaults.cardColors(containerColor = SleekSurface), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(model = if (llama.image?.startsWith("/") == true) "https://pennydb.net${llama.image}" else llama.image, contentDescription = null, modifier = Modifier.size(48.dp))
                        Text(llama.quantity.toString(), fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                        Text(llama.name ?: "", fontSize = 9.sp, color = Color.White, textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun PennySquadsList(squads: Map<String, PennySquad>) {
    if (squads.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No squads found", color = SleekTextMuted) }
    else {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(squads.values.toList()) { squad ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurface), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(squad.squadName ?: "Unnamed Squad", fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.weight(1f))
                            Text("${squad.workerCount}/7", fontSize = 11.sp, color = SleekTextMuted)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Lead: ${squad.leadSurvivor?.name ?: "None"}", color = getRarityColor(squad.leadSurvivor?.rarity ?: "Common"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        squad.activeBonuses?.forEach { bonus ->
                            Text("• ${bonus.bonusName}: +${bonus.totalBonusPct}%", fontSize = 11.sp, color = SleekEmerald)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PennyConnectedAccounts(accounts: List<PennyAlternateAccount>) {
    if (accounts.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No connected accounts found", color = SleekTextMuted) }
    else {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(accounts) { acc ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurface)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(acc.displayName ?: "Epic Player", fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        acc.externalAuths?.forEach { (type, auth) ->
                            Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(when(type) { "psn" -> Icons.Default.Gamepad; "xbl" -> Icons.Default.Gamepad; "steam" -> Icons.Default.Computer; "nintendo" -> Icons.Default.Gamepad; else -> Icons.Default.Link }, null, tint = SleekCyan, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("${type.uppercase()}: ${auth.externalDisplayName ?: "Connected"}", fontSize = 13.sp, color = SleekTextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PennyLoadoutsList(loadouts: List<PennyLoadout>, currentGuid: String?, onClick: (PennyLoadout) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(loadouts) { loadout ->
            val isActive = loadout.guid == currentGuid
            Card(modifier = Modifier.fillMaxWidth().clickable { onClick(loadout) }, colors = CardDefaults.cardColors(containerColor = SleekSurface), border = BorderStroke(1.dp, if (isActive) SleekCyan else SleekSurfaceBorder)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = loadout.commander?.imageLink, contentDescription = null, modifier = Modifier.size(48.dp).clip(CircleShape))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(loadout.commander?.name ?: "No Commander", fontWeight = FontWeight.Black, color = Color.White)
                        Text(loadout.teamPerk ?: "No Team Perk", fontSize = 12.sp, color = SleekCyan)
                    }
                    if (isActive) Box(Modifier.background(SleekCyan, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) { Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black) }
                }
            }
        }
    }
}

@Composable
fun PennyHeroDetailContent(hero: PennyHero) {
    Column(Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = hero.imageLink, contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(16.dp))
            Column {
                val name = if (hero.name == null || hero.name == "Hero") hero.templateId ?: "Hero" else hero.name
                Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = SleekTextPrimary)
                Text("${hero.rarity?.uppercase()} ${hero.heroClass?.uppercase()}", color = getRarityColor(hero.rarity ?: ""), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DetailStatCard("Level", hero.attributes?.level.toString(), Modifier.weight(1f))
            DetailStatCard("Power", hero.powerLevel.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
        Text("PERKS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekCyan)
        hero.heroPerks?.commanderPerk?.let { PennyPerkItem(it, "Commander") }
        hero.heroPerks?.subCommanderPerk?.let { PennyPerkItem(it, "Standard") }
        Spacer(Modifier.height(16.dp))
        Text("ABILITIES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekCyan)
        hero.heroPerks?.abilities?.forEach { ability ->
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = ability.imageLink, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(ability.name ?: "", color = Color.White, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("DESCRIPTION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekCyan)
        Text(hero.description ?: "No description available", color = Color.White, fontSize = 13.sp)
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun PennyPerkItem(perk: PennyPerk, type: String) {
    Row(Modifier.padding(vertical = 8.dp).fillMaxWidth()) {
        AsyncImage(model = perk.imageLink, contentDescription = null, modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text("$type: ${perk.name}", fontWeight = FontWeight.Bold, color = Color.White)
            Text(perk.description ?: "", fontSize = 12.sp, color = SleekTextMuted)
        }
    }
}

@Composable
fun PennySchematicDetailContent(schematic: PennySchematic) {
    Column(Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = schematic.imageLink, contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(16.dp))
            Column {
                val name = if (schematic.name == null || schematic.name == "Schematic") schematic.templateId ?: "Schematic" else schematic.name
                Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = SleekTextPrimary)
                Text(schematic.rarity?.uppercase() ?: "", color = getRarityColor(schematic.rarity ?: ""), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DetailStatCard("Power", schematic.powerLevel.toString(), Modifier.weight(1f))
            DetailStatCard("Level", schematic.attributes?.level.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
        Text("STATS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekCyan)
        schematic.stats?.forEach { (k, v) ->
            if (v is Number || v is String) {
                Row(Modifier.padding(vertical = 2.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(k.replace(Regex("([a-z])([A-Z])"), "$1 $2"), color = SleekTextMuted, fontSize = 12.sp)
                    Text(v.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("PERKS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekCyan)
        schematic.perks?.forEach { perk ->
            Row(Modifier.padding(vertical = 4.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(perk.name ?: "", color = Color.White, fontSize = 14.sp)
                Text(perk.rarity ?: "", color = getRarityColor(perk.rarity ?: "Common"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("CRAFTING COST", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekCyan)
        schematic.craftingCosts?.forEach { cost ->
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = if (cost.image?.startsWith("/") == true) "https://pennydb.net${cost.image}" else cost.image, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("${cost.name}: ${cost.quantity}", color = Color.White, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun PennySurvivorDetailContent(survivor: PennySurvivor) {
    Column(Modifier.fillMaxWidth().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = survivor.imageLink, contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(16.dp))
            Column {
                val name = if (survivor.name == null || survivor.name == "Survivor") survivor.templateId ?: "Survivor" else survivor.name
                Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = SleekTextPrimary)
                Text(survivor.rarity?.uppercase() ?: "", color = getRarityColor(survivor.rarity ?: ""), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DetailStatCard("Level", survivor.attributes?.level.toString(), Modifier.weight(1f))
            DetailStatCard("Power", survivor.powerLevel.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
        InfoRow("Personality", survivor.personality ?: "None")
        InfoRow("Set Bonus", survivor.setBonus ?: "None")
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun PennyDefenderDetailContent(defender: PennyDefender) {
    Column(Modifier.fillMaxWidth().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = defender.imageLink, contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(16.dp))
            Column {
                val name = if (defender.name == null || defender.name == "Defender") defender.templateId ?: "Defender" else defender.name
                Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = SleekTextPrimary)
                Text("${defender.rarity?.uppercase()} ${defender.defenderClass?.uppercase()}", color = getRarityColor(defender.rarity ?: ""), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        DetailStatCard("Power Level", defender.powerLevel.toString())
        Spacer(Modifier.height(24.dp))
        Text("PERKS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekCyan)
        defender.perks?.forEach { Text("• ${it.name}", color = Color.White, fontSize = 14.sp) }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun PennyLoadoutDetailContent(loadout: PennyLoadout) {
    Column(Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text(loadout.commander?.name?.uppercase() ?: "LOADOUT", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = if (loadout.teamPerkImage?.startsWith("/") == true) "https://pennydb.net${loadout.teamPerkImage}" else loadout.teamPerkImage, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(8.dp))
            Text("Team Perk: ${loadout.teamPerk}", color = SleekCyan, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        Text("COMMANDER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekTextMuted)
        PennyHeroBasicTile(loadout.commander)
        Spacer(Modifier.height(16.dp))
        Text("SUPPORT TEAM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekTextMuted)
        loadout.followers?.forEach { PennyHeroBasicTile(it) }
        Spacer(Modifier.height(16.dp))
        Text("GADGETS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekTextMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PennyGadgetTile(loadout.gadget1, if (loadout.gadget1Image?.startsWith("/") == true) "https://pennydb.net${loadout.gadget1Image}" else loadout.gadget1Image, Modifier.weight(1f))
            PennyGadgetTile(loadout.gadget2, if (loadout.gadget2Image?.startsWith("/") == true) "https://pennydb.net${loadout.gadget2Image}" else loadout.gadget2Image, Modifier.weight(1f))
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun PennyHeroBasicTile(hero: PennyHeroBasic?) {
    hero?.let {
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = SleekSurface)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = it.imageLink, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(it.name ?: "", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("PL ${it.powerLevel} • ${it.heroClass}", fontSize = 11.sp, color = getRarityColor(it.rarity ?: "Common"))
                }
            }
        }
    }
}

@Composable
fun PennyGadgetTile(name: String?, imageUrl: String?, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = SleekSurface)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.size(32.dp))
            Text(name ?: "Empty", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun PennyAchievementsList(achievements: Map<String, Map<String, PennyAchievement>>) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        achievements.values.flatMap { it.values }.forEach { ach ->
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurface)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = ach.imageLink, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(ach.name ?: "", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${ach.currentValue} / ${ach.totalRequired}", fontSize = 12.sp, color = SleekTextMuted)
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (ach.currentValue ?: 0).toFloat() / (ach.totalRequired ?: 1).toFloat() },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = SleekCyan,
                                trackColor = SleekBackground,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            ach.completed?.date?.let { Text("Completed on $it", fontSize = 9.sp, color = SleekEmerald) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PennyAchievementsSummary(achievements: Map<String, Map<String, PennyAchievement>>?, onClick: () -> Unit) {
    achievements?.let {
        Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = SleekSurface), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ACHIEVEMENTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekTextMuted)
                    Icon(Icons.Default.ChevronRight, null, tint = SleekTextMuted)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    it.values.flatMap { inner -> inner.values }.take(5).forEach { ach ->
                        AsyncImage(model = ach.imageLink, contentDescription = null, modifier = Modifier.size(40.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PennyExpeditionsList(expeditions: Map<String, PennyExpedition>) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(expeditions.values.toList()) { ex ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SleekSurface)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Explore, null, tint = Color.Yellow)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(ex.name ?: "Expedition", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(ex.templateId ?: "", fontSize = 10.sp, color = SleekTextMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardTile(modifier: Modifier = Modifier, label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(modifier = modifier.height(90.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = SleekSurface), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, tint = color, modifier = Modifier.size(28.dp)); Spacer(Modifier.height(8.dp)); Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black, color = SleekTextPrimary) }
    }
}

@Composable
fun CommandMenuTile(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = SleekSurface), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp); Text(subtitle, fontSize = 11.sp, color = SleekTextMuted) }
            Icon(Icons.Default.ChevronRight, null, tint = SleekTextMuted)
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
fun DetailStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = SleekSurface), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, SleekSurfaceBorder)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White); Text(label, fontSize = 12.sp, color = SleekTextMuted) }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = SleekTextMuted)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
    }
}
