package top.iseason.bukkit.mmogem.stat

import net.Indyuce.mmoitems.stat.type.DoubleStat
import org.bukkit.Material

object RebuildTimes : DoubleStat(
    "REBUILD_TIMES",
    Material.EXPERIENCE_BOTTLE,
    "Rebuild Times",
    arrayOf("重塑次数"),
    arrayOf("all"),
)