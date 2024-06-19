package top.iseason.bukkit.mmogem.ui

import io.lumine.mythic.lib.UtilityMethods
import io.lumine.mythic.lib.api.item.NBTItem
import net.Indyuce.mmoitems.MMOItems
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem
import net.Indyuce.mmoitems.api.player.PlayerData
import net.Indyuce.mmoitems.api.util.NumericStatFormula
import net.Indyuce.mmoitems.api.util.NumericStatFormula.FormulaInputType
import net.Indyuce.mmoitems.stat.data.DoubleData
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.mmoforge.uitls.kparser.ExpressionParser
import top.iseason.bukkit.mmogem.Config
import top.iseason.bukkit.mmogem.Lang
import top.iseason.bukkit.mmogem.MMOGem
import top.iseason.bukkit.mmogem.VaultHook.takeMoney
import top.iseason.bukkit.mmogem.stat.RebuildTimes
import top.iseason.bukkittemplate.hook.PlaceHolderHook
import top.iseason.bukkittemplate.ui.container.ChestUI
import top.iseason.bukkittemplate.ui.slot.*
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.applyMeta
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.decrease
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.toColor

class RebuildUI(val player: Player) :
    ChestUI(
        PlaceHolderHook.setPlaceHolder(RebuildUIConfig.title, player),
        RebuildUIConfig.row,
        RebuildUIConfig.clickDelay
    ) {

    init {
        for ((index, item) in RebuildUIConfig.background) {
            Icon(item, index).setup()
        }
    }

    val inputSlot = run {
        val (slots, item) = RebuildUIConfig.input
        val index = slots[0]
        InputSlot(index, item).setup()
    }

    val outputSlot = run {
        val (slots, item) = RebuildUIConfig.result
        val index = slots[0]
        OutputSlot(index, item).setup()
    }

    val materialSlot = run {
        val (slots, item) = RebuildUIConfig.material
        val index = slots[0]
        MaterialSlot(index, item).setup()
    }

    val button = run {
        val (slots, item) = RebuildUIConfig.button
        slots.map {
            ButtonSlot(it, item).setup()
        }
    }


    inner class InputSlot(index: Int, items: HashMap<String, ItemStack>) : IOSlot(index, items["default"]) {
        var mmoItem: LiveMMOItem? = null
        var typeId: String? = null

        init {
            inputFilter {
                val nbtItem = NBTItem.get(it) ?: return@inputFilter false
                if (!nbtItem.hasType()) return@inputFilter false
                val type = MMOItems.getType(nbtItem)!!.id
                val id = MMOItems.getID(nbtItem)!!
                val typeId = UtilityMethods.enumName("$type:$id")
                val config = Config.rebuildMaterials[typeId]
                if (config == null) {
                    val msg = PlaceHolderHook.setPlaceHolder(Lang.rebuild__deny, player)
                    player.sendColorMessage(msg)
                    return@inputFilter false
                }
                val liveMMOItem = LiveMMOItem(it)
                val times = (liveMMOItem.getData(RebuildTimes) as? DoubleData)?.value ?: 0.0
                if (times >= config.guarantee) {
                    val msg = PlaceHolderHook.setPlaceHolder(Lang.rebuild__max, player)
                    player.sendColorMessage(msg)
                    return@inputFilter false
                }
                if (!liveMMOItem.gemstones.isEmpty()) {
                    val msg = PlaceHolderHook.setPlaceHolder(Lang.rebuild__has_gem, player)
                    player.sendColorMessage(msg)
                    return@inputFilter false
                }

                mmoItem = liveMMOItem
                this@InputSlot.typeId = typeId
                true
            }
            onInput(true) {
                val config = Config.rebuildMaterials[typeId]!!
                val mId = materialSlot.typeId
                for (buttonSlot in button) {
                    buttonSlot.setInput(config)
                }
                outputSlot.ejectSilently(player)
                if (mId != null) {
                    if (mId != config.normalMaterial && mId != config.specialMaterial) {
                        materialSlot.ejectSilently(player)
                    } else
                        materialSlot.onInput.invoke(materialSlot, materialSlot.itemStack!!)
                }
            }
            onOutput(true) {
                mmoItem = null
                materialSlot.ejectSilently(player)
                for (buttonSlot in button) {
                    buttonSlot.reset()
                }
                outputSlot.reset()
                this@InputSlot.typeId = null
            }
        }

        override fun reset() {
            super.reset()
            this@InputSlot.typeId = null
            mmoItem = null
        }
    }

    inner class OutputSlot(index: Int, items: HashMap<String, ItemStack>) : IOSlot(index, items["default"]) {
        var isPreview = false

        init {
            inputAble(false)
            outputAble(false)
        }

        fun setPreview() {
            val mmo = inputSlot.mmoItem!!
            itemStack =
                MMOItems.plugin.templates.getTemplate(mmo.type, mmo.id)!!
                    .newBuilder(PlayerData.get(player).rpg, true)
                    .build().newBuilder().build(true)
            isPreview = true
            outputAble(false)
        }

        override fun reset() {
            super.reset()
            isPreview = false
        }

        override fun ejectSilently(humanEntity: HumanEntity) {
            if (isPreview) {
                reset()
                return
            }
            super.ejectSilently(humanEntity)
        }
    }

    inner class ButtonSlot(index: Int, items: HashMap<String, ItemStack>) : Button(items["default"], index) {
        private val available: ItemStack = items["available"]!!
        private val input: ItemStack = items["input"]!!
        private var state = 0

        init {
            onClicked {
                when (state) {
                    1 -> {
                        val config = Config.rebuildMaterials[inputSlot.typeId]!!

                        val mmoItem = inputSlot.mmoItem!!
                        val times = (mmoItem.getData(RebuildTimes) as? DoubleData)?.value ?: 0.0
                        val express = Config.rebuildGoldExp.trim().replace("{rebuild}", times.toInt().toString())
                        val gold = ExpressionParser().evaluate(express)
                        if (!player.takeMoney(gold)) {
                            player.sendColorMessage(Lang.rebuild__no_money)
                            return@onClicked
                        }
                        val isGuarantee = times >= config.guarantee - 1 || materialSlot.typeId == config.specialMaterial

                        val template = MMOItems.plugin.templates.getTemplate(mmoItem.type, mmoItem.id)!!
                        val newMMO =
                            template
                                .newBuilder(PlayerData.get(player).rpg)
                                .build()
                        if (isGuarantee) {
                            for (stat in newMMO.stats) {
                                val numericStatFormula = template.baseItemData[stat] as? NumericStatFormula ?: continue
                                val calculate = numericStatFormula.calculate(0.0, FormulaInputType.UPPER_BOUND)
                                newMMO.setData(stat, DoubleData(calculate))
                            }
                            newMMO.setData(RebuildTimes, DoubleData(config.guarantee.toDouble()))
                        } else {
                            newMMO.setData(RebuildTimes, DoubleData(times + 1))
                        }
                        outputSlot.itemStack = newMMO.newBuilder().buildSilently()
                        val amount = materialSlot.itemStack!!.amount
                        var consume = 1
                        if (inputSlot.typeId == config.normalMaterial) {
                            consume = config.normalConsume
                        } else if (inputSlot.typeId == config.specialMaterial) {
                            consume = config.specialConsume
                        }
                        if (amount == consume) materialSlot.reset()
                        else materialSlot.itemStack!!.decrease(consume)
                        val itemName = MMOGem.getItemName(inputSlot.typeId!!)
                        inputSlot.reset()
                        outputSlot.outputAble(true)
                        outputSlot.isPreview = false

                        val msg = PlaceHolderHook.setPlaceHolder(Lang.rebuild__success.formatBy(itemName), player)
                        player.sendColorMessage(msg)

                        if (times >= config.guarantee - 1) {
                            val msg = PlaceHolderHook.setPlaceHolder(Lang.rebuild__no_max.formatBy(itemName), player)
                            player.sendColorMessage(msg)
                        } else if (materialSlot.typeId == config.specialMaterial) {
                            val msg = PlaceHolderHook.setPlaceHolder(Lang.rebuild__special.formatBy(itemName), player)
                            player.sendColorMessage(msg)
                        }
                        reset()
                    }

                    else -> return@onClicked
                }
            }
        }

        fun setInput(config: Config.RebuildConfig) {
            val string = RebuildUIConfig.buttonSection
                .getString("input-lore", "- {1} 个 {0}")!!
            itemStack = input.clone().applyMeta {
                val rawLore = if (hasLore()) lore!!.toMutableList() else ArrayList()

                val lore1 =
                    string.formatBy(MMOGem.getItemName(config.normalMaterial).toColor(), config.normalConsume)
                val lore2 =
                    string.formatBy(MMOGem.getItemName(config.specialMaterial).toColor(), config.specialConsume)
                rawLore.add(lore1)
                rawLore.add(lore2)
                lore = rawLore

            }
            state = 2
        }

        fun setAvailable(gold: Double, rebuild: Int, maxRebuild: Int) {
            itemStack = available.clone().applyMeta {
                if (hasDisplayName()) setDisplayName(
                    displayName
                        .replace("{gold}", gold.toString())
                        .replace("{rebuild}", rebuild.toString())
                        .replace("{max-rebuild}", maxRebuild.toString())

                )
                if (hasLore()) lore = lore!!.map {
                    it.replace("{gold}", gold.toString())
                        .replace("{rebuild}", rebuild.toString())
                        .replace("{max-rebuild}", maxRebuild.toString())
                }
            }
            state = 1
            outputSlot.setPreview()
        }

        override fun reset() {
            super.reset()
            state = 0
        }

    }

    inner class MaterialSlot(index: Int, items: HashMap<String, ItemStack>) : IOSlot(index, items["default"]) {
        var typeId: String? = null

        init {
            lockable(false)
            inputFilter {
                if (inputSlot.mmoItem == null) return@inputFilter false
                val nbtItem = NBTItem.get(it) ?: return@inputFilter false
                if (!nbtItem.hasType()) return@inputFilter false
                val config = Config.rebuildMaterials[inputSlot.typeId]!!
                val type = MMOItems.getType(nbtItem)!!.id
                val id = MMOItems.getID(nbtItem)!!
                val typeId = UtilityMethods.enumName("$type:$id")
                val result = config.normalMaterial == typeId || config.specialMaterial == typeId
                this@MaterialSlot.typeId = typeId
                result
            }
            onInput {
                val config = Config.rebuildMaterials[inputSlot.typeId]!!
                val amount = itemStack!!.amount
                if (typeId == config.normalMaterial && amount >= config.normalConsume) {
                    val times = (inputSlot.mmoItem!!.getData(RebuildTimes) as? DoubleData)?.value ?: 0.0
                    val express = Config.rebuildGoldExp.trim().replace("{rebuild}", times.toInt().toString())
                    val evaluate = ExpressionParser().evaluate(express)
                    for (buttonSlot in button) {
                        buttonSlot.setAvailable(evaluate, times.toInt(), config.guarantee)
                    }
                } else if (typeId == config.specialMaterial && amount >= config.specialConsume) {
                    val times = (inputSlot.mmoItem!!.getData(RebuildTimes) as? DoubleData)?.value ?: 0.0
                    val express = Config.rebuildGoldExp.trim().replace("{rebuild}", times.toInt().toString())
                    val evaluate = ExpressionParser().evaluate(express)
                    for (buttonSlot in button) {
                        buttonSlot.setAvailable(evaluate, times.toInt(), config.guarantee)
                    }
                } else {
                    for (buttonSlot in button) {
                        buttonSlot.setInput(config)
                    }
                }
            }
            onOutput {
                outputSlot.ejectSilently(player)
                val config = Config.rebuildMaterials[inputSlot.typeId]
                if (itemStack != null && config != null) {
                    onInput(itemStack!!)
                } else {
                    for (buttonSlot in button) {
                        buttonSlot.reset()
                    }
                }

            }

        }

        override fun reset() {
            super.reset()
            typeId = null
        }
    }

}
