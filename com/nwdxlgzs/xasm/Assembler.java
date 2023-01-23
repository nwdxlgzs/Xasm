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
    private final Proto proto;
    private boolean checkIsSub = false;

    public Assembler(Proto proto) {
        this.proto = proto;
    }

    public void setCheckIsSub(boolean ck) {
        checkIsSub = ck;
    }

    public boolean getCheckIsSub() {
        return checkIsSub;
    }

    private int getSubIndex(Proto f, Proto target, int[] map, int deep, int[][] ret) {
        if (target == null || target == proto) return -1;
        if (f == null) f = proto;
        if (map == null || map.length <= deep) {
            map = realloc(map, deep + 1);
            ret[0] = map;
        }
        for (int i = 0; i < f.sizep(); i++) {
            map[deep] = i;
            if (f.p[i] == target) return i;
            int sub = getSubIndex(f.p[i], target, map, deep + 1, ret);
            if (sub != -1) return sub;
        }
        return -1;
    }


    //f:哪个Proto（父子Proto指定层级）
    //needNote:是否需要辅助性注释
    //likeActCode:是否需要指令码转成生动的行为符号（如：OP_ADD用+代替）
    //needSubXasm:是否要解析子函数汇编
    public String getXasm(Proto f, boolean needNote, boolean likeActCode, boolean needSubXasm) {
        if (f == null) f = proto;
        StringBuilder sb = new StringBuilder();
        if (f != proto) {
            int[][] pIndex = new int[1][];
            int find = getSubIndex(proto, f, null, 0, pIndex);
            if (find == -1) throw new IllegalArgumentException("没有找到目标函数原型");
            for (int i = 0; i < pIndex[0].length; i++) {
                sb.append("/");
                sb.append(pIndex[0][i]);
            }
        }
        return getXasm(f, sb.toString(), needNote, likeActCode, needSubXasm);
    }

    public String getXasm(Proto f, String subPath, boolean needNote, boolean likeActCode, boolean needSubXasm) {
        int i;
        if (f == null) f = proto;
        if (checkIsSub) {
            Proto p = proto.findSub(subPath == null ? "" : subPath);
            if (p != f) {
                throw new IllegalArgumentException("安全检查未通过，当前索引和传入的函数原型并非一个。");
            }
        }
        bindInst.bind(f);//识别会跳跃到的位置等信息，方便统一打标签等
        StringBuilder sb = new StringBuilder();
        sb.append(".function main");
        sb.append(subPath == null ? "" : subPath);
        if (needNote) {
            if (f != proto) {
                sb.append(" $ 当前根索引函数原型路径");
            } else {
                sb.append(" $ 当前函数原型是根");
            }
        }
        sb.append("\n");
        sb.append(".sub-parse ");
        sb.append(needSubXasm);
        if (needNote) {
            if (needSubXasm) {
                sb.append(" $ 本Xasm也会解析子函数");
            } else {
                sb.append(" $ 本Xasm只解析本函数，子函数将不进行解析");
            }
        }
        sb.append("\n");
        sb.append(".source ");
        if (f.source == null || f.source.equals(NIL)) {
            sb.append("null");
        } else {
            sb.append(f.source.toWarpString());
        }
        if (needNote) {
            if (f.source == null || f.source.equals(NIL)) {
                sb.append(" $ 当前函数原型没有指定源文件");
            } else {
                byte[] buf = (byte[]) f.source.value_;
                if (buf.length > 0) {
                    if (buf[0] == '@') {
                        sb.append(" $ 当前函数原型的源文件路径");
                    } else {
                        sb.append(" $ 当前函数原型的源内容");
                    }
                } else {
                    sb.append(" $ 当前函数原型的源文件未填写");
                }
            }
        }
        sb.append("\n");
        sb.append(".is_vararg ").append(f.is_vararg);
        if (needNote) {
            if (f.is_vararg == 0) {
                sb.append(" $ 没使用变参");
            } else if (f.is_vararg == 1) {
                sb.append(" $ 使用了变参作为最后一个参数");
            } else if (f.is_vararg == 2) {
                sb.append(" $ 使用了变参作为最后一个参数（根一定是）");
            } else {
                sb.append(" $ 是否支持变参：0表示没使用，1表示使用了变参作为最后一个参数，2表示根");
            }
        }
        sb.append("\n");
        sb.append(".maxstacksize ").append(f.maxstacksize);
        if (needNote) {
            sb.append(" $ 最大栈大小，合法值为0~250（255是理论的，但是虚拟机支持到250）");
        }
        sb.append("\n");
        sb.append(".numparams ").append(f.numparams);
        if (needNote) {
            sb.append(" $ 固定的形参个数");
        }
        sb.append("\n");
        sb.append(".linedefined ").append(f.linedefined);
        if (needNote) {
            if (f != proto) {
                sb.append(" $ 起始定义的行号");
            } else {
                sb.append(" $ 起始定义的行号，但是根一般是0");
            }
        }
        sb.append("\n");
        sb.append(".lastlinedefined ").append(f.lastlinedefined);
        if (needNote) {
            if (f != proto) {
                sb.append(" $ 结束定义的行号");
            } else {
                sb.append(" $ 结束定义的行号，但是根一般是0");
            }
        }
        sb.append("\n");
        sb.append("\n");//成组出现，多加入一次换行
        int sizelocvars = f.sizelocvars();
        for (i = 0; i < sizelocvars; i++) {
            if (needNote && i == 0) {
                sb.append("$ 局部变量名 起始位置(指令) -> 回收位置(指令)").append("\n");
            }
            LocVar lv = f.locvars[i];
            sb.append(".locvars ")
                    .append(lv.varname.toVarString()).append(" ")
                    .append(lv.startpc).append(" -> ").append(lv.endpc);
            sb.append("\n");
        }
        sb.append("\n");//成组出现，多加入一次换行
        int sizeupvalues = f.sizeupvalues();
        for (i = 0; i < sizeupvalues; i++) {
            if (needNote && i == 0) {
                sb.append("$ 上值变量名 索引编号 是否在栈中").append("\n");
            }
            Upvaldesc uv = f.upvalues[i];
            sb.append(".upvaldesc ")
                    .append(uv.name.toVarString()).append(" ")
                    .append(uv.idx).append(" ").append(uv.instack);
            sb.append("\n");
        }
        sb.append("\n");//成组出现，多加入一次换行
        if (needNote) {
            sb.append("$ op操作码 各种操作数 rXX表示XX寄存器，uXX表示XX上值，pXX表示XX子函数，kXX表示第XX常量（只有找不到该常量才会出现kXX），goto_XX表示跳跃的XX标签，直接是数字表示数字，一般按A,B,C,Bx,sBx,Ax顺序表示").append("\n");
            sb.append("$ .goto_XX是跳跃标签，.line XX是行号标签（特别的，0不会显示，上一次的line和本次一致也会不显示）").append("\n");
        }
        sb.append(".code-start");
        sb.append("\n");
        int sizecode = f.sizecode();
        int sizelineinfo = f.sizelineinfo();
        int lastline = 0;
//        int sizek = f.sizek();
        i = -1;//先++到0
        while (true) {
            i++;
            if (i >= sizecode) {
                break;
            }
            if (sizelineinfo > 0 && i < sizelineinfo) {
                int line = f.lineinfo[i];
                if (line != 0 && line != lastline) {
                    sb.append(".line ").append(line).append("\n");
                }
                lastline = line;
            }
            Instruction instruction = f.code[i];
            OPCode opcode = instruction.getOpCode();
            if (instruction.isCanJump2There) {
                sb.append(".goto_").append(i).append("\n");
            }
            if (likeActCode) {
                switch (opcode) {
                    case OP_ADD: {
                        sb.append("+ ");
                        break;
                    }
                    case OP_SUB: {
                        sb.append("- ");
                        break;
                    }
                    case OP_MUL: {
                        sb.append("* ");
                        break;
                    }
                    case OP_MOD: {
                        sb.append("% ");
                        break;
                    }
                    case OP_POW: {
                        sb.append("^ ");
                        break;
                    }
                    case OP_DIV: {
                        sb.append("/ ");
                        break;
                    }
                    case OP_IDIV: {
                        sb.append("// ");
                        break;
                    }
                    case OP_BAND: {
                        sb.append("& ");
                        break;
                    }
                    case OP_BOR: {
                        sb.append("| ");
                        break;
                    }
                    case OP_BXOR: {
                        sb.append("~ ");
                        break;
                    }
                    case OP_SHL: {
                        sb.append("<< ");
                        break;
                    }
                    case OP_SHR: {
                        sb.append(">> ");
                        break;
                    }
                    case OP_UNM: {
                        sb.append("-() ");
                        break;
                    }
                    case OP_BNOT: {
                        sb.append("~() ");
                        break;
                    }
                    case OP_LEN: {
                        sb.append("# ");
                        break;
                    }
                    case OP_CONCAT: {
                        sb.append(".. ");
                        break;
                    }
                    case OP_NEWTABLE: {
                        sb.append("{} ");
                        break;
                    }
                    case OP_NEWARRAY: {
                        sb.append("[] ");
                        break;
                    }
                    case OP_CLOSURE: {
                        sb.append("func ");
                        break;
                    }
                    case OP_EQ: {
                        if (instruction.A() == 0) {
                            sb.append("~= ");
                        } else {
                            sb.append("== ");
                        }
                        break;
                    }
                    case OP_LT: {
                        if (instruction.A() != 0) {
                            sb.append("< ");
                        } else {
                            sb.append("> ");
                        }
                        break;
                    }
                    case OP_LE: {
                        if (instruction.A() != 0) {
                            sb.append("<= ");
                        } else {
                            sb.append("=> ");
                        }
                        break;
                    }
                    default: {
                        sb.append(opcode.getName()).append(" ");
                        break;
                    }
                }
            } else {
                sb.append(opcode.getName()).append(" ");
            }
            switch (opcode) {
                case OP_MOVE: {//rA rB
                    sb.append("r").append(instruction.A()).append(" ")
                            .append("r").append(instruction.B());
                    if (needNote) {
                        sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值赋值给寄存器（r").append(instruction.A()).append("）");
                    }
                    break;
                }
                case OP_LOADK: {//rA <value>
                    sb.append("r").append(instruction.A()).append(" ")
                            .append(safeToVarString(f, instruction.Bx()));
                    if (needNote) {
                        sb.append(" $ 加载常量（").append(safeToVarString(f, instruction.Bx())).append("）到寄存器（r").append(instruction.A()).append("）");
                    }
                    break;
                }
                case OP_LOADKX: {//rA <value>
                    sb.append("r").append(instruction.A()).append(" ");
                    i++;
                    Instruction instruction2 = f.code[i];
                    sb.append(safeToVarString(f, instruction2.Ax()));
                    if (needNote) {
                        sb.append(" $ 加载常量（").append(safeToVarString(f, instruction.Ax())).append("）到寄存器（r").append(instruction.A()).append("）");
                    }
                    break;
                }
                case OP_LOADBOOL: {//rA <bool>或者rA <bool> <bool>或者rA <bool> goto_XX
                    sb.append("r").append(instruction.A()).append(" ")
                            .append(instruction.B() != 0).append(" ");
                    if (instruction.C() != 0) {
                        sb.append("goto_").append(i + 1 + 1);
                    }
                    if (needNote) {
                        sb.append(" $ 加载布尔值（").append(instruction.B() != 0).append("）到寄存器（r").append(instruction.A()).append("）");
                        if (instruction.C() != 0) {
                            sb.append("，并跳过下一条指令");
                        }
                    }
                    break;
                }
                case OP_LOADNIL: {//rA r(A+B)或者rA <B>
                    sb.append("r").append(instruction.A()).append(" ")
                            .append("r").append(instruction.B() + instruction.A());
                    if (needNote) {
                        sb.append(" $ 将寄存器（r").append(instruction.A()).append("）到寄存器（r").append(instruction.B() + instruction.A()).append("）闭区间的值设置为nil");
                    }
                    break;
                }
                case OP_GETUPVAL: {//rA uB
                    sb.append("r").append(instruction.A()).append(" ")
                            .append("u").append(instruction.B());
                    if (needNote) {
                        sb.append(" $ 将上值（u").append(instruction.B()).append("）的值赋值给寄存器（r").append(instruction.A()).append("）");
                    }
                    break;
                }
                case OP_GETTABUP: {//rA uB rC或者rA uB <value>
                    sb.append("r").append(instruction.A()).append(" ")
                            .append("u").append(instruction.B()).append(" ");
                    if (ISK(instruction.C())) {
                        sb.append(safeToVarString(f, INDEXK(instruction.C())));
                        if (needNote) {
                            sb.append(" $ 将上值（u").append(instruction.B()).append("）的值的键为常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）的值赋值给寄存器（r").append(instruction.A()).append("）");
                        }
                    } else {
                        sb.append("r").append(instruction.C());
                        if (needNote) {
                            sb.append(" $ 将上值（u").append(instruction.B()).append("）的值的键为寄存器（r").append(instruction.C()).append("）的值赋值给寄存器（r").append(instruction.A()).append("）");
                        }
                    }
                    break;
                }
                case OP_GETTABLE: {//rA rB rC或者rA rB <value>
                    sb.append("r").append(instruction.A()).append(" ")
                            .append("r").append(instruction.B()).append(" ");
                    if (ISK(instruction.C())) {
                        sb.append(safeToVarString(f, INDEXK(instruction.C())));
                        if (needNote) {
                            sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值的键为常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）的值赋值给寄存器（r").append(instruction.A()).append("）");
                        }
                    } else {
                        sb.append("r").append(instruction.C());
                        if (needNote) {
                            sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值的键为寄存器（r").append(instruction.C()).append("）的值赋值给寄存器（r").append(instruction.A()).append("）");
                        }
                    }
                    break;
                }
                case OP_SETTABUP: {//uA <value> <value>或者uA rB <value>或者uA <value> rC或者uA rB rC
                    sb.append("u").append(instruction.A()).append(" ");
                    if (ISK(instruction.B())) {
                        sb.append(safeToVarString(f, INDEXK(instruction.B()))).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将上值（u").append(instruction.A()).append("）的值的键为常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）的值设置为常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将上值（u").append(instruction.A()).append("）的值的键为常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）的值设置为寄存器（r").append(instruction.C()).append("）的值");
                            }
                        }
                    } else {
                        sb.append("r").append(instruction.B()).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将上值（u").append(instruction.A()).append("）的值的键为寄存器（r").append(instruction.B()).append("）的值设置为常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将上值（u").append(instruction.A()).append("）的值的键为寄存器（r").append(instruction.B()).append("）的值设置为寄存器（r").append(instruction.C()).append("）的值");
                            }
                        }
                    }
                    break;
                }
                case OP_SETUPVAL: {//uB rA
                    sb.append("u").append(instruction.B()).append(" ")
                            .append("r").append(instruction.A());
                    if (needNote) {
                        sb.append(" $ 将寄存器（r").append(instruction.A()).append("）的值赋值给上值（u").append(instruction.B()).append("）");
                    }
                    break;
                }
                case OP_SETTABLE: {//rA <value> <value>或者rA rB <value>或者rA <value> rC或者rA rB rC
                    sb.append("r").append(instruction.A()).append(" ");
                    if (ISK(instruction.B())) {
                        sb.append(safeToVarString(f, INDEXK(instruction.B()))).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.A()).append("）的值的键为常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）的值设置为常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.A()).append("）的值的键为常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）的值设置为寄存器（r").append(instruction.C()).append("）的值");
                            }
                        }
                    } else {
                        sb.append("r").append(instruction.B()).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.A()).append("）的值的键为寄存器（r").append(instruction.B()).append("）的值设置为常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.A()).append("）的值的键为寄存器（r").append(instruction.B()).append("）的值设置为寄存器（r").append(instruction.C()).append("）的值");
                            }
                        }
                    }
                    break;
                }
                case OP_NEWTABLE: {//rA <B> <C>
                    sb.append("r").append(instruction.A()).append(" ")
                            .append(instruction.B()).append(" ")
                            .append(instruction.C());
                    if (needNote) {
                        sb.append(" $ 创建一个新的表，将其赋值给寄存器（r").append(instruction.A()).append("），并将其数组部分的大小设置为（").append(instruction.B()).append("），哈希部分的大小设置为（").append(instruction.C()).append("）");
                    }
                    break;
                }
                case OP_SELF: {//rA rB <value>或者rA rB rC
                    sb.append("r").append(instruction.A()).append(" ")
                            .append("r").append(instruction.B()).append(" ");
                    if (ISK(instruction.C())) {
                        sb.append(safeToVarString(f, INDEXK(instruction.C())));
                        if (needNote) {
                            sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值的键为常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）的值赋值给寄存器（r").append(instruction.A()).append("），并将寄存器（r").append(instruction.B()).append("）的值赋值给寄存器（r").append(instruction.A() + 1).append("）");
                        }
                    } else {
                        sb.append("r").append(instruction.C());
                        if (needNote) {
                            sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值的键为寄存器（r").append(instruction.C()).append("）的值赋值给寄存器（r").append(instruction.A()).append("），并将寄存器（r").append(instruction.B()).append("）的值赋值给寄存器（r").append(instruction.A() + 1).append("）");
                        }
                    }
                    break;
                }
                case OP_ADD: {//rA <value> <value>或者rA rB <value>或者rA <value> rC或者rA rB rC
                    sb.append("r").append(instruction.A()).append(" ");
                    if (ISK(instruction.B())) {
                        sb.append(safeToVarString(f, INDEXK(instruction.B()))).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）与常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）相加，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）与寄存器（r").append(instruction.C()).append("）的值相加，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    } else {
                        sb.append("r").append(instruction.B()).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值与常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）相加，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值与寄存器（r").append(instruction.C()).append("）的值相加，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    }
                    break;
                }
                case OP_SUB: {//rA <value> <value>或者rA rB <value>或者rA <value> rC或者rA rB rC
                    sb.append("r").append(instruction.A()).append(" ");
                    if (ISK(instruction.B())) {
                        sb.append(safeToVarString(f, INDEXK(instruction.B()))).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）与常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）相减，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）与寄存器（r").append(instruction.C()).append("）的值相减，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    } else {
                        sb.append("r").append(instruction.B()).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值与常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）相减，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值与寄存器（r").append(instruction.C()).append("）的值相减，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    }
                    break;
                }
                case OP_MUL: {//rA <value> <value>或者rA rB <value>或者rA <value> rC或者rA rB rC
                    sb.append("r").append(instruction.A()).append(" ");
                    if (ISK(instruction.B())) {
                        sb.append(safeToVarString(f, INDEXK(instruction.B()))).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）与常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）相乘，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）与寄存器（r").append(instruction.C()).append("）的值相乘，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    } else {
                        sb.append("r").append(instruction.B()).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值与常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）相乘，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值与寄存器（r").append(instruction.C()).append("）的值相乘，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    }
                    break;
                }
                case OP_MOD: {//rA <value> <value>或者rA rB <value>或者rA <value> rC或者rA rB rC
                    sb.append("r").append(instruction.A()).append(" ");
                    if (ISK(instruction.B())) {
                        sb.append(safeToVarString(f, INDEXK(instruction.B()))).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）与常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）求余，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）与寄存器（r").append(instruction.C()).append("）的值求余，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    } else {
                        sb.append("r").append(instruction.B()).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值与常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）求余，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值与寄存器（r").append(instruction.C()).append("）的值求余，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    }
                    break;
                }
                case OP_POW: {//rA <value> <value>或者rA rB <value>或者rA <value> rC或者rA rB rC
                    sb.append("r").append(instruction.A()).append(" ");
                    if (ISK(instruction.B())) {
                        sb.append(safeToVarString(f, INDEXK(instruction.B()))).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）的值的常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）次方，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）的值的寄存器（r").append(instruction.C()).append("）的值次方，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    } else {
                        sb.append("r").append(instruction.B()).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值的常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）次方，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值的寄存器（r").append(instruction.C()).append("）的值次方，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    }
                    break;
                }
                case OP_DIV: {//rA <value> <value>或者rA rB <value>或者rA <value> rC或者rA rB rC
                    sb.append("r").append(instruction.A()).append(" ");
                    if (ISK(instruction.B())) {
                        sb.append(safeToVarString(f, INDEXK(instruction.B()))).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）的值除以常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）的值，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）的值除以寄存器（r").append(instruction.C()).append("）的值，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    } else {
                        sb.append("r").append(instruction.B()).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值除以常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）的值，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值除以寄存器（r").append(instruction.C()).append("）的值，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    }
                    break;
                }
                case OP_IDIV: {//rA <value> <value>或者rA rB <value>或者rA <value> rC或者rA rB rC
                    sb.append("r").append(instruction.A()).append(" ");
                    if (ISK(instruction.B())) {
                        sb.append(safeToVarString(f, INDEXK(instruction.B()))).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）的值整除以常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）的值，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）的值整除以寄存器（r").append(instruction.C()).append("）的值，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    } else {
                        sb.append("r").append(instruction.B()).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值整除以常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）的值，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值整除以寄存器（r").append(instruction.C()).append("）的值，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    }
                    break;
                }
                case OP_BAND: {//rA <value> <value>或者rA rB <value>或者rA <value> rC或者rA rB rC
                    sb.append("r").append(instruction.A()).append(" ");
                    if (ISK(instruction.B())) {
                        sb.append(safeToVarString(f, INDEXK(instruction.B()))).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）的值与常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）的值进行按位与运算，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）的值与寄存器（r").append(instruction.C()).append("）的值进行按位与运算，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    } else {
                        sb.append("r").append(instruction.B()).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值与常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）的值进行按位与运算，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值与寄存器（r").append(instruction.C()).append("）的值进行按位与运算，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    }
                    break;
                }
                case OP_BOR: {//rA <value> <value>或者rA rB <value>或者rA <value> rC或者rA rB rC
                    sb.append("r").append(instruction.A()).append(" ");
                    if (ISK(instruction.B())) {
                        sb.append(safeToVarString(f, INDEXK(instruction.B()))).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）的值与常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）的值进行按位或运算，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）的值与寄存器（r").append(instruction.C()).append("）的值进行按位或运算，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    } else {
                        sb.append("r").append(instruction.B()).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值与常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）的值进行按位或运算，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值与寄存器（r").append(instruction.C()).append("）的值进行按位或运算，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    }
                    break;
                }
                case OP_BXOR: {//rA <value> <value>或者rA rB <value>或者rA <value> rC或者rA rB rC
                    sb.append("r").append(instruction.A()).append(" ");
                    if (ISK(instruction.B())) {
                        sb.append(safeToVarString(f, INDEXK(instruction.B()))).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）的值与常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）的值进行按位异或运算，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）的值与寄存器（r").append(instruction.C()).append("）的值进行按位异或运算，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    } else {
                        sb.append("r").append(instruction.B()).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值与常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）的值进行按位异或运算，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值与寄存器（r").append(instruction.C()).append("）的值进行按位异或运算，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    }
                    break;
                }
                case OP_SHL: {//rA <value> <value>或者rA rB <value>或者rA <value> rC或者rA rB rC
                    sb.append("r").append(instruction.A()).append(" ");
                    if (ISK(instruction.B())) {
                        sb.append(safeToVarString(f, INDEXK(instruction.B()))).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）的值左移常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）位，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）的值左移寄存器（r").append(instruction.C()).append("）的值位，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    } else {
                        sb.append("r").append(instruction.B()).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值左移常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）位，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值左移寄存器（r").append(instruction.C()).append("）的值位，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    }
                    break;
                }
                case OP_SHR: {//rA <value> <value>或者rA rB <value>或者rA <value> rC或者rA rB rC
                    sb.append("r").append(instruction.A()).append(" ");
                    if (ISK(instruction.B())) {
                        sb.append(safeToVarString(f, INDEXK(instruction.B()))).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）的值右移常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）位，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）的值右移寄存器（r").append(instruction.C()).append("）的值位，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    } else {
                        sb.append("r").append(instruction.B()).append(" ");
                        if (ISK(instruction.C())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.C())));
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值右移常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("）位，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        } else {
                            sb.append("r").append(instruction.C());
                            if (needNote) {
                                sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值右移寄存器（r").append(instruction.C()).append("）的值位，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                            }
                        }
                    }
                    break;
                }
                case OP_UNM: {//rA rB
                    sb.append("r").append(instruction.A()).append(" r").append(instruction.B());
                    if (needNote) {
                        sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值取反，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                    }
                    break;
                }
                case OP_BNOT: {//rA rB
                    sb.append("r").append(instruction.A()).append(" r").append(instruction.B());
                    if (needNote) {
                        sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的值按位取反，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                    }
                    break;
                }
                case OP_NOT: {//rA rB
                    sb.append("r").append(instruction.A()).append(" r").append(instruction.B());
                    if (needNote) {
                        sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的布尔值取反，将结果赋值给寄存器（r").append(instruction.A()).append("）");
                    }
                    break;
                }
                case OP_LEN: {//rA rB
                    sb.append("r").append(instruction.A()).append(" r").append(instruction.B());
                    if (needNote) {
                        sb.append(" $ 将寄存器（r").append(instruction.B()).append("）的长度赋值给寄存器（r").append(instruction.A()).append("）");
                    }
                    break;
                }
                case OP_CONCAT: {//rA rB rC
                    sb.append("r").append(instruction.A()).append(" r").append(instruction.B()).append(" r").append(instruction.C());
                    if (needNote) {
                        sb.append(" $ 将寄存器（r").append(instruction.B()).append("）到寄存器（r").append(instruction.C()).append("）闭区间的值连接拼接起来，将结果字符串赋值给寄存器（r").append(instruction.A()).append("）");
                    }
                    break;
                }
                case OP_JMP: {//<sBx>或者<A> <sBx>或者goto_XX或者<A> goto_XX
                    if (instruction.A() == 0) {
                        sb.append("goto_").append(i + 1 + instruction.sBx());
                        if (needNote) {
                            sb.append(" $ 无条件跳转");
                        }
                    } else {
                        sb.append(instruction.A()).append(" ").append("goto_").append(i + 1 + instruction.sBx());
                        if (needNote) {
                            sb.append(" $ 关闭寄存器（r").append(instruction.A() - 1).append("）以及更高层的值，然后跳转");
                        }
                    }
                    break;
                }
                case OP_EQ: {//<bool> <value> <value>或者<bool> rB <value>或者<bool> <value> rC或者<bool> rB rC
                    sb.append(instruction.A() != 0).append(" ");
                    if (instruction.A() != 0) {
                        if (ISK(instruction.B())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.B())));
                            if (ISK(instruction.C())) {
                                sb.append(" ").append(safeToVarString(f, INDEXK(instruction.C())));
                                if (needNote) {
                                    sb.append(" $ 如果常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）等于常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("），则跳转，否则，跳过下一条指令");
                                }
                            } else {
                                sb.append(" r").append(instruction.C());
                                if (needNote) {
                                    sb.append(" $ 如果常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）等于寄存器（r").append(instruction.C()).append("），则跳转，否则，跳过下一条指令");
                                }
                            }
                        } else {
                            sb.append("r").append(instruction.B()).append(" ");
                            if (ISK(instruction.C())) {
                                sb.append(safeToVarString(f, INDEXK(instruction.C())));
                                if (needNote) {
                                    sb.append(" $ 如果寄存器（r").append(instruction.B()).append("）的值等于常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("），则跳转，否则，跳过下一条指令");
                                }
                            } else {
                                sb.append("r").append(instruction.C());
                                if (needNote) {
                                    sb.append(" $ 如果寄存器（r").append(instruction.B()).append("）的值等于寄存器（r").append(instruction.C()).append("）的值，跳转，否则，跳过下一条指令");
                                }
                            }
                        }
                    } else {
                        if (ISK(instruction.B())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.B())));
                            if (ISK(instruction.C())) {
                                sb.append(" ").append(safeToVarString(f, INDEXK(instruction.C())));
                                if (needNote) {
                                    sb.append(" $ 如果常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）不等于常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("），则跳转，否则，跳过下一条指令");
                                }
                            } else {
                                sb.append(" r").append(instruction.C());
                                if (needNote) {
                                    sb.append(" $ 如果常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）不等于寄存器（r").append(instruction.C()).append("），则跳转，否则，跳过下一条指令");
                                }
                            }
                        } else {
                            sb.append("r").append(instruction.B()).append(" ");
                            if (ISK(instruction.C())) {
                                sb.append(safeToVarString(f, INDEXK(instruction.C())));
                                if (needNote) {
                                    sb.append(" $ 如果寄存器（r").append(instruction.B()).append("）的值不等于常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("），则跳转，否则，跳过下一条指令");
                                }
                            } else {
                                sb.append("r").append(instruction.C());
                                if (needNote) {
                                    sb.append(" $ 如果寄存器（r").append(instruction.B()).append("）的值不等于寄存器（r").append(instruction.C()).append("）的值，跳转，否则，跳过下一条指令");
                                }
                            }
                        }
                    }
                    break;
                }
                case OP_LT: {//<bool> <value> <value>或者<bool> rB <value>或者<bool> <value> rC或者<bool> rB rC
                    sb.append(instruction.A() != 0).append(" ");
                    if (instruction.A() != 0) {
                        if (ISK(instruction.B())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.B())));
                            if (ISK(instruction.C())) {
                                sb.append(" ").append(safeToVarString(f, INDEXK(instruction.C())));
                                if (needNote) {
                                    sb.append(" $ 如果常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）小于常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("），则跳转，否则，跳过下一条指令");
                                }
                            } else {
                                sb.append(" r").append(instruction.C());
                                if (needNote) {
                                    sb.append(" $ 如果常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）小于寄存器（r").append(instruction.C()).append("），则跳转，否则，跳过下一条指令");
                                }
                            }
                        } else {
                            sb.append("r").append(instruction.B()).append(" ");
                            if (ISK(instruction.C())) {
                                sb.append(safeToVarString(f, INDEXK(instruction.C())));
                                if (needNote) {
                                    sb.append(" $ 如果寄存器（r").append(instruction.B()).append("）的值小于常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("），则跳转，否则，跳过下一条指令");
                                }
                            } else {
                                sb.append("r").append(instruction.C());
                                if (needNote) {
                                    sb.append(" $ 如果寄存器（r").append(instruction.B()).append("）的值小于寄存器（r").append(instruction.C()).append("）的值，跳转，否则，跳过下一条指令");
                                }
                            }
                        }
                    } else {
                        if (ISK(instruction.B())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.B())));
                            if (ISK(instruction.C())) {
                                sb.append(" ").append(safeToVarString(f, INDEXK(instruction.C())));
                                if (needNote) {
                                    sb.append(" $ 如果常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）不小于常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("），则跳转，否则，跳过下一条指令");
                                }
                            } else {
                                sb.append(" r").append(instruction.C());
                                if (needNote) {
                                    sb.append(" $ 如果常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）不小于寄存器（r").append(instruction.C()).append("），则跳转，否则，跳过下一条指令");
                                }
                            }
                        } else {
                            sb.append("r").append(instruction.B()).append(" ");
                            if (ISK(instruction.C())) {
                                sb.append(safeToVarString(f, INDEXK(instruction.C())));
                                if (needNote) {
                                    sb.append(" $ 如果寄存器（r").append(instruction.B()).append("）的值不小于常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("），则跳转，否则，跳过下一条指令");
                                }
                            } else {
                                sb.append("r").append(instruction.C());
                                if (needNote) {
                                    sb.append(" $ 如果寄存器（r").append(instruction.B()).append("）的值不小于寄存器（r").append(instruction.C()).append("）的值，跳转，否则，跳过下一条指令");
                                }
                            }
                        }
                    }
                    break;
                }
                case OP_LE: {//<bool> <value> <value>或者<bool> rB <value>或者<bool> <value> rC或者<bool> rB rC
                    sb.append(instruction.A() != 0).append(" ");
                    if (instruction.A() != 0) {
                        if (ISK(instruction.B())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.B())));
                            if (ISK(instruction.C())) {
                                sb.append(" ").append(safeToVarString(f, INDEXK(instruction.C())));
                                if (needNote) {
                                    sb.append(" $ 如果常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）小于等于常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("），则跳转，否则，跳过下一条指令");
                                }
                            } else {
                                sb.append(" r").append(instruction.C());
                                if (needNote) {
                                    sb.append(" $ 如果常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）小于等于寄存器（r").append(instruction.C()).append("），则跳转，否则，跳过下一条指令");
                                }
                            }
                        } else {
                            sb.append("r").append(instruction.B()).append(" ");
                            if (ISK(instruction.C())) {
                                sb.append(safeToVarString(f, INDEXK(instruction.C())));
                                if (needNote) {
                                    sb.append(" $ 如果寄存器（r").append(instruction.B()).append("）的值小于等于常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("），则跳转，否则，跳过下一条指令");
                                }
                            } else {
                                sb.append("r").append(instruction.C());
                                if (needNote) {
                                    sb.append(" $ 如果寄存器（r").append(instruction.B()).append("）的值小于等于寄存器（r").append(instruction.C()).append("）的值，跳转，否则，跳过下一条指令");
                                }
                            }
                        }
                    } else {
                        if (ISK(instruction.B())) {
                            sb.append(safeToVarString(f, INDEXK(instruction.B())));
                            if (ISK(instruction.C())) {
                                sb.append(" ").append(safeToVarString(f, INDEXK(instruction.C())));
                                if (needNote) {
                                    sb.append(" $ 如果常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）不小于等于常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("），则跳转，否则，跳过下一条指令");
                                }
                            } else {
                                sb.append(" r").append(instruction.C());
                                if (needNote) {
                                    sb.append(" $ 如果常量（").append(safeToVarString(f, INDEXK(instruction.B()))).append("）不小于等于寄存器（r").append(instruction.C()).append("），则跳转，否则，跳过下一条指令");
                                }
                            }
                        } else {
                            sb.append("r").append(instruction.B()).append(" ");
                            if (ISK(instruction.C())) {
                                sb.append(safeToVarString(f, INDEXK(instruction.C())));
                                if (needNote) {
                                    sb.append(" $ 如果寄存器（r").append(instruction.B()).append("）的值不小于等于常量（").append(safeToVarString(f, INDEXK(instruction.C()))).append("），则跳转，否则，跳过下一条指令");
                                }
                            } else {
                                sb.append("r").append(instruction.C());
                                if (needNote) {
                                    sb.append(" $ 如果寄存器（r").append(instruction.B()).append("）的值不小于等于寄存器（r").append(instruction.C()).append("）的值，跳转，否则，跳过下一条指令");
                                }
                            }
                        }
                    }
                    break;
                }
                case OP_TEST: {//rA <bool>
                    sb.append("r").append(instruction.A()).append(" ").append(instruction.C() != 0);
                    if (needNote) {
                        sb.append(" $ 如果寄存器（r").append(instruction.A()).append("）的转布尔值后的值为").append(instruction.C() != 0).append("，则跳转，否则，跳过下一条指令");
                    }
                    break;
                }
                case OP_TESTSET: {//rA rB <bool>
                    sb.append("r").append(instruction.A()).append(" r").append(instruction.B()).append(" ").append(instruction.C() != 0);
                    if (needNote) {
                        sb.append(" $ 如果寄存器（r").append(instruction.B()).append("）的转布尔值后的值为").append(instruction.C() != 0).append("，则把寄存器（r").append(instruction.B()).append("）的值赋值给寄存器（r").append(instruction.A()).append("），并跳转，否则，跳过下一条指令");
                    }
                    break;
                }
                case OP_CALL: {//rA <B> <C>
                    sb.append("r").append(instruction.A()).append(" ").append(instruction.B()).append(" ").append(instruction.C());
                    if (needNote) {
                        sb.append(" $ 调用函数，参数个数为").append(instruction.B());
                        if (instruction.C() == 0) {
                            sb.append("，返回值数量不确定");
                        } else if (instruction.C() == 1) {
                            sb.append("，无返回值");
                        } else if (instruction.C() > 1) {
                            sb.append("，返回值数量为").append(instruction.C() - 1);
                        }
                    }
                    break;
                }
                case OP_TAILCALL: {//rA <B>
                    sb.append("r").append(instruction.A()).append(" ").append(instruction.B()).append(" ").append(instruction.C());
                    if (needNote) {
                        sb.append(" $ 尾调用函数，参数个数为").append(instruction.B()).append("，返回值数量不确定，通常这个指令后面会跟一个返回指令");
                    }
                    break;
                }
                case OP_RETURN: {//rA <B>
                    sb.append("r").append(instruction.A()).append(" ").append(instruction.B());
                    if (needNote) {
                        sb.append(" $ 返回");
                        if (instruction.B() == 0) {
                            sb.append("，返回值数量已经部分在栈顶，把剩余的返回值数量压入栈顶");
                        } else if (instruction.B() == 1) {
                            sb.append("，无返回值");
                        } else if (instruction.B() > 1) {
                            sb.append("，返回值数量为").append(instruction.B() - 1);
                        }
                    }
                    break;
                }
                case OP_FORLOOP: {//rA <sBx>或者rA goto_XX
                    sb.append("r").append(instruction.A()).append(" ").append("goto_").append(i + 1 + instruction.sBx());
                    if (needNote) {
                        sb.append(" $ 循环指令，增长相应步长后跳转");
                    }
                    break;
                }
                case OP_FORPREP: {//rA <sBx>或者rA goto_XX
                    sb.append("r").append(instruction.A()).append(" ").append("goto_").append(i + 1 + instruction.sBx());
                    if (needNote) {
                        sb.append(" $ 循环指令，准备循环，把循环变量减去步长，然后跳转");
                    }
                    break;
                }
                case OP_TFORCALL: {//rA <C>
                    sb.append("r").append(instruction.A()).append(" ").append(instruction.C());
                    if (needNote) {
                        sb.append(" $ 调用迭代函数，参数个数为").append(instruction.C());
                    }
                    break;
                }
                case OP_TFORLOOP: {//rA <sBx>或者rA goto_XX
                    sb.append("r").append(instruction.A()).append(" ").append("goto_").append(i + 1 + instruction.sBx());
                    if (needNote) {
                        sb.append(" $ 循环指令，如果寄存器（r").append(instruction.A() + 1).append("）非nil，则跳转");
                    }
                    break;
                }
                case OP_SETLIST: {//rA <B> <C or Ax>
                    sb.append("r").append(instruction.A()).append(" ").append(instruction.B());
                    if (instruction.C() == 0) {
                        i++;
                        Instruction instruction2 = f.code[i];
                        sb.append(" ").append(instruction2.Ax());
                        if (needNote) {
                            sb.append(" $ 设置表，批次数量为").append(instruction2.Ax());
                        }
                    } else {
                        sb.append(" ").append(instruction.C());
                        if (needNote) {
                            sb.append(" $ 设置表，批次数量为").append(instruction.C());
                        }
                    }
                    break;
                }
                case OP_CLOSURE: {//rA pBx
                    sb.append("r").append(instruction.A()).append(" ").append("p").append(instruction.Bx());
                    if (needNote) {
                        sb.append(" $ 创建闭包，子原型索引为").append(instruction.Bx());
                    }
                    break;
                }
                case OP_VARARG: {//rA <B>
                    sb.append("r").append(instruction.A()).append(" ").append(instruction.B());
                    if (needNote) {
                        sb.append(" $ 变长参数");
                    }
                    break;
                }
                case OP_EXTRAARG: {//Ax
                    sb.append(instruction.Ax());
                    if (needNote) {
                        sb.append(" $ 附加参数，他应该不会翻译出来，用到它的指令应该已经自动解析了才对。");
                    }
                    break;
                }
                case OP_TBC: {//rA
                    sb.append("r").append(instruction.A());
                    if (needNote) {
                        sb.append(" $ ToBeClosed，Alua用于简单支持lua54特性的指令");
                    }
                    break;
                }
                case OP_NEWARRAY: {//rA <B>
                    sb.append("r").append(instruction.A()).append(" ").append(instruction.B());
                    if (needNote) {
                        sb.append(" $ 创建数组，数组长度为").append(instruction.B());
                    }
                    break;
                }
                case OP_TFOREACH: {//rA
                    sb.append("r").append(instruction.A());
                    if (needNote) {
                        sb.append(" $ 迭代器循环指令，目前Alua仅解释器支持运行，暂不支持编译出这个指令，你看到它多半有问题");
                    }
                    break;
                }
                default: {
                    sb.append(instruction.value());
                    break;
                }
            }
            sb.append("\n");
        }
        sb.append(".code-end");
        sb.append("\n");
        sb.append(".end");
        sb.append("\n");
        sb.append("\n");
        int sizep = f.sizep();
        for (i = 0; i < sizep; i++) {
            if (subPath == null) {
                sb.append(getXasm(f.p[i], String.valueOf(i), needNote, likeActCode, needSubXasm));
            } else {
                sb.append(getXasm(f.p[i], subPath + "/" + String.valueOf(i), needNote, likeActCode, needSubXasm));
            }
        }
        return sb.toString();
    }
}
