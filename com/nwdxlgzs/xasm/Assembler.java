/*
 * Xasm汇编文件结构：
 * 1：.function到.end之间为一个函数原型
 * 2：.function后跟索引路径，根为main，索引路径用/分割，如main/0/1/2
 * 3：.sub-parse <bool>是否这个汇编文件也解析了目标函数子函数
 * 4：.source <string>对应Proto的source，nil表示没有
 * 5：.is_vararg/.maxstacksize/.numparams/.linedefined/.lastlinedefined <int>对应Proto的is_vararg/maxstacksize/numparams/linedefined/lastlinedefined
 * 6：sizep/sizek/sizecode/sizeupvalues/sizelineinfo/sizelocvars等不使用，根据解析内容自动回补
 * 7：.locvars <string> <int> -> <int>对应Proto的locvars，第一个string为name（可以是nil），第二个int为startpc，第三个int为endpc
 * 8：.upvaldesc <string> <int> <bool>对应Proto的upvalues，第一个string为name（可以是nil），第一个int为idx，第一个bool为instack
 * 9：.code-start到.code-end为一个块，块内只允许指令数据或者.line <int>或者.goto_XX存在
 * 10：.line <int>对应Proto的lineinfo，int为行号（特别的，0不会显示，上一次的line和本次一致也会不显示）
 * 11：.goto_XX是辅助表示跳转指令的跳转目的地
 * 12：每个指令表示结构并不完全一样，请看case分支处注释。
 * 13：字符串转义规则：\t\n\b\f\"\r\\\0，\xXX表示16进制字符
 */
package com.nwdxlgzs.xasm;

import static com.nwdxlgzs.xasm.TValue.*;
import static com.nwdxlgzs.xasm.defines.*;

public class Assembler {
    private final CharSequence source;


    public Assembler(CharSequence source) {
        this.source = source;
    }

}
