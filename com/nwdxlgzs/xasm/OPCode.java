package com.nwdxlgzs.xasm;

import static com.nwdxlgzs.xasm.OPCode.OpArgMask.*;
import static com.nwdxlgzs.xasm.OPCode.OpMode.*;

public enum OPCode {
    UNKNOWN(-1, 0, 0, OpArgMask.UNKNOWN, OpArgMask.UNKNOWN, OpMode.UNKNOWN, "unknown"),
    OP_MOVE(0, 0, 1, OpArgR, OpArgN, iABC, "move"),
    OP_LOADK(1, 0, 1, OpArgK, OpArgN, iABx, "loadk"),
    OP_LOADKX(2, 0, 1, OpArgN, OpArgN, iABx, "loadkx"),
    OP_LOADBOOL(3, 0, 1, OpArgU, OpArgU, iABC, "loadbool"),
    OP_LOADNIL(4, 0, 1, OpArgU, OpArgN, iABC, "loadnil"),
    OP_GETUPVAL(5, 0, 1, OpArgU, OpArgN, iABC, "getupval"),
    OP_GETTABUP(6, 0, 1, OpArgU, OpArgK, iABC, "gettabup"),
    OP_GETTABLE(7, 0, 1, OpArgR, OpArgK, iABC, "gettable"),
    OP_SETTABUP(8, 0, 0, OpArgK, OpArgK, iABC, "settabup"),
    OP_SETUPVAL(9, 0, 0, OpArgU, OpArgN, iABC, "setupval"),
    OP_SETTABLE(10, 0, 0, OpArgK, OpArgK, iABC, "settable"),
    OP_NEWTABLE(11, 0, 1, OpArgU, OpArgU, iABC, "newtable"),
    OP_SELF(12, 0, 1, OpArgR, OpArgK, iABC, "self"),
    OP_ADD(13, 0, 1, OpArgK, OpArgK, iABC, "add"),
    OP_SUB(14, 0, 1, OpArgK, OpArgK, iABC, "sub"),
    OP_MUL(15, 0, 1, OpArgK, OpArgK, iABC, "mul"),
    OP_MOD(16, 0, 1, OpArgK, OpArgK, iABC, "mod"),
    OP_POW(17, 0, 1, OpArgK, OpArgK, iABC, "pow"),
    OP_DIV(18, 0, 1, OpArgK, OpArgK, iABC, "div"),
    OP_IDIV(19, 0, 1, OpArgK, OpArgK, iABC, "idiv"),
    OP_BAND(20, 0, 1, OpArgK, OpArgK, iABC, "band"),
    OP_BOR(21, 0, 1, OpArgK, OpArgK, iABC, "bor"),
    OP_BXOR(22, 0, 1, OpArgK, OpArgK, iABC, "bxor"),
    OP_SHL(23, 0, 1, OpArgK, OpArgK, iABC, "shl"),
    OP_SHR(24, 0, 1, OpArgK, OpArgK, iABC, "shr"),
    OP_UNM(25, 0, 1, OpArgR, OpArgN, iABC, "unm"),
    OP_BNOT(26, 0, 1, OpArgR, OpArgN, iABC, "bnot"),
    OP_NOT(27, 0, 1, OpArgR, OpArgN, iABC, "not"),
    OP_LEN(28, 0, 1, OpArgR, OpArgN, iABC, "len"),
    OP_CONCAT(29, 0, 1, OpArgR, OpArgR, iABC, "concat"),
    OP_JMP(30, 0, 0, OpArgR, OpArgN, iAsBx, "jmp"),
    OP_EQ(31, 1, 0, OpArgK, OpArgK, iABC, "eq"),
    OP_LT(32, 1, 0, OpArgK, OpArgK, iABC, "lt"),
    OP_LE(33, 1, 0, OpArgK, OpArgK, iABC, "le"),
    OP_TEST(34, 1, 0, OpArgR, OpArgU, iABC, "test"),
    OP_TESTSET(35, 1, 1, OpArgR, OpArgU, iABC, "testset"),
    OP_CALL(36, 0, 1, OpArgU, OpArgU, iABC, "call"),
    OP_TAILCALL(37, 0, 0, OpArgU, OpArgU, iABC, "tailcall"),
    OP_RETURN(38, 0, 0, OpArgU, OpArgN, iABC, "return"),
    OP_FORLOOP(39, 1, 0, OpArgR, OpArgN, iAsBx, "forloop"),
    OP_FORPREP(40, 1, 0, OpArgR, OpArgN, iAsBx, "forprep"),
    OP_TFORCALL(41, 0, 0, OpArgU, OpArgU, iABC, "tforcall"),
    OP_TFORLOOP(42, 1, 0, OpArgR, OpArgN, iAsBx, "tforloop"),
    OP_SETLIST(43, 0, 0, OpArgU, OpArgU, iABC, "setlist"),
    OP_CLOSURE(44, 0, 1, OpArgU, OpArgN, iABx, "closure"),
    OP_VARARG(45, 0, 1, OpArgU, OpArgN, iABC, "vararg"),
    OP_EXTRAARG(46, 0, 0, OpArgU, OpArgU, iAx, "extraarg"),
    //下头三个是nirenr的Androlua+自定义指令，其中TFOREACH只是保留指令，还未实现
    OP_TBC(47, 0, 0, OpArgN, OpArgN, iABC, "tbc"),
    OP_NEWARRAY(48, 0, 1, OpArgU, OpArgN, iABC, "newarray"),
    OP_TFOREACH(49, 0, 0, OpArgN, OpArgU, iABC, "tforeach"),
    //下头是不存在的op
    OP_OP50(50, 0, 0, OpArgMask.UNKNOWN, OpArgMask.UNKNOWN, OpMode.UNKNOWN, "op50"),
    OP_OP51(51, 0, 0, OpArgMask.UNKNOWN, OpArgMask.UNKNOWN, OpMode.UNKNOWN, "op51"),
    OP_OP52(52, 0, 0, OpArgMask.UNKNOWN, OpArgMask.UNKNOWN, OpMode.UNKNOWN, "op52"),
    OP_OP53(53, 0, 0, OpArgMask.UNKNOWN, OpArgMask.UNKNOWN, OpMode.UNKNOWN, "op53"),
    OP_OP54(54, 0, 0, OpArgMask.UNKNOWN, OpArgMask.UNKNOWN, OpMode.UNKNOWN, "op54"),
    OP_OP55(55, 0, 0, OpArgMask.UNKNOWN, OpArgMask.UNKNOWN, OpMode.UNKNOWN, "op55"),
    OP_OP56(56, 0, 0, OpArgMask.UNKNOWN, OpArgMask.UNKNOWN, OpMode.UNKNOWN, "op56"),
    OP_OP57(57, 0, 0, OpArgMask.UNKNOWN, OpArgMask.UNKNOWN, OpMode.UNKNOWN, "op57"),
    OP_OP58(58, 0, 0, OpArgMask.UNKNOWN, OpArgMask.UNKNOWN, OpMode.UNKNOWN, "op58"),
    OP_OP59(59, 0, 0, OpArgMask.UNKNOWN, OpArgMask.UNKNOWN, OpMode.UNKNOWN, "op59"),
    OP_OP60(60, 0, 0, OpArgMask.UNKNOWN, OpArgMask.UNKNOWN, OpMode.UNKNOWN, "op60"),
    OP_OP61(61, 0, 0, OpArgMask.UNKNOWN, OpArgMask.UNKNOWN, OpMode.UNKNOWN, "op61"),
    OP_OP62(62, 0, 0, OpArgMask.UNKNOWN, OpArgMask.UNKNOWN, OpMode.UNKNOWN, "op62"),
    OP_OP63(63, 0, 0, OpArgMask.UNKNOWN, OpArgMask.UNKNOWN, OpMode.UNKNOWN, "op63");

