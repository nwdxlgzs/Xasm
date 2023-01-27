package com.nwdxlgzs.xasm;

import static com.nwdxlgzs.xasm.OPCode.*;
import static com.nwdxlgzs.xasm.defines.*;

public class bindInst {
    public static void bind(Proto f) {
        int i;
        Instruction[] code = f.code;
        int sizek = f.sizek();
        int sizeupvalues = f.sizeupvalues();
        int sizep = f.sizep();
        int sizecode = f.sizecode();
        for (i = 0; i < sizecode; i++) {
            Instruction instruction = code[i];
            OPCode op = instruction.getOpCode();
            switch (op) {
                case OP_CLOSURE: {//溢出检查
                    if (instruction.Bx() >= sizep) {
                        instruction.isRealFake = true;
                    }
                    break;
                }
                case OP_LOADK: {//溢出检查
                    if (instruction.B() >= sizek) {
                        instruction.isRealFake = true;
                    }
                    break;
                }
                case OP_SETTABUP: {//溢出检查
                    if (instruction.A() >= sizeupvalues) {
                        instruction.isRealFake = true;
                    }
                    //这里不用break，因为还要溢出检查
                }
                case OP_SETTABLE:
                case OP_ADD:
                case OP_SUB:
                case OP_MUL:
                case OP_MOD:
                case OP_POW:
                case OP_DIV:
                case OP_IDIV:
                case OP_BAND:
                case OP_BOR:
                case OP_BXOR:
                case OP_SHL:
                case OP_SHR: {//溢出检查
                    if (ISK(instruction.B()) && INDEXK(instruction.B()) >= sizek) {
                        instruction.isRealFake = true;
                    }
                    //这里不用break，因为还要检查C
                }
                case OP_GETTABLE:
                case OP_SELF: {//溢出检查
                    if (ISK(instruction.C()) && INDEXK(instruction.C()) >= sizek) {
                        instruction.isRealFake = true;
                    }
                    break;
                }
                case OP_GETTABUP: {
                    if (ISK(instruction.C()) && INDEXK(instruction.C()) >= sizek) {//溢出检查
                        instruction.isRealFake = true;
                    }
                    if (instruction.B() >= sizeupvalues) {//溢出检查
                        instruction.isRealFake = true;
                    }
                    break;
                }
                case OP_SETUPVAL: {
                    if (instruction.C() != 0) {//填补没用的操作数C
                        instruction.C(0);
                    }
                    if (instruction.B() >= sizeupvalues) {//溢出检查
                        instruction.isRealFake = true;
                    }
                    break;
                }
                case OP_GETUPVAL: {//溢出检查
                    if (instruction.B() >= sizeupvalues) {
                        instruction.isRealFake = true;
                    }
                    //这里不用break，因为还要检查C
                }
                case OP_MOVE:
                case OP_LOADNIL:
                case OP_NEWARRAY:
                case OP_UNM:
                case OP_BNOT:
                case OP_NOT:
                case OP_LEN:
                case OP_TAILCALL:
                case OP_RETURN:
                case OP_VARARG: {//填补没用的操作数C
                    if (instruction.C() != 0) {
                        instruction.C(0);
                    }
                    break;
                }
                case OP_TFOREACH:
                case OP_TBC: {//填补没用的操作数B&C
                    if (instruction.B() != 0) {
                        instruction.B(0);
                    }
                    if (instruction.C() != 0) {
                        instruction.C(0);
                    }
                    break;
                }
                case OP_LOADKX: {//OP_LOADKX特例
                    int target = i + 1;
                    if (target < sizecode) {
                        code[target].isCanJump2There = true;
                        Instruction instruction2 = code[target];
                        if (instruction2.Ax() >= sizek) {
                            instruction2.isRealFake = true;
                        }
                    } else {
                        instruction.isRealFake = true;
                    }
                    if (instruction.Bx() != 0) {//填补没用的操作数Bx
                        instruction.Bx(0);
                    }
                    break;
                }
                case OP_LOADBOOL: {//OP_LOADBOOL特例
                    if (instruction.C() != 0) {
                        int target = i + 2;
                        if (target < sizecode) {
                            code[target].isCanJump2There = true;
                        } else {
                            instruction.isRealFake = true;
                        }
                    }
                    if (instruction.B() > 1) {//B就只有0和非0（1）两种情况
                        instruction.B(1);
                    }
                    break;
                }
                case OP_JMP:
                case OP_FORLOOP:
                case OP_FORPREP:
                case OP_TFORLOOP: {//sBx
                    int sBx = instruction.sBx();
                    int target = i + 1 + sBx;
                    if (target >= 0 && target < sizecode) {
                        code[target].isCanJump2There = true;
                    } else {
                        instruction.isRealFake = true;
                    }
                    break;
                }
                case OP_TFORCALL: {//填补没用的操作数B以及当前指令+下一句sBx
                    if (instruction.B() != 0) {
                        instruction.B(0);
                    }
                    int target = i + 1;
                    if (target < sizecode) {
                        code[target].isCanJump2There = true;
                        Instruction instruction2 = code[target];
                        int sBx = instruction2.sBx();
                        target = i + 2 + sBx;
                        if (target >= 0 && target < sizecode) {
                            code[target].isCanJump2There = true;
                        } else {
                            instruction.isRealFake = true;
                        }
                    } else {
                        instruction.isRealFake = true;
                    }
                    break;
                }
                case OP_EQ:
                case OP_LT:
                case OP_LE: {
                    if (instruction.A() > 1) {//A就只有0和非0（1）两种情况
                        instruction.A(1);
                    }
                    if (ISK(instruction.B()) && INDEXK(instruction.B()) >= sizek) {//溢出检查
                        instruction.isRealFake = true;
                    }
                    if (ISK(instruction.C()) && INDEXK(instruction.C()) >= sizek) {//溢出检查
                        instruction.isRealFake = true;
                    }
                    //没有break，因为还有后续处理
                }
                case OP_TEST:
                case OP_TESTSET: {//当前指令+下一句sBx
                    int target = i + 1;
                    if (target < sizecode) {
                        code[target].isCanJump2There = true;
                        Instruction instruction2 = code[target];
                        int sBx = instruction2.sBx();
                        target = i + 2 + sBx;
                        if (target >= 0 && target < sizecode) {
                            code[target].isCanJump2There = true;
                        } else {
                            instruction.isRealFake = true;
                        }
                    } else {
                        instruction.isRealFake = true;
                    }
                    break;
                }
                case OP_SETLIST: {//OP_SETLIST特例
                    if (instruction.C() == 0) {
                        int target = i + 1;
                        if (target < sizecode) {
                            code[target].isCanJump2There = true;
                        } else {
                            instruction.isRealFake = true;
                        }
                    }
                    break;
                }
                default: {
                    if (op.getOP() >= OP_OP50.getOP()) {
                        instruction.isRealFake = true;
                    }
                    break;
                }
            }
        }
        for (i = 0; i < sizecode; i++) {
            Instruction instruction = code[i];
            if (instruction.isRealFake) {
                //上一个isCanJump2There(i:inst goto _inst)到下一个isCanJump2There(i-1:_inst goto inst)之间的指令都是假的
                int j;
                for (j = i; j > 0; j--) {
                    if (j == 1) {//第一个指令一定是JMP了
                        code[j].isStartRealFake = true;
                        break;
                    }
                    if (code[j].isCanJump2There || code[j].getOpCode() == OP_RETURN) {
                        boolean dobreak = false;
                        Instruction instruction2 = code[j];
                        switch (instruction2.getOpCode()) {
                            case OP_LOADKX:
                            case OP_EQ:
                            case OP_LT:
                            case OP_LE:
                            case OP_TEST:
                            case OP_TESTSET: {//忽略这个情况
                                break;
                            }
                            default: {
                                code[j].isStartRealFake = true;
                                dobreak = true;
                                break;
                            }
                        }
                        if (dobreak) {
                            break;
                        }
                    }
                }
                for (j = i + 1; j < sizecode; j++) {
                    if (j == sizecode - 1) {
                        code[j].isEndRealFake = true;
                        break;
                    }
                    if (code[j].isCanJump2There || code[j].getOpCode() == OP_RETURN) {
                        boolean dobreak = false;
                        Instruction instruction2 = code[j - 1];
                        switch (instruction2.getOpCode()) {
                            case OP_LOADKX:
                            case OP_EQ:
                            case OP_LT:
                            case OP_LE:
                            case OP_TEST:
                            case OP_TESTSET: {//忽略这个情况
                                break;
                            }
                            default: {
                                code[j - 1].isEndRealFake = true;
                                dobreak = true;
                                break;
                            }
                        }
                        if (dobreak) {
                            break;
                        }
                    }
                }
            }
        }
        for (i = 0; i < sizecode; i++) {//合并Start和End
            Instruction instruction = code[i];
            //End和Start前后脚或者同一个的就合并
            if (instruction.isEndRealFake && instruction.isStartRealFake) {
                instruction.isEndRealFake = false;
                instruction.isStartRealFake = false;
            }
            if (instruction.isEndRealFake) {
                if (i > 0) {
                    Instruction instruction2 = code[i - 1];
                    if (instruction2.isStartRealFake) {
                        instruction.isEndRealFake = false;
                        instruction2.isStartRealFake = false;
                    }
                }
            }
        }
        for (i = 0; i < sizecode; i++) {//剔除重复的Start
            Instruction instruction = code[i];
            if (instruction.isStartRealFake) {
                int j;
                for (j = i + 1; j < sizecode; j++) {
                    Instruction instruction2 = code[j];
                    if (instruction2.isEndRealFake) {
                        break;
                    }
                    if (instruction2.isStartRealFake) {
                        instruction2.isStartRealFake = false;
                    }
                }
            }
        }
    }

}
