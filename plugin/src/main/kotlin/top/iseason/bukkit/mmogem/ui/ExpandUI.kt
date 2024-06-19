package top.iseason.bukkit.mmogem.ui

import io.lumine.mythic.lib.UtilityMethods
import io.lumine.mythic.lib.api.item.NBTItem
import net.Indyuce.mmoitems.ItemStats
import net.Indyuce.mmoitems.MMOItems
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem
import net.Indyuce.mmoitems.stat.data.GemSocketsData
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.mmogem.Config
import top.iseason.bukkit.mmogem.Lang
import top.iseason.bukkittemplate.hook.PlaceHolderHook
import top.iseason.bukkittemplate.ui.container.ChestUI
import top.iseason.bukkittemplate.ui.slot.*
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.decrease
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage

class ExpandUI(val player: Player) :
    ChestUI(
        PlaceHolderHook.setPlaceHolder(ExpandUIConfig.title, player),
        ExpandUIConfig.row,
        ExpandUIConfig.clickDelay
    ) {

    init {
        for ((index, item) in ExpandUIConfig.background) {
            Icon(item, index).setup()
        }
    }

    val inputSlot = run {
        val (slots, item) = ExpandUIConfig.input
        val index = slots[0]
        InputSlot(index, item).setup()
    }

    val outputSlot = run {
        val (slots, item) = ExpandUIConfig.result
        val index = slots[0]
        OutputSlot(index, item).setup()
    }

    val materialSlot = run {
        val (slots, item) = ExpandUIConfig.material
        val index = slots[0]
        MaterialSlot(index, item).setup()
    }

    val button = run {
        val (slots, item) = ExpandUIConfig.button
        slots.map {
            ButtonSlot(it, item).setup()
        }
    }


    inner class InputSlot(index: Int, items: HashMap<String, ItemStack>) : IOSlot(index, items["default"]) {
        var mmoItem: LiveMMOItem? = null

        init {
            inputFilter {
                val nbtItem = NBTItem.get(it) ?: return@inputFilter false
                if (!nbtItem.hasType()) return@inputFilter false
                val type = MMOItems.getType(nbtItem)!!.id
                val id = MMOItems.getID(nbtItem)!!
                val typeId = UtilityMethods.enumName("$type:$id")
                val contains = Config.expandGemWhiteList.contains(typeId)
                if (!contains) {
                    val msg = PlaceHolderHook.setPlaceHolder(Lang.expand__deny, player)
                    player.sendColorMessage(msg)
                }
                contains
            }
            onInput(true) {
                val liveMMOItem = LiveMMOItem(it)
                mmoItem = liveMMOItem
                if (materialSlot.itemStack != null)
                    materialSlot.onInput.invoke(materialSlot, materialSlot.itemStack!!)
            }
            onOutput(true) {
                mmoItem = null
                materialSlot.eject(player)
            }
        }
    }

    inner class OutputSlot(index: Int, items: HashMap<String, ItemStack>) : IOSlot(index, items["default"]) {

        init {
            inputAble(false)
        }

    }

    inner class ButtonSlot(index: Int, items: HashMap<String, ItemStack>) : Button(items["default"], index) {
        private val available: ItemStack = items["available"]!!
        private val unavailable: ItemStack = items["unavailable"]!!
        private var state = 0

        init {
            onClicked {
                when (state) {
                    1 -> {
                        val pair = Config.expandGemMaterials[materialSlot.typeId]!!
                        val colors = pair.second
                        val mmoItem = inputSlot.mmoItem!!
                        val data =
                            mmoItem.getData(ItemStats.GEM_SOCKETS) as? GemSocketsData ?: GemSocketsData(ArrayList())
                        val color = colors.split(',').random()
                        data.addEmptySlot(color)
                        mmoItem.setData(ItemStats.GEM_SOCKETS, data)
                        outputSlot.itemStack = mmoItem.newBuilder().buildSilently()
                        val amount = materialSlot.itemStack!!.amount
                        if (amount == 1) materialSlot.reset()
                        else materialSlot.itemStack!!.decrease(1)
                        reset()
                        inputSlot.reset()
                        val msg =
                            PlaceHolderHook.setPlaceHolder(Lang.expand__success.replace("{color}", color), player)
                        player.sendColorMessage(msg)

                    }

                    else -> return@onClicked
                }
            }
        }

        fun setAvailable() {
            itemStack = available
            state = 1
        }

        fun setUnAvailable() {
            itemStack = unavailable
            state = 2
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
                val type = MMOItems.getType(nbtItem)!!.id
                val id = MMOItems.getID(nbtItem)!!
                typeId = UtilityMethods.enumName("$type:$id")
                Config.expandGemMaterials.containsKey(typeId)
            }
            onInput {
                val pair = Config.expandGemMaterials[typeId]!!
                val max = pair.first
                val mmoItem = inputSlot.mmoItem!!
                val data = mmoItem.getData(ItemStats.GEM_SOCKETS) as? GemSocketsData ?: GemSocketsData(ArrayList())
                val current = data.gems.size + data.emptySlots.size
                outputSlot.ejectSilently(player)
                if (current < max) {
                    for (buttonSlot in button) {
                        buttonSlot.setAvailable()
                    }
                } else {
                    for (buttonSlot in button) {
                        buttonSlot.setUnAvailable()
                    }
                    outputSlot.reset()
                }
            }

            onOutput {
                for (buttonSlot in button) {
                    buttonSlot.reset()
                }
                outputSlot.ejectSilently(player)
                outputSlot.reset()
                typeId = null
            }

        }

        override fun reset() {
            super.reset()
            typeId = null
        }
    }

}
