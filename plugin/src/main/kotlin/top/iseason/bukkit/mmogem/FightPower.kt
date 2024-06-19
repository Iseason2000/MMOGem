package top.iseason.bukkit.mmogem

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import net.Indyuce.mmoitems.MMOItems
import net.Indyuce.mmoitems.api.player.PlayerData
import org.bukkit.OfflinePlayer
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key


@FilePath("fight-power.yml")
object FightPower : SimpleYAMLConfig() {

    @Key
    @Comment("", "战力注册的变量名, 实际变量为 mmogen_fight_power")
    var papiName = "fight_power"

    @Key
    @Comment("", "所有的属性, 属性ID:战力系数")
    var stats = MMOItems.plugin.stats.numericStats.associate { it.id to 1.0 }

}