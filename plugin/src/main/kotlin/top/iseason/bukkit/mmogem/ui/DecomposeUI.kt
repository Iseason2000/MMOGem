package top.iseason.bukkit.mmogem.ui

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.mmogem.DecomposeConfig
import top.iseason.bukkit.mmogem.Lang
import top.iseason.bukkittemplate.hook.MMOItemsHook
import top.iseason.bukkittemplate.hook.PlaceHolderHook
import top.iseason.bukkittemplate.ui.container.ChestUI
import top.iseason.bukkittemplate.ui.slot.*
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.giveItems
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.applyMeta
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.getDisplayName
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessages
import top.iseason.bukkittemplate.utils.other.RandomUtils
import java.util.ArrayList
import kotlin.collections.HashMap
import kotlin.math.ceil
import kotlin.math.roundToInt

class DecomposeUI(val player: Player) :
    ChestUI(
        PlaceHolderHook.setPlaceHolder(DecomposeUIConfig.title, player),
        DecomposeUIConfig.row,
        DecomposeUIConfig.clickDelay
    ) {

    init {
        for ((index, item) in DecomposeUIConfig.background) {
            Icon(item, index).setup()
        }
    }

    val inputSlots = run {
        val (slots, item) = DecomposeUIConfig.input
        slots.map {
            InputSlot(it, item).setup()
        }
    }

    val materialSlots = run {
        val (slots, item) = DecomposeUIConfig.material
        slots.map {
            MaterialSlot(it, item).setup()
        }
    }

    val button = run {
        val (slots, item) = DecomposeUIConfig.button
        slots.map {
            ButtonSlot(it, item).setup()
        }
    }

    inner class InputSlot(index: Int, items: HashMap<String, ItemStack>) : IOSlot(index, items["default"]) {
        var decomposes: DecomposeConfig.Decomposes? = null

        init {
            inputFilter {
                val typeId = MMOItemsHook.getMMOItemsId(it) ?: return@inputFilter false
                decomposes = DecomposeConfig.items[typeId] ?: return@inputFilter false
                true
            }
            onInput(true) {
                update()
            }
            onOutput(true) {
                if (itemStack == null) {
                    decomposes = null
                }
                update()
            }
        }

        override fun reset() {
            super.reset()
            decomposes = null
            update()
        }

        fun update() {
            val rates = hashMapOf<ItemStack, Triple<DecomposeConfig.Decompose, Double, Double>>()
            for (slot in inputSlots) {
                val decomposes = slot.decomposes ?: continue
                val itemStack1 = slot.itemStack ?: continue
                repeat(itemStack1.amount) {
                    for (decompose in decomposes.result) {
                        val item = decompose.item
                        var (_, faultRate, num) = rates[item] ?: Triple(decompose, 1.0, 0.0)
                        num += ((decompose.max - decompose.min) / 2.0 + decompose.min) * (decompose.rate / 100.0)
                        rates[item] = Triple(decompose, (faultRate * (1 - decompose.rate / 100.0)), num)
                    }
                }
            }
            val sort =
                rates.values.sortedBy { it.second }.sortedByDescending { DecomposeConfig.weights[it.first.key] ?: 0.0 }
            val iterator = sort.iterator()
            val append = DecomposeUIConfig.materialSection.getStringList("lore")
            for (slot in materialSlots) {
                if (iterator.hasNext()) {
                    val (first, faultRate, num) = iterator.next()
                    slot.itemStack = first.item.clone().applyMeta {
                        if (append.isNotEmpty()) {
                            val replace = append.map {
                                PlaceHolderHook.setPlaceHolder(
                                    it.replace(
                                        "{rate}",
                                        ceil((1 - faultRate) * 100).toInt().toString()
                                    ).replace("{num}", num.roundToInt().toString()), player
                                )
                            }
                            if (hasLore()) {
                                val lore1 = lore!!
                                lore1.addAll(replace)
                                lore = lore1
                            } else lore = replace
                        }
                    }
                } else {
                    slot.reset()
                }
            }
            if (sort.isNotEmpty()) {
                for (slot in button) {
                    slot.setAvailable()
                }
            } else {
                for (slot in button) {
                    slot.reset()
                }
            }
        }
    }


    inner class MaterialSlot(index: Int, items: HashMap<String, ItemStack>) : IOSlot(index, items["default"]) {
        init {
            lockable(true)
        }

        override fun reset() {
            super.reset()
        }
    }

    inner class ButtonSlot(index: Int, items: HashMap<String, ItemStack>) : Button(items["default"], index) {
        private val available: ItemStack = items["available"]!!

        private var state = 0

        init {
            onClicked {
                if (state != 1) return@onClicked

                val map = hashMapOf<String, Pair<ItemStack, Int>>()
                val results = ArrayList<ItemStack>()
                for (slot in inputSlots) {
                    val decomposes = slot.decomposes ?: continue
                    val itemStack1 = slot.itemStack ?: continue
                    repeat(itemStack1.amount) {
                        for (decompose in decomposes.result) {
                            if (RandomUtils.checkPercentage(decompose.rate)) {
                                continue
                            }
                            val amount = RandomUtils.getInteger(decompose.min, decompose.max)
                            val clone = decompose.item.clone()
                            clone.amount = amount
                            val (_, acc) = map[decompose.key] ?: (clone to 0)
                            map[decompose.key] = clone to acc + amount
                            results.add(clone)
                        }
                    }
                    player.sendColorMessages(decomposes.msg)
                }

                val isDrop = player.giveItems(results)
                reset()
                for (slot in inputSlots) {
                    slot.reset()
                }
                for (slot in materialSlots) {
                    slot.reset()
                }
                val builder = StringBuilder()

                for ((_, pair) in map) {
                    builder.append(
                        Lang.decompose__format.formatBy(
                            pair.first.getDisplayName() ?: pair.first.type,
                            pair.second
                        )
                    )
                }
                player.sendColorMessage(Lang.decompose__success.formatBy(builder.toString()))
                if (isDrop) {
                    player.sendColorMessage(Lang.decompose__drop)
                }
            }
        }

        fun setAvailable() {
            itemStack = available
            state = 1
        }

        override fun reset() {
            super.reset()
            state = 0
        }


    }

}
