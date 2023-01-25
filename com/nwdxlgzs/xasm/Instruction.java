package com.nwdxlgzs.xasm;

import static com.nwdxlgzs.xasm.defines.*;

import java.io.Serializable;

public class Instruction implements Serializable {
    private int value = 0;
    //下方几个辅助汇编用的，不是Instruction该有的信息，toString我就不处理了
    public boolean isCanJump2There = false;
    public boolean isRealFake = false;
    public boolean isStartRealFake = false;
    public boolean isEndRealFake = false;

    public Instruction(int instruction) {
        value = instruction;
    }

    public OPCode getOpCode() {
        return OPCode.fromValue(opcode());
    }

    public OPCode.OpMode getOpMode() {
        return getOpCode().getMode();
    }

    public OPCode.OpArgMask getOpArgB() {
        return getOpCode().getB();
    }

    public OPCode.OpArgMask getOpArgC() {
        return getOpCode().getC();
    }

    /*
     * 下方是设置和获取的方法
     */
    public int opcode() {
        return GET_OPCODE(value);
    }

    public void opcode(int opcode) {
        value = SET_OPCODE(value, opcode);
    }

    public int A() {
        return GETARG_A(value);
    }

    public void A(int a) {
        value = SETARG_A(value, a);
    }

    public int B() {
        return GETARG_B(value);
    }

    public void B(int b) {
        value = SETARG_B(value, b);
    }

    public int C() {
        return GETARG_C(value);
    }

    public void C(int c) {
        value = SETARG_C(value, c);
    }

    public int Bx() {
        return GETARG_Bx(value);
    }

    public void Bx(int bx) {
        value = SETARG_Bx(value, bx);
    }

    public int sBx() {
        return GETARG_sBx(value);
    }

    public void sBx(int sbx) {
        value = SETARG_sBx(value, sbx);
    }

    public int Ax() {
        return GETARG_Ax(value);
    }

    public void Ax(int ax) {
        value = SETARG_Ax(value, ax);
    }

    public int value() {
        return value;
    }

    public void value(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        int A = 0;
        int B = 0;
        int C = 0;
        int Bx = 0;
        int sBx = 0;
        int Ax = 0;
        int opcode = GET_OPCODE(value);
        OPCode opName = OPCode.fromValue(opcode);
        switch (opName.getMode()) {
            case iABC: {
                A = GETARG_A(value);
                B = GETARG_B(value);
                C = GETARG_C(value);
                break;
            }
            case iABx: {
                A = GETARG_A(value);
                Bx = GETARG_Bx(value);
                break;
            }
            case iAsBx: {
                A = GETARG_A(value);
                sBx = GETARG_sBx(value);
                break;
            }
            case iAx: {
                Ax = GETARG_Ax(value);
                break;
            }
            default: {
                A = GETARG_A(value);
                B = GETARG_B(value);
                C = GETARG_C(value);
                Bx = GETARG_Bx(value);
                sBx = GETARG_sBx(value);
                Ax = GETARG_Ax(value);
                break;
            }
        }
        switch (opName.getMode()) {
            case iABC: {
                return "Instruction{" + "\n" +
                        "value=" + value + "\n" +
                        ", opcode=" + opcode + "\n" +
                        ", opName=" + opName + "\n" +
                        ", A=" + A + "\n" +
                        ", B=" + B + "\n" +
                        ", C=" + C + "\n" +
                        '}';
            }
            case iABx: {
                return "Instruction{" + "\n" +
                        "value=" + value + "\n" +
                        ", opcode=" + opcode + "\n" +
                        ", opName=" + opName + "\n" +
                        ", A=" + A + "\n" +
                        ", Bx=" + Bx + "\n" +
                        '}';
            }
            case iAsBx: {
                return "Instruction{" + "\n" +
                        "value=" + value + "\n" +
                        ", opcode=" + opcode + "\n" +
                        ", opName=" + opName + "\n" +
                        ", A=" + A + "\n" +
                        ", sBx=" + sBx + "\n" +
                        '}';
            }
            case iAx: {
                return "Instruction{" + "\n" +
                        "value=" + value + "\n" +
                        ", opcode=" + opcode + "\n" +
                        ", opName=" + opName + "\n" +
                        ", Ax=" + Ax + "\n" +
                        '}';
            }
            default: {
                return "Instruction{" + "\n" +
                        "value=" + value + "\n" +
                        ", opcode=" + opcode + "\n" +
                        ", opName=" + opName + "\n" +
                        ", A=" + A + "\n" +
                        ", B=" + B + "\n" +
                        ", C=" + C + "\n" +
                        ", Bx=" + Bx + "\n" +
                        ", sBx=" + sBx + "\n" +
                        ", Ax=" + Ax + "\n" +
                        '}';
            }
        }
    }
}
