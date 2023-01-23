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
    private TokenState currentTokenState;

    private TokenState lastTokenState;

    private String[] protoKeywords = {"sub-parse", "source", "is_vararg",
            "maxstacksize",
            "numparams",
            "linedefined",
            "lastlinedefined",
            "locvars",
            "upvaldesc",

    };

    private String[] functionKeywords = {
            "function", "end"
    };

    private String[] codeKeywords = {
            "code-start", "code-end"
    };

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

            "neq",
            "nlt",
            "nle",
            "func",
            "def",


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
        currentTokenState = new TokenState(Tokens.WHITESPACE, length, offset);
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
        return currentTokenState.token;
    }

    private char peekCharWithLength(int i) {
        return source.charAt(offset + length + i);
    }


    public Tokens nextToken() {
        lastTokenState = currentTokenState;
        return (currentTokenState = calcLineAndColumn(nextTokenInternal())).token;
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

    private TokenState wrapState(Tokens token) {
        TokenState state = new TokenState(token, length, offset);
        return state;
    }

    private TokenState calcLineAndColumn(Tokens result) {

        column += lastTokenState.length;

        if (lastTokenState.token == Tokens.NEWLINE) {
            line++;
            column = 0;
        }

        return wrapState(result);
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
            line++;
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

        //空格
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

        //数字
        if (isDigit(ch)) {
            while (offset + length < bufferLen && isDigit(peekCharWithLength())) {
                length++;
            }
            return Tokens.INTEGER_LITERAL;
        }

        // keyword
        if (isLetter(ch)) {
            return scanIdentifier();
        }

        //为点
        if (ch == '.') {
            char nextChar = peekCharWithLength(1);

            if (isLetter(nextChar)) {
                // 这里是移除点
                index++;
                length--;
                Tokens result = scanIdentifier();
                //还是得加回来的。
                index--;
                length++;
                return result;
            }

            return Tokens.DOT;
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
        //呃不对，还有注释啥的。。
        char ch;
        while (!isWhitespace((ch = peekCharWithLength()))) {
            if (ch == '$') {
                break;
            }
            length++;
        }

        String tokenText = getTokenText();

        System.out.println(tokenText);

        for (String keyword : opCodeKeyWords) {
            if (tokenText.startsWith(keyword)) {
                return Tokens.OP_KEYWORD;
            }
        }

        // opxx
        if (tokenText.startsWith("op")) {
            String tokenTextSub = tokenText.substring(2, tokenText.length() - 1);
            int index = 0;
            boolean isNumber = true;
            while (index < tokenTextSub.length()) {
                if (!isDigit(tokenTextSub.charAt(index))) {
                    isNumber = false;
                    break;
                }
                index++;
            }
            if (isNumber) {
                return Tokens.OP_KEYWORD;
            }
        }


        // goto_
        if (tokenText.startsWith("goto_")) {
            String tokenTextSub = tokenText.substring(5, tokenText.length() - 1);
            int index = 0;
            boolean isNumber = true;
            while (index < tokenTextSub.length()) {
                if (!isDigit(tokenTextSub.charAt(index))) {
                    isNumber = false;
                    break;
                }
                index++;
            }
            if (isNumber) {
                return Tokens.OP_KEYWORD;
            }
        }

        for (String keyword : valueKeyWords) {
            if (tokenText.startsWith(keyword)) {
                return Tokens.VALUE_KEYWORD;
            }
        }

        for (String keyword : codeKeywords) {
            if (tokenText.startsWith(keyword)) {
                return Tokens.CODE_KEYWORD;
            }
        }

        for (String keyword : functionKeywords) {
            if (tokenText.startsWith(keyword)) {
                return Tokens.FUNCTION_KEYWORD;
            }
        }


        return Tokens.IDENTIFIER;
    }

    protected void scanNewline() {
        if (offset + length < bufferLen && peekChar(offset + length) == '\n') {
            length++;
        }
    }

    protected static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || c == '_' || (c >= 'A' && c <= 'Z');
    }

    protected static boolean isDigit(char c) {
        return ((c >= '0' && c <= '9'));
    }

    protected static boolean isWhitespace(char c) {
        return (c == '\t' || c == ' ' || c == '\f' || c == '\n' || c == '\r');
    }


    protected class TokenState {
        Tokens token;
        int length;
        int offset;

        public TokenState(Tokens token, int length, int offset) {
            this.token = token;
            this.length = length;
            this.offset = offset;
        }
    }

    public enum Tokens {
        WHITESPACE, NEWLINE, UNKNOWN, EOF,

        LINE_COMMENT,

        DIV, MULT, IDENTIFIER, INTEGER_LITERAL, DOT, MINUS, STRING, CHARACTER_LITERAL, LPAREN, RPAREN, LBRACE, RBRACE, LBRACK, RBRACK, COMMA, EQ, GT, LT, NOT, COMP, QUESTION, COLON, AND, OR, PLUS, XOR, MOD, FLOATING_POINT_LITERAL, SEMICOLON,

        PROTO_KEYWORD,

        CODE_KEYWORD, FUNCTION_KEYWORD,

        OP_KEYWORD,


        VALUE_KEYWORD,
    }
}
