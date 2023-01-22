package com.nwdxlgzs.xasm;

import static com.nwdxlgzs.xasm.OPCode.*;
import static com.nwdxlgzs.xasm.defines.*;
import static com.nwdxlgzs.xasm.TValue.*;

public class bindInst {
    public static void bind(Proto f) {
        int i;
        Instruction[] code = f.code;
        int sizecode = f.sizecode();
        for (i = 0; i < sizecode; i++) {
            Instruction instruction = code[i];
            OPCode op = instruction.getOpCode();
            switch (op) {
                case OP_LOADKX: {//OP_LOADKX特例
                    int target = i + 1;
                    if (target < sizecode) {
                        code[target].isCanJump2There = true;
                    }
                    break;
                }
                case OP_LOADBOOL: {//OP_LOADBOOL特例
                    if (instruction.C() != 0) {
                        int target = i + 1;
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
                case OP_EQ:
                case OP_LT:
                case OP_LE: {
                    if (instruction.A() > 1) {//A就只有0和非0（1）两种情况
                        instruction.A(1);
                    }
                    //没有break，因为还有后续处理
                }
                case OP_TEST:
                case OP_TESTSET:
                case OP_TFORCALL: {//当前指令+下一句sBx
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
