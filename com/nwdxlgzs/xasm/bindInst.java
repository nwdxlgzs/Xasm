package com.nwdxlgzs.xasm;

public class bindInst {
    public static void bind(Proto f) {
        int i;
        Instruction[] code = f.code;
        int sizecode = f.sizecode();
        for (i = 0; i < sizecode; i++) {
            Instruction instruction = code[i];
            OPCode op = instruction.getOpCode();
            switch (op) {
                case OP_MOVE:
                case OP_LOADNIL:
                case OP_GETUPVAL:
                case OP_SETUPVAL:
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
                    }
                    break;
                }
                case OP_TFORCALL: {//填补没用的操作数B以及当前指令+下一句sBx（case冲突，java怎么没goto啊）
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
                        }
                    }
                    break;
                }
                case OP_EQ:
                case OP_LT:
                case OP_LE: {
                    if (instruction.A() > 1) {//A就只有0和非0（1）两种情况
                        instruction.A(1);
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
                        }
                    }
                    break;
                }
                case OP_SETLIST: {//OP_SETLIST特例
                    if (instruction.C() == 0) {
                        int target = i + 1;
                        if (target < sizecode) {
                            code[target].isCanJump2There = true;
                        }
                    }
                    break;
                }
                default: {
                    break;
                }
            }
        }
    }
}
