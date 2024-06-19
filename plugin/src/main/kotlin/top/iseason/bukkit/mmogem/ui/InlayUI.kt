package top.iseason.bukkit.mmogem.ui

import io.lumine.mythic.lib.UtilityMethods
import io.lumine.mythic.lib.api.item.NBTItem
import net.Indyuce.mmoitems.ItemStats
import net.Indyuce.mmoitems.MMOItems
import net.Indyuce.mmoitems.api.Type
import net.Indyuce.mmoitems.api.interaction.GemStone
import net.Indyuce.mmoitems.api.interaction.GemStone.ResultType.*
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem
import net.Indyuce.mmoitems.api.player.PlayerData
import net.Indyuce.mmoitems.stat.data.GemSocketsData
import net.Indyuce.mmoitems.stat.type.NameData
import net.Indyuce.mmoitems.util.MMOUtils
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.mmogem.Config
import top.iseason.bukkit.mmogem.Lang
import top.iseason.bukkit.mmogem.MMOGem.applyPlaceHolder
import top.iseason.bukkittemplate.hook.PlaceHolderHook
import top.iseason.bukkittemplate.ui.container.ChestUI
import top.iseason.bukkittemplate.ui.slot.*
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.decrease
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.other.EasyCoolDown
import java.util.*
import kotlin.collections.HashMap

class InlayUI(val player: Player) :
    ChestUI(
        PlaceHolderHook.setPlaceHolder(InlayUIConfig.title, player),
        InlayUIConfig.row,
        InlayUIConfig.clickDelay
    ) {

    init {
        for ((index, item) in InlayUIConfig.background) {
            Icon(item, index).setup()
        }
    }

    val inputSlot = run {
        val (slots, item) = InlayUIConfig.input
        val index = slots[0]
        InputSlot(index, item).setup()
    }

    val materialSlots = run {
        val (slots, item) = InlayUIConfig.material
        slots.map {
            MaterialSlot(it, item).setup()
        }
    }

    val gemSlots = run {
        val (slots, item) = InlayUIConfig.slots
        slots.map {
            GemSlot(it, item).setup()
        }
    }


    inner class InputSlot(index: Int, items: HashMap<String, ItemStack>) : IOSlot(index, items["default"]) {
        var mmoItem: LiveMMOItem? = null

        init {
            inputFilter {
                if (mmoItem != null) return@inputFilter false
                val get = NBTItem.get(it)
                get.hasTag(ItemStats.GEM_SOCKETS.nbtPath)
            }
            onInput(true) {
                val liveMMOItem = LiveMMOItem(it)
                mmoItem = liveMMOItem
                val data = liveMMOItem.getData(ItemStats.GEM_SOCKETS) as GemSocketsData
                val itr = liveMMOItem.extractGemstones().iterator()
                val itr2 = data.emptySlots.iterator()
                for (gemSlot in gemSlots) {
                    if (itr.hasNext()) {
                        val next = itr.next()
                        gemSlot.showGem(next.key.historicUUID, next.key.socketColor, next.value)
                    } else if (itr2.hasNext()) {
                        gemSlot.showAvailable(itr2.next())
                    } else gemSlot.reset()
                }
            }
            onOutput(true) {
                mmoItem = null
                for (gemSlot in gemSlots) {
                    gemSlot.reset()
                }
                for (materialSlot in materialSlots) {
                    materialSlot.ejectSilently(player)
                }

            }
        }
    }

    inner class GemSlot(index: Int, items: HashMap<String, ItemStack>) : IOSlot(index, items["default"]) {
        private var state = 0
        private var slotColor: String? = null
        private val available: ItemStack = items["available"]!!
        private var live: LiveMMOItem? = null
        private var placeHolder2: ItemStack = items["default"]!!
        private var uuid: UUID? = null

        init {
            // 装填
            inputFilter {
                if (this.itemStack != null) return@inputFilter false
                if (it.amount > 1) {
                    if (EasyCoolDown.check("${player.uniqueId}-ui_inlay", Lang.cooldown)) {
                        val msg = PlaceHolderHook.setPlaceHolder(Lang.inlay__only_one, player)
                        player.sendColorMessage(msg)
                    }
                    return@inputFilter false
                }
                if (inputSlot.mmoItem == null) return@inputFilter false
                val nbtItem = NBTItem.get(it) ?: return@inputFilter false
                val liveMMOItem = LiveMMOItem(nbtItem)
                if (liveMMOItem.type != Type.GEM_STONE) return@inputFilter false
                val gem = GemStone(PlayerData.get(player), nbtItem)
                val inputMMOItem = inputSlot.mmoItem!!
                val applyResult = gem.applyOntoItem(
                    inputMMOItem,
                    inputMMOItem.type,
                    MMOUtils.getDisplayName(inputMMOItem.nbt.item),
                    true,
                    false
                )
                when (applyResult.type) {
                    FAILURE -> true
                    SUCCESS -> {
                        inputSlot.itemStack = applyResult.result
                        val msg = PlaceHolderHook.setPlaceHolder(Lang.inlay__in_success, player)
                        player.sendColorMessage(msg)
                        true
                    }

                    NONE -> {
                        if (EasyCoolDown.check("${player.uniqueId}-ui_inlay", Lang.cooldown)) {
                            val msg = PlaceHolderHook.setPlaceHolder(
                                Lang.inlay__none, player
                            )
                            player.sendColorMessage(msg)
                        }
                        false
                    }
                }
            }
            onInput {
                inputSlot.onInput.invoke(inputSlot, inputSlot.itemStack!!)
            }
            // 拆卸
            outputFilter {
                if (state != 2) return@outputFilter false
                val pair = Config.removeGemMaterials[live!!.id] ?: return@outputFilter false
                val type = pair.first
                var need = pair.second
                val result = materialSlots.any {
                    if (it.typeId != type) return@any false
                    val num = it.itemStack?.amount ?: return@any false
                    need -= num
                    need <= 0
                }
                if (!result) {
                    if (EasyCoolDown.check("${player.uniqueId}-ui_inlay", Lang.cooldown)) {
                        val split = type.split(':', limit = 2)
                        val t = Type.get(split[0])!!
                        val template = MMOItems.plugin.templates.getTemplate(t, split[1])!!
                        val name = (template.baseItemData[ItemStats.NAME] as NameData).bake()
                        val msg = PlaceHolderHook.setPlaceHolder(
                            Lang.inlay__failure
                                .replace("{num}", need.toString())
                                .replace("{material}", name), player
                        )
                        player.sendColorMessage(msg)
                    }
                }
                result
            }
            onOutput {
                val pair = Config.removeGemMaterials[live!!.id] ?: return@onOutput
                val type = pair.first
                var need = pair.second
                for (materialSlot in materialSlots) {
                    if (materialSlot.typeId != type) continue
                    val itemStack = materialSlot.itemStack ?: continue
                    val amount = itemStack.amount
                    if (amount > need) {
                        itemStack.decrease(need)
                    } else {
                        materialSlot.reset()
                        need -= amount
                        if (need <= 0) break
                    }
                }
                val mmo = inputSlot.mmoItem!!
                mmo.removeGemStone(uuid!!, slotColor!!)
                val buildSilently = mmo.newBuilder().buildSilently()
                inputSlot.itemStack = buildSilently
                inputSlot.onInput.invoke(inputSlot, buildSilently)
                val msg = PlaceHolderHook.setPlaceHolder(
                    Lang.inlay__out_success, player
                )
                player.sendColorMessage(msg)
            }
        }

        fun showGem(uuid: UUID, color: String?, mmoItem: MMOItem) {
            itemStack = mmoItem.newBuilder().buildSilently()
            state = 2
            slotColor = color
            live = LiveMMOItem(itemStack)
            this.uuid = uuid
        }

        fun showAvailable(color: String) {
            placeholder = available.clone().applyPlaceHolder(player, "{color}" to color)
            itemStack = null
            state = 1
            slotColor = color
        }

        override fun reset() {
            placeholder = placeHolder2
            super.reset()
            state = 0
            slotColor = null
            live = null
            uuid = null
        }

    }

    inner class MaterialSlot(index: Int, items: HashMap<String, ItemStack>) : IOSlot(index, items["default"]) {
        var typeId: String? = null

        init {
            lockable(false)
            inputFilter {
                val nbtItem = NBTItem.get(it) ?: return@inputFilter false
                if (!nbtItem.hasType()) return@inputFilter false
                val type = MMOItems.getType(nbtItem)!!.id
                val id = MMOItems.getID(nbtItem)!!
                typeId = UtilityMethods.enumName("$type:$id")
                true
            }
            onOutput {
                typeId = null
            }

        }

        override fun reset() {
            super.reset()
            typeId = null
        }
    }

}
