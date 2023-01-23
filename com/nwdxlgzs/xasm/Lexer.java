package com.nwdxlgzs.xasm;

public class Lexer {

    private CharSequence source;
    protected int bufferLen;
    private int line;
    private int column;
    private int index;
    protected int offset;
    protected int length;
    private Tokens currentToken;


    public Lexer(CharSequence src) {
        if (src == null) {
            throw new IllegalArgumentException("src can not be null");
        }
        this.source = src;
        init();
    }

    private void init() {
        line = 0;
        column = 0;
        length = 0;
        index = 0;
        currentToken = Tokens.WHITESPACE;
        this.bufferLen = source.length();
    }

    public int getTokenLength() {
        return length;
    }

    public String getTokenText() {
        return source.subSequence(index, index + length).toString();
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public int getIndex() {
        return index;
    }

    public Tokens getToken() {
        return currentToken;
    }

    private char peekCharWithLength(int i) {
        return source.charAt(i);
    }


    public Tokens nextToken() {
        return currentToken = nextTokenInternal();
    }

    private char peekNextChar() {
        return source.charAt(offset);
    }

    private char peekCharWithLength() {
        return source.charAt(offset + length);
    }

    private char peekChar(int offset) {
        return source.charAt(offset);
    }

    private Tokens nextTokenInternal() {

        index = index + length;
        offset = offset + length;
        if (offset >= bufferLen) {
            return Tokens.EOF;
        }

        char ch = peekNextChar();
        length = 1;


        //分析简单char
        if (ch == '\n') {
            return Tokens.NEWLINE;
        } else if (ch == '\r') {
            scanNewline();
            return Tokens.NEWLINE;
        } else if (ch == '$') {
            while (offset + length < bufferLen && peekCharWithLength() != '\n') {
                length++;
            }
            return Tokens.LINE_COMMENT;
        } else if (ch == ';') {
            return Tokens.SEMICOLON;
        }


        return Tokens.UNKNOWN;
    }

    protected final void throwIfNeeded() {
        if (offset + length == bufferLen) {
            throw new RuntimeException("Token too long");
        }
    }

    protected void scanNewline() {
        if (offset + length < bufferLen && peekCharWithLength(offset + length) == '\n') {
            length++;
        }
    }

    protected static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || c == '_' || (c >= 'A' && c <= 'Z');
    }

    protected static boolean isDigit(char c) {
        return ((c >= '0' && c <= '9') /*|| (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f')*/);
    }

    protected static boolean isWhitespace(char c) {
        return (c == '\t' || c == ' ' || c == '\f' || c == '\n' || c == '\r');
    }

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
        SEMICOLON,

        FUNCTION_START,
        FUNCTION_END,
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
