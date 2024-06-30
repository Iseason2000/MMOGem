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

@FilePath("ui/decompose.yml")
object DecomposeUIConfig : SimpleYAMLConfig() {

    @Key
    @Comment("ui标题")
    var title = "物品分解"

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
    @Comment("输入槽")
    var inputSection: MemorySection = YamlConfiguration().apply {
        set("slots", "11,12,13,14,15")
        set("default", Material.RED_STAINED_GLASS_PANE.item.applyMeta {
            setDisplayName("${ChatColor.RED} 请放入待分解的物品")
        }.toSection())
    }

    var input: Pair<IntArray, HashMap<String, ItemStack>> = Config.empty


    @Key("button")
    @Comment("分解按钮")
    var buttonSection: MemorySection = YamlConfiguration().apply {
        set("slots", "31")
        set("default", Material.ANVIL.item.applyMeta {
            setDisplayName("${ChatColor.RED} 请放入材料")
        }.toSection())

        set("available", Material.ANVIL.item.applyMeta {
            setDisplayName("${ChatColor.RED} 点击分解")
        }.toSection())
    }
    var button: Pair<IntArray, HashMap<String, ItemStack>> = Config.empty

    @Key("material")
    @Comment("材料预览槽")
    var materialSection: MemorySection = YamlConfiguration().apply {
        set("slots", "38,39,40,41,42")
        set("default", Material.RED_STAINED_GLASS_PANE.item.applyMeta {
            setDisplayName("${ChatColor.RED} 请放入待分解的物品")
        }.toSection())
        set("lore", listOf(
            "出现概率: {rate} %",
            "预估数量: {num}"
        ))
    }

    var material: Pair<IntArray, HashMap<String, ItemStack>> = Config.empty

    override fun onLoaded(section: ConfigurationSection) {
        background = readBackGround(backgroundSection)
        input = readSlot(inputSection)
        button = readSlot(buttonSection)
        material = readSlot(materialSection)
    }

}