    private int op;
    private int T;
    private int A;
    private OpArgMask B;
    private OpArgMask C;
    private OpMode Mode;
    private String name;

    OPCode(int op, int T, int A, OpArgMask B, OpArgMask C, OpMode Mode, String name) {
        this.op = op;
        this.T = T;
        this.A = A;
        this.B = B;
        this.C = C;
        this.Mode = Mode;
        this.name = name;
    }

    public int getOP() {
        return op;
    }

    public int getT() {
        return T;
    }

    public int getA() {
        return A;
    }

    public OpArgMask getB() {
        return B;
    }

    public OpArgMask getC() {
        return C;
    }

    public OpMode getMode() {
        return Mode;
    }

    public String getName() {
        return name;
    }

    public static OPCode fromValue(int value) {
        for (OPCode opCode : OPCode.values()) {
            if (opCode.getOP() == value) {
                return opCode;
            }
        }
        return UNKNOWN;
    }

    public static OPCode fromName(String name) {
        name = name.toLowerCase();
        for (OPCode opCode : OPCode.values()) {
            if (opCode.getName().equals(name)) {
                return opCode;
            }
        }
        if ("neq".equals(name) || "~=".equals(name) || "!=".equals(name) || "==".equals(name)) {
            return OP_EQ;
        }
        if ("nlt".equals(name) || ">".equals(name) || "<".equals(name)) {
            return OP_LT;
        }
        if ("nle".equals(name) || ">=".equals(name) || "<=".equals(name)) {
            return OP_LE;
        }
        if ("func".equals(name) || "def".equals(name)) {
            return OP_CLOSURE;
        }
        if ("+".equals(name)) {
            return OP_ADD;
        }
        if ("-".equals(name)) {
            return OP_SUB;
        }
        if ("*".equals(name)) {
            return OP_MUL;
        }
        if ("%".equals(name)) {
            return OP_MOD;
        }
        if ("^".equals(name)) {
            return OP_POW;
        }
        if ("/".equals(name)) {
            return OP_DIV;
        }
        if ("//".equals(name)) {
            return OP_IDIV;
        }
        if ("&".equals(name)) {
            return OP_BAND;
        }
        if ("|".equals(name)) {
            return OP_BOR;
        }
        if ("~".equals(name)) {
            return OP_BXOR;
        }
        if ("<<".equals(name)) {
            return OP_SHL;
        }
        if (">>".equals(name)) {
            return OP_SHR;
        }
        if ("-()".equals(name)) {
            return OP_UNM;
        }
        if ("~()".equals(name)) {
            return OP_BNOT;
        }
        if ("!()".equals(name)) {
            return OP_NOT;
        }
        if ("#".equals(name)) {
            return OP_LEN;
        }
        if ("..".equals(name)) {
            return OP_CONCAT;
        }
        if ("{}".equals(name)) {
            return OP_NEWTABLE;
        }
        if ("[]".equals(name)) {
            return OP_NEWARRAY;
        }


        //特殊支持一下：opXX
        if (name.startsWith("op")) {
            try {
                int op = Integer.parseInt(name.substring(2));
                return fromValue(op);
            } catch (Exception e) {
                //ignore
            }
        }
        return UNKNOWN;
    }

    public enum OpArgMask {
        OpArgN(0),
        OpArgU(1),
        OpArgR(2),
        OpArgK(3),
        UNKNOWN(-1);
        public int value;

        OpArgMask(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static OpArgMask fromValue(int value) {
            for (OpArgMask opArgMask : OpArgMask.values()) {
                if (opArgMask.getValue() == value) {
                    return opArgMask;
                }
            }
            return UNKNOWN;
        }
    }

    public enum OpMode {
        iABC(0),
        iABx(1),
        iAsBx(2),
        iAx(3),
        UNKNOWN(-1);
        public int value;

        OpMode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static OpMode fromValue(int value) {
            for (OpMode opMode : OpMode.values()) {
                if (opMode.getValue() == value) {
                    return opMode;
                }
            }
            return UNKNOWN;
        }
    }
}
