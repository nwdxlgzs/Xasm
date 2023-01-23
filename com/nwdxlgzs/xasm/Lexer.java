package com.nwdxlgzs.xasm;

import static com.nwdxlgzs.xasm.OPCode.OpArgMask.OpArgK;
import static com.nwdxlgzs.xasm.OPCode.OpArgMask.OpArgN;
import static com.nwdxlgzs.xasm.OPCode.OpArgMask.OpArgR;
import static com.nwdxlgzs.xasm.OPCode.OpArgMask.OpArgU;
import static com.nwdxlgzs.xasm.OPCode.OpMode.iABC;
import static com.nwdxlgzs.xasm.OPCode.OpMode.iABx;
import static com.nwdxlgzs.xasm.OPCode.OpMode.iAsBx;
import static com.nwdxlgzs.xasm.OPCode.OpMode.iAx;

public class Lexer {

    public enum Tokens {
        WHITESPACE,
        NEWLINE,
        UNKNOWN,
        EOF,

        LINE_COMMENT,

        DIV,
        MULT,
        IDENTIFIER,
        INTEGER_LITERAL,
        DOT,
        MINUS,
        STRING,
        CHARACTER_LITERAL,
        LPAREN,
        RPAREN,
        LBRACE,
        RBRACE,
        LBRACK,
        RBRACK,
        COMMA,
        EQ,
        GT,
        LT,
        NOT,
        COMP,
        QUESTION,
        COLON,
        AND,
        OR,
        PLUS,
        XOR,
        MOD,
        FLOATING_POINT_LITERAL,

        FUNCTION_NAME,
        SUB_PARSE,
        SOURCE,
        IS_VARARG,
        MAXSTACKSIZE,
        NUMPARAMS,
        LINEDEFINED,
        LAST_LINEDEFINED,
        UPVALDDESC,


        CODE_START,
        CODE_END,


        OP_UNKNOWN,
        OP_MOVE,

        OP_LOADK,
        OP_LOADKX,
        OP_LOADBOOL,
        OP_LOADNIL,
        OP_GETUPVAL,
        OP_GETTABUP,
        OP_GETTABLE,
        OP_SETTABUP,
        OP_SETUPVAL,
        OP_SETTABLE,
        OP_NEWTABLE,
        OP_SELF,
        OP_ADD,
        OP_SUB,
        OP_MUL,
        OP_MOD,
        OP_POW,
        OP_DIV,
        OP_IDIV,
        OP_BAND,
        OP_BOR,
        OP_BXOR,
        OP_SHL,
        OP_SHR,
        OP_UNM,
        OP_BNOT,
        OP_NOT,
        OP_LEN,
        OP_CONCAT,
        OP_JMP,
        OP_EQ,
        OP_LT,
        OP_LE,
        OP_TEST,
        OP_TESTSET,
        OP_CALL,
        OP_TAILCALL,
        OP_RETURN,
        OP_FORLOOP,
        OP_FORPREP,
        OP_TFORCALL,
        OP_TFORLOOP,
        OP_SETLIST,
        OP_CLOSURE,
        OP_VARARG,
        OP_EXTRAARG,
        //下头三个是nirenr的Androlua+自定义指令，其中TFOREACH只是保留指令，还未实现
        OP_TBC,
        OP_NEWARRAY,
        OP_TFOREACH,


        TRUE,
        FALSE,
        /**
         * NULL or nil
         */
        NULL,
    }
}
