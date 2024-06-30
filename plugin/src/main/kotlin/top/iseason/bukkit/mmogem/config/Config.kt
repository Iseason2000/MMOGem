package top.iseason.bukkit.mmogem.config

import io.lumine.mythic.lib.UtilityMethods
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.item
import java.util.ArrayList
import java.util.HashSet
import kotlin.apply
import kotlin.collections.forEach
import kotlin.collections.map
import kotlin.collections.set
import kotlin.collections.toIntArray
import kotlin.collections.toTypedArray
import kotlin.text.split
import kotlin.text.toInt
import kotlin.text.trim
import kotlin.to

@FilePath("config.yml")
object Config : SimpleYAMLConfig() {

    @Key("remove-gem-materials")
    @Comment("", "拆卸宝石消耗的物品", "宝石ID: 材料ID,数量")
    var removeGemMaterialsSection: MemorySection = YamlConfiguration().apply {
        set("RUBY", "MATERIAL:STEEL_INGOT,1")
        set("GEM_OF_LIFE", "MATERIAL:STEEL_INGOT,2")
    }

    @Key("gem-expand-materials")
    @Comment("", "宝石开槽消耗品", "MMO物品ID: 最大开槽数量,开槽颜色,...多个颜色随机")
    var expandGemMaterialsSection: MemorySection = YamlConfiguration().apply {
        set("MATERIAL:STEEL_INGOT", "3,Red")
        set("MATERIAL:UNCOMMON_WEAPON_ESSENCE", "5,Blue,Red,Yellow")
    }

    @Key("gem-expand-white-list")
    @Comment("", "宝石开白名单,MMO物品ID")
    var expandGemWhiteList: HashSet<String> =
        hashSetOf("SWORD:LONG_SWORD", "SWORD:CUTLASS", "SWORD:FALCON_BLADE")

    @Key("rebuild")
    @Comment(
        "",
        "",
        "物品重塑设置",
    )
    var rebuild: MemorySection? = null

    @Key("rebuild.default-guarantee")
    @Comment("默认重塑保底次数")
    var defaultRebuildGuarantee = 30

    @Key("rebuild.default-normal")
    @Comment("", "默认重塑消耗", "MMO物品ID,数量")
    var defaultRebuildMaterialsSection = "MATERIAL:STEEL_INGOT,1"

    @Key("rebuild.default-special")
    @Comment("", "默认保底重塑消耗", "MMO物品ID,数量")
    var defaultMaxRebuildMaterialsSection = "MATERIAL:UNCOMMON_WEAPON_ESSENCE,1"

    @Key("rebuild.available")
    @Comment("", "可重塑的物品", "MMO物品ID列表")
    var rebuildAvailableSection: HashSet<String> =
        hashSetOf("SWORD:LONG_SWORD", "SWORD:CUTLASS", "SWORD:FALCON_BLADE")

    @Key("rebuild.override-config")
    @Comment(
        "", "覆盖默认重塑设置",
        "物品ID.normal:普通重塑道具,数量",
        "物品ID.special:保底重塑道具,数量",
        "物品ID.guarantee:保底次数"
    )
    var rebuildMaterialsSection: MemorySection = YamlConfiguration().apply {
        set("SWORD:CUTLASS.normal", "MATERIAL:STEEL_INGOT,2")
        set("SWORD:CUTLASS.special", "MATERIAL:UNCOMMON_WEAPON_ESSENCE,2")
        set("SWORD:CUTLASS.guarantee", 20)
    }

    @Key("rebuild.gold-exp")
    @Comment(
        "", "金币公式",
        "{rebuild}: 当前重塑次数",
    )
    var rebuildGoldExp = "500*{rebuild}+500"

    var rebuildMaterials = emptyMap<String, RebuildConfig>()

    var removeGemMaterials = emptyMap<String, Pair<String, Int>>()

    var expandGemMaterials = emptyMap<String, Pair<Int, String>>()

    override fun onLoaded(section: ConfigurationSection) {
        val hashMap = java.util.HashMap<String, Pair<String, Int>>()
        for ((key, data) in removeGemMaterialsSection.getValues(false)) {
            val s = data as String
            val split = s.split(",", limit = 2)
            val material = UtilityMethods.enumName(split[0])
            val num = split[1].toInt()
            hashMap[key] = material to num
        }
        removeGemMaterials = hashMap

        val hashMap2 = java.util.HashMap<String, Pair<Int, String>>()
        for ((key, data) in expandGemMaterialsSection.getValues(false)) {
            val s = data as String
            val split = s.split(",", limit = 2)
            val material = UtilityMethods.enumName(key)
            hashMap2[material] = split[0].toInt() to split[1]
        }
        expandGemMaterials = hashMap2

        val hashMap3 = java.util.HashMap<String, RebuildConfig>()
        for (name in rebuildAvailableSection) {
            val normalStr = rebuildMaterialsSection.getString("$name.normal") ?: defaultRebuildMaterialsSection
            val normalSplit = normalStr.split(",", limit = 2)
            val specialStr = rebuildMaterialsSection.getString("$name.special") ?: defaultMaxRebuildMaterialsSection
            val specialSplit = specialStr.split(",", limit = 2)
            val guarantee = rebuildMaterialsSection.getInt("$name.guarantee", defaultRebuildGuarantee)
            val rebuildConfig = RebuildConfig(
                UtilityMethods.enumName(normalSplit[0]),
                normalSplit[1].toInt(),
                UtilityMethods.enumName(specialSplit[0]),
                specialSplit[1].toInt(),
                guarantee
            )
            hashMap3[UtilityMethods.enumName(name)] = rebuildConfig
        }
        rebuildMaterials = hashMap3

    }

    val empty: Pair<IntArray, HashMap<String, ItemStack>> = Pair(IntArray(0), java.util.HashMap())

    /**
     * 读取槽
     */
    fun readSlot(section: ConfigurationSection): Pair<IntArray, HashMap<String, ItemStack>> {
        var slotsStr: String? = section.getString("slots")
        val slots: IntArray
        if (slotsStr != null) {
            slots = slotsStr.trim().split(',').map { it.toInt() }.toIntArray()
        } else {
            slotsStr = section.getString("slot") ?: return empty
            slots = IntArray(1)
            slots[0] = slotsStr.toInt()
        }
        val map = java.util.HashMap<String, ItemStack>()
        section.getValues(false).forEach { (key, cfg) ->
            if (cfg !is ConfigurationSection) return@forEach
            val item = ItemUtils.fromSection(cfg) ?: Material.AIR.item
            map[key] = item
        }
        return slots to map
    }

    fun readBackGround(section: ConfigurationSection): Array<Pair<Int, ItemStack>> {
        val arrayList = ArrayList<Pair<Int, ItemStack>>()
        for ((_, sec) in section.getValues(false)) {
            val conf = sec as? ConfigurationSection ?: continue
            val slotsStr = conf.getString("slots") ?: continue
            val itemSection = conf.getConfigurationSection("icon") ?: continue
            val item = ItemUtils.fromSection(itemSection) ?: continue
            slotsStr.trim().split(',').map {
                arrayList.add(it.toInt() to item)
            }
        }
        return arrayList.toTypedArray()
    }

    data class RebuildConfig(
        val normalMaterial: String,
        val normalConsume: Int,
        val specialMaterial: String,
        val specialConsume: Int,
        val guarantee: Int
    )


}