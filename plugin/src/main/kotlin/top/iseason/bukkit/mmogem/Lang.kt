package top.iseason.bukkit.mmogem

import top.iseason.bukkittemplate.config.Lang
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key

@Key
@FilePath("lang.yml")
object Lang : Lang() {

    @Comment("消息最小时间间隔, 毫秒")
    var cooldown = 300L

    var inlay__failure = "你还需要 {num} 个 {material} 来拆卸该宝石"
    var inlay__none = "该宝石不能镶嵌"
    var inlay__only_one = "一次只能镶嵌一个宝石！"
    var inlay__out_success = "拆卸成功！"
    var inlay__in_success = "镶嵌成功！"

    var expand__success = "槽位扩充成功, 颜色 {color}"
    var expand__deny = "该物品无法开孔"

    var rebuild__deny = "该物品无法重塑"
    var rebuild__max = "已达到完美"
    var rebuild__no_max = "{0}该物品已达到保底"
    var rebuild__special = "{0}该物品已达到保底"
    var rebuild__has_gem = "请先把宝石全部卸下再重塑"
    var rebuild__success = "{0}物品重塑完成"
    var rebuild__no_money = "金币不够"

    var decompose__success = "&a本次分解获得了 {0}"
    var decompose__format = " {0} &7 X &a {1};"
    var decompose__drop = "&c背包已满，待分解物品掉落在地。"

}