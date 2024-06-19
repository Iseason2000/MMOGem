package top.iseason.bukkit.mmogem

import net.Indyuce.mmoitems.ItemStats
import net.Indyuce.mmoitems.MMOItems
import net.Indyuce.mmoitems.api.Type
import net.Indyuce.mmoitems.api.player.PlayerData
import net.Indyuce.mmoitems.stat.type.NameData
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.mmogem.stat.RebuildTimes
import top.iseason.bukkit.mmogem.ui.ExpandUIConfig
import top.iseason.bukkit.mmogem.ui.InlayUIConfig
import top.iseason.bukkit.mmogem.ui.RebuildUIConfig
import top.iseason.bukkittemplate.BukkitPlugin
import top.iseason.bukkittemplate.command.CommandHandler
import top.iseason.bukkittemplate.debug.info
import top.iseason.bukkittemplate.hook.MMOItemsHook
import top.iseason.bukkittemplate.hook.PlaceHolderHook
import top.iseason.bukkittemplate.ui.UIListener
import top.iseason.bukkittemplate.utils.bukkit.EventUtils.registerListener
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.applyMeta

@Suppress("UNUSED")
object MMOGem : BukkitPlugin {

    override fun onLoad() {
        MMOItems.plugin.stats.register(RebuildTimes)
    }

    override fun onEnable() {
        VaultHook.checkHooked()
        PlaceHolderHook.checkHooked()
        MMOItemsHook.checkHooked()
        UIListener.registerListener()
        Lang.load()
        Config.load()
        InlayUIConfig.load()
        ExpandUIConfig.load()
        RebuildUIConfig.load()

        command()
        CommandHandler.updateCommands()
        info("&a插件已启用!")
        FightPower.load()
        if (PlaceHolderHook.hasHooked)
            PAPIExpansion.register()
    }

    override fun onDisable() {
        info("&6插件已卸载!")
    }

    fun ItemStack.applyPlaceHolder(player: Player?, vararg custom: Pair<String, String>): ItemStack {
        return applyMeta {
            if (hasDisplayName()) {
                var temp = displayName
                for ((key, value) in custom) {
                    temp = temp.replace(key, value)
                }
                setDisplayName(PlaceHolderHook.setPlaceHolder(temp, player))
            }
            if (hasLore()) {
                lore = lore!!.map {
                    var temp = it
                    for ((key, value) in custom) {
                        temp = temp.replace(key, value)
                    }
                    PlaceHolderHook.setPlaceHolder(temp, player)
                }
            }
        }
    }

    fun getItemName(id: String): String {
        val split = id.split(':', limit = 2)
        val t = Type.get(split[0])!!
        val template = MMOItems.plugin.templates.getTemplate(t, split[1])!!
        return (template.baseItemData[ItemStats.NAME] as NameData).bake()
    }

}