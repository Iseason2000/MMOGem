package top.iseason.bukkit.mmogem

import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.mmogem.ui.DecomposeUI
import top.iseason.bukkit.mmogem.ui.ExpandUI
import top.iseason.bukkit.mmogem.ui.InlayUI
import top.iseason.bukkit.mmogem.ui.RebuildUI
import top.iseason.bukkittemplate.command.command
import top.iseason.bukkittemplate.command.executor
import top.iseason.bukkittemplate.command.node
import top.iseason.bukkittemplate.utils.bukkit.SchedulerUtils.submit

fun command() {
    command("mmogem") {
        description = "测试命令1"
        alias = arrayOf("mg", "mgem", "gem")
        node("inlay") {
            description = "镶嵌/拆卸宝石"
            default = PermissionDefault.TRUE
            async = true
            isPlayerOnly = true
            executor { _, sender ->
                val player = sender as Player
                val build = InlayUI(player).build()
                submit {
                    player.openInventory(build)
                }
            }
        }
        node("expand") {
            description = "宝石开孔"
            default = PermissionDefault.TRUE
            async = true
            isPlayerOnly = true
            executor { _, sender ->
                val player = sender as Player
                val build = ExpandUI(player).build()
                submit {
                    player.openInventory(build)
                }
            }
        }
        node("rebuild") {
            description = "物品重塑"
            default = PermissionDefault.TRUE
            async = true
            isPlayerOnly = true
            executor { _, sender ->
                val player = sender as Player
                val build = RebuildUI(player).build()
                submit {
                    player.openInventory(build)
                }
            }
        }
        node("decompose") {
            description = "物品分解"
            default = PermissionDefault.TRUE
            async = true
            isPlayerOnly = true
            executor { _, sender ->
                val player = sender as Player
                val build = DecomposeUI(player).build()
                submit {
                    player.openInventory(build)
                }
            }
        }
    }
}