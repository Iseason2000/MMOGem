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
import top.iseason.bukkit.mmogem.Config
import top.iseason.bukkit.mmogem.Config.readBackGround
import top.iseason.bukkit.mmogem.Config.readSlot
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.applyMeta
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.item
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.toSection

@FilePath("ui/expand.yml")
object ExpandUIConfig : SimpleYAMLConfig() {

    @Key
    @Comment("ui标题")
    var title = "开拓宝石槽"

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
            setDisplayName("${ChatColor.RED} 请放入开孔材料")
        }.toSection())
    }

    var input: Pair<IntArray, HashMap<String, ItemStack>> = Config.empty

    @Key("material")
    @Comment("材料槽")
    var materialSection: MemorySection = YamlConfiguration().apply {
        set("slot", "22")
        set("default", Material.RED_STAINED_GLASS_PANE.item.applyMeta {
            setDisplayName("${ChatColor.RED} 请放入开孔材料")
        }.toSection())
    }
    var material: Pair<IntArray, HashMap<String, ItemStack>> = Config.empty

    @Key("button")
    @Comment("开孔按钮")
    var buttonSection: MemorySection = YamlConfiguration().apply {
        set("slots", "31")
        set("default", Material.ANVIL.item.applyMeta {
            setDisplayName("${ChatColor.RED} 请放入开孔材料")
        }.toSection())

        set("available", Material.ANVIL.item.applyMeta {
            setDisplayName("${ChatColor.RED} 点击开槽")
        }.toSection())

        set("unavailable", Material.ANVIL.item.applyMeta {
            setDisplayName("${ChatColor.RED} 已无法开孔")
        }.toSection())
    }
    var button: Pair<IntArray, HashMap<String, ItemStack>> = Config.empty

    @Key("result")
    @Comment("输出槽，只能有一个")
    var resultSection: MemorySection = YamlConfiguration().apply {
        set("slot", "40")
        set("default", Material.RED_STAINED_GLASS_PANE.item.applyMeta {
            setDisplayName("${ChatColor.RED} 请放入待强化的物品")
        }.toSection())
    }

    var result: Pair<IntArray, HashMap<String, ItemStack>> = Config.empty

    override fun onLoaded(section: ConfigurationSection) {
        background = readBackGround(backgroundSection)
        material = readSlot(materialSection)
        input = readSlot(inputSection)
        button = readSlot(buttonSection)
        result = readSlot(resultSection)
    }

}