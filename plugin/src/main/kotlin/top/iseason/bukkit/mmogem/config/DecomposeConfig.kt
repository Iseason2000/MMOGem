package top.iseason.bukkit.mmogem

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
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.toSection
import top.iseason.bukkittemplate.utils.bukkit.SchedulerUtils.submit

@FilePath("decompose.yml")
object DecomposeConfig : SimpleYAMLConfig() {

    @Key
    @Comment(
        "",
        "物品优先级，优先级越高越前"
    )
    var weights: Map<String, Double> = mapOf(
        "DIAMOND" to 100.0,
        "MATERIAL:STEEL_INGOT" to 200.0
    )

    @Key("items")
    @Comment(
        "",
        "物品分解"
    )
    var itemSection: MemorySection = YamlConfiguration().apply {
        set(
            "MATERIAL:STEEL_INGOT.result", listOf(
                ItemStack(Material.DIAMOND).toSection().apply {
                    set("rate", 100.0)
                    set("num", "2-3")
                },
                ItemStack(Material.DIAMOND).toSection().apply {
                    set("material", "MATERIAL:STEEL_INGOT")
                    set("rate", 5.0)
                    set("num", 1)
                })
        )
        set("MATERIAL:STEEL_INGOT.msg", listOf<String>("[console] say ok"))
    }

    var items: HashMap<String, Decomposes> = HashMap()

    override fun onLoaded(section: ConfigurationSection) {
        items.clear()
        submit {
            itemSection.getValues(false).forEach { (key, value) ->
                key ?: return@forEach
                if (value !is ConfigurationSection) return@forEach
                val result = value.getList("result")?.map { it ->
                    val conf: ConfigurationSection
                    if (it is ConfigurationSection) {
                        conf = it
                    } else {
                        section.set("temp", null)
                        conf = section.createSection("temp")
                        (it as Map<String, Any>).forEach { k, v ->
                            conf.set(k, v)
                        }
                    }
                    val item = ItemUtils.fromSection(conf) ?: return@forEach
                    val rate = conf.getDouble("rate", 100.0)
                    val numConf = conf.get("num")
                    var min: Int
                    var max: Int
                    when (numConf) {
                        is Int -> {
                            min = numConf
                            max = numConf
                        }

                        is String -> {
                            val split = numConf.split('-', limit = 2)
                            min = split[0].toInt()
                            max = split[1].toInt()
                        }

                        else -> return@forEach
                    }
                    Decompose(conf.getString("material")!!, item, rate, min, max)
                } ?: return@forEach
                items[key] = Decomposes(result, value.getStringList("msg"))
            }
        }
        section.set("temp", null)
    }

    data class Decomposes(
        val result: List<Decompose>,
        val msg: List<String>
    )

    data class Decompose(
        val key: String,
        val item: ItemStack,
        val rate: Double,
        val min: Int,
        val max: Int
    )
}