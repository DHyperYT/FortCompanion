package com.dhyper.fncompanion.data.repository

import com.dhyper.fncompanion.data.models.StwMissionAlert
import com.dhyper.fncompanion.data.models.StwReward
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.util.UUID

class StwScraperRepository {

    suspend fun fetchMissionAlerts(): Result<List<StwMissionAlert>> = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect("https://seebot.dev/missions.php")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(30000)
                .get()

            val alerts = mutableListOf<StwMissionAlert>()
            
            val allRows = doc.select("table tr")

            var currentZone = "Global"

            allRows.forEach { row ->
                // Check if this row is a header for a Zone (Stonewood, etc.)
                val headerText = row.text().trim()
                val zones = listOf("Stonewood", "Plankerton", "Canny Valley", "Twine Peaks", "Ventures")
                val foundZone = zones.find { headerText.equals(it, ignoreCase = true) || (headerText.contains(it) && row.select("td, th").size == 1) }
                
                if (foundZone != null) {
                    currentZone = foundZone
                    return@forEach
                }

                val cells = row.select("td")
                // Mapping: 0:Done, 1:PL, 2:Icon, 3:Biome, 4:Modifiers, 5:Alert, 6:Base, 7:Quest
                if (cells.size >= 7) {
                    val plText = cells[1].text().trim()
                    if (plText.isEmpty() || plText.lowercase().contains("pl")) return@forEach // Skip header row
                    
                    val pl = plText.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                    
                    val missionImg = cells[2].select("img").firstOrNull()
                    val missionName = missionImg?.attr("title")?.ifBlank { missionImg.attr("alt") }
                        ?: cells[2].text().trim().ifBlank { "Mission" }
                    
                    val biome = cells[3].text().trim()
                    val modifiers = cells[4].select("img").mapNotNull { it.attr("title").ifBlank { it.attr("alt") }.ifBlank { null } }
                    
                    val alertRewards = parseRewardsFromCell(cells[5])
                    val baseRewards = parseRewardsFromCell(cells[6])
                    val quest = if (cells.size > 7) cells[7].text().trim() else ""

                    if (pl > 0 || missionName != "Mission") {
                        alerts.add(StwMissionAlert(
                            id = UUID.randomUUID().toString(),
                            name = missionName,
                            zoneName = currentZone,
                            missionType = if (alertRewards.isNotEmpty()) "Alert" else "Standard",
                            difficulty = pl,
                            rewards = baseRewards,
                            bonusRewards = alertRewards,
                            biome = biome,
                            requirements = quest,
                            modifiers = modifiers
                        ))
                    }
                }
            }

            Result.success(alerts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseRewardsFromCell(cell: org.jsoup.nodes.Element): List<StwReward> {
        val rewards = mutableListOf<StwReward>()
        
        // Seebot often uses images with alt/title and text next to them
        val rewardItems = cell.select("img, span")
        
        if (rewardItems.isEmpty() && cell.text().isNotBlank()) {
            // Fallback to text parsing
            cell.text().split(",").forEach {
                val t = it.trim()
                if (t.isNotBlank()) {
                    rewards.add(StwReward(UUID.randomUUID().toString(), t, 1, determineRarityFromText(t)))
                }
            }
            return rewards
        }

        // Better approach: find all images and their associated text
        // Or just iterate child nodes
        cell.childNodes().forEach { node ->
            if (node is org.jsoup.nodes.Element) {
                if (node.tagName() == "img") {
                    val name = node.attr("title").ifBlank { node.attr("alt") }
                    val icon = node.attr("abs:src")
                    if (name.isNotBlank()) {
                        rewards.add(StwReward(UUID.randomUUID().toString(), name, 1, determineRarityFromText(name), icon))
                    }
                } else if (node.tagName() == "span" || node.tagName() == "b") {
                    val name = node.text().trim()
                    if (name.isNotBlank() && name.length > 2) {
                        rewards.add(StwReward(UUID.randomUUID().toString(), name, 1, determineRarityFromText(name)))
                    }
                }
            } else if (node is org.jsoup.nodes.TextNode) {
                val text = node.text().trim().trim(',').trim()
                if (text.isNotBlank() && text.length > 2 && !text.startsWith("x")) {
                    rewards.add(StwReward(UUID.randomUUID().toString(), text, 1, determineRarityFromText(text)))
                }
            }
        }

        return rewards
    }

    private fun determineRarityFromText(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("mythic") -> "Mythic"
            lower.contains("legendary") || lower.contains("v-bucks") || lower.contains("survivor (legendary)") -> "Legendary"
            lower.contains("epic") -> "Epic"
            lower.contains("rare") -> "Rare"
            lower.contains("uncommon") -> "Uncommon"
            else -> "Common"
        }
    }
}
