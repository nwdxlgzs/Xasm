package com.nwdxlgzs.xasm;

import static com.nwdxlgzs.xasm.OPCode.OpArgMask.OpArgK;
import static com.nwdxlgzs.xasm.OPCode.OpArgMask.OpArgN;
import static com.nwdxlgzs.xasm.OPCode.OpArgMask.OpArgR;
import static com.nwdxlgzs.xasm.OPCode.OpArgMask.OpArgU;
import static com.nwdxlgzs.xasm.OPCode.OpMode.iABC;
import static com.nwdxlgzs.xasm.OPCode.OpMode.iABx;
import static com.nwdxlgzs.xasm.OPCode.OpMode.iAsBx;
import static com.nwdxlgzs.xasm.OPCode.OpMode.iAx;

import java.util.Arrays;

public class Lexer {

    private CharSequence source;
    protected int bufferLen;
    private int line;
    private int column;
    private int index;
    protected int offset;
    protected int length;
    private Tokens currentToken;

    private String[] protoKeywords = {};

    private String[] valueKeyWords = {
            "true", "false", "nil", "null"
    };

    private String[] opCodeKeyWords = {
            "unknown",
            "move",
            "loadk",
            "loadkx",
            "loadbool",
            "loadnil",
            "getupval",
            "gettabup",
            "gettable",
            "settabup",
            "setupval",
            "settable",
            "newtable",
            "self",
            "add",
            "sub",
            "mul",
            "mod",
            "pow",
            "div",
            "idiv",
            "band",
            "bor",
            "bxor",
            "shl",
            "shr",
            "unm",
            "bnot",
            "not",
            "len",
            "concat",
            "jmp",
            "eq",
            "lt",
            "le",
            "test",
            "testset",
            "call",
            "tailcall",
            "return",
            "forloop",
            "forprep",
            "tforcall",
            "tforloop",
            "setlist",
            "closure",
            "vararg",
            "extraarg",
            //下头三个是nirenr的Androlua+自定义指令，其中TFOREACH只是保留指令，还未实现
            "tbc",
            "newarray",
            "tforeach",
            //下头是不存在的op
            "op50",
            "op51",
            "op52",
            "op53",
            "op54",
            "op55",
            "op56",
            "op57",
            "op58",
            "op59",
            "op60",
            "op61",
            "op62",
            "op63",

            "neq",
            "nlt",
            "nle",
            "func",
            "def"

    };

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
        if (index + length > bufferLen) {
            return source.subSequence(index, bufferLen).toString();
        }
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

    private char nextChar() {
        offset++;
        return peekNextChar();
    }

    private char nextCharWithLength() {
        length++;
        return peekCharWithLength();
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

        if (isWhitespace(ch)) {
            char chLocal;
            while (isWhitespace(chLocal = peekCharWithLength())) {
                if (chLocal == '\r' || chLocal == '\n' || offset + length < bufferLen) {
                    break;
                }
                length++;
            }
            return Tokens.WHITESPACE;
        }

        // keyword
        if (isLetter(ch)) {
            return scanIdentifier();
        }


        return Tokens.UNKNOWN;
    }

    protected final void throwIfNeeded() {
        if (offset + length == bufferLen) {
            throw new RuntimeException("Token too long");
        }
    }

    protected Tokens scanIdentifier() {
        throwIfNeeded();

        //对于标识符来说，只要不遇到空格符就是合法的
        //其他校检我暂时懒得做了


        while (!isWhitespace(peekCharWithLength())) {
            length++;
        }

        String tokenText = getTokenText();

        for (String keyword : opCodeKeyWords) {
            if (tokenText.startsWith(keyword)) {
                return Tokens.OP_KEYWORD;
            }
        }

        for (String keyword : valueKeyWords) {
            if (tokenText.startsWith(keyword)) {
                return Tokens.VALUE_KEYWORD;
            }
        }


        return Tokens.IDENTIFIER;
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

        PROTO_KEYWORD,

        CODE_KEYWORD,
        FUNCTION_KEYWORD,

        OP_KEYWORD,


        VALUE_KEYWORD,
    }
}
