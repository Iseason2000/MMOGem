/*
 * Description:
 * @Author: Iseason2000
 * @Date: 2022/10/1 下午7:44
 *
 */

package top.iseason.bukkit.mmogem.ui

import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.mmogem.config.Config
import top.iseason.bukkit.mmogem.config.Config.readBackGround
import top.iseason.bukkit.mmogem.config.Config.readSlot
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.applyMeta
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.item
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.toSection

@FilePath("ui/inlay.yml")
object InlayUIConfig : SimpleYAMLConfig() {

    @Key
    @Comment("ui标题")
    var title = "宝石镶嵌"

    @Key
    @Comment("ui行数")
    var row = 6

    @Key
    @Comment("点击延迟(毫秒)")
    var clickDelay = 200L

    @Key("background")
    @Comment("背景图标")
    var backgroundSection: MemorySection = YamlConfiguration().apply {
        createSection("default", buildMap {
            put("slots", (0 until row * 9).joinToString(separator = ","))
            put("icon", Material.GRAY_STAINED_GLASS_PANE.item.applyMeta { setDisplayName(" ") }.toSection())
        })
    }

    var background: Array<Pair<Int, ItemStack>> = emptyArray()

    @Key("input")
    @Comment("工具输入槽，只能有一个")
    var inputSection: MemorySection = YamlConfiguration().apply {
        set("slot", "13")
        set("default", Material.RED_STAINED_GLASS_PANE.item.applyMeta {
            setDisplayName("${ChatColor.RED} 请放入待强化的物品")
        }.toSection())
    }

    var input: Pair<IntArray, HashMap<String, ItemStack>> = Config.empty

    @Key("gem-slots")
    @Comment("宝石槽-空")
    var slotsSection: MemorySection = YamlConfiguration().apply {
        set("slots", "19,20,21,22,23")
        set("default", Material.RED_STAINED_GLASS_PANE.item.applyMeta {
            setDisplayName("${ChatColor.RED} 请放入待强化的物品")
        }.toSection())
        set("available", Material.RED_STAINED_GLASS_PANE.item.applyMeta {
            setDisplayName("${ChatColor.YELLOW} 可镶嵌, 颜色 {color}")
        }.toSection())
    }

    var slots: Pair<IntArray, HashMap<String, ItemStack>> = Config.empty

    @Key("material")
    @Comment("材料槽")
    var materialSection: MemorySection = YamlConfiguration().apply {
        set("slots", "31")
        set("default", Material.RED_STAINED_GLASS_PANE.item.applyMeta {
            setDisplayName("${ChatColor.RED} 拆卸宝石请放入材料")
        }.toSection())
    }

    var material: Pair<IntArray, HashMap<String, ItemStack>> = Config.empty

    override fun onLoaded(section: ConfigurationSection) {
        background = readBackGround(backgroundSection)
        input = readSlot(inputSection)
        slots = readSlot(slotsSection)
        material = readSlot(materialSection)
    }

}