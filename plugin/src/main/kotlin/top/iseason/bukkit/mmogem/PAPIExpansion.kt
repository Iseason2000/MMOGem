package top.iseason.bukkit.mmogem

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import net.Indyuce.mmoitems.api.player.PlayerData
import org.bukkit.OfflinePlayer
import kotlin.math.roundToInt

object PAPIExpansion : PlaceholderExpansion() {

    override fun getIdentifier(): String {
        return "mmogen"
    }

    override fun getAuthor(): String {
        return "Iseason"
    }

    override fun getVersion(): String {
        return "1.0.0"
    }

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (player == null) return null
        if (params != FightPower.papiName) return null
        val dataMap = PlayerData.get(player).stats.map
        var fightPower = 0.0
        for ((id, rate) in FightPower.stats) {
            fightPower += dataMap.getStat(id) * rate
        }
        return Math.ceil(fightPower).toInt().toString()
    }
}