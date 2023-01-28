package com.nwdxlgzs.xasm;

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

    private String[] protoKeywords = {
            "sub-parse",
            "source",
            "is_vararg",
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
            "code-start", "code-end", "line"
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
            throw new IllegalArgumentException("输入不能为空");
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

    public void reset() {
        init();
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

    public void gotoNextToken(Tokens tk, boolean justSkipUselessToken) {
        while (true) {
            Tokens token = nextToken();
            if (token == Lexer.Tokens.EOF || token == tk) {
                return;
            }
            if (justSkipUselessToken) {
                if (token == Tokens.WHITESPACE || token == Tokens.NEWLINE || token == Tokens.LINE_COMMENT ||
                        token == Tokens.OPERATOR || token == Tokens.SEMICOLON) {
                    continue;
                } else {
                    throw new RuntimeException("语法错误:未期待的令牌" + token);
                }
            }
        }
    }

    public Tokens skipUselessToken() {
        while (true) {
            Tokens token = nextToken();
            if (token == Lexer.Tokens.EOF) {
                return token;
            }
            if (token == Tokens.WHITESPACE || token == Tokens.NEWLINE || token == Tokens.LINE_COMMENT ||
                    token == Tokens.OPERATOR || token == Tokens.SEMICOLON) {
                continue;
            } else {
                return token;
            }
        }
    }

    public boolean testNextToken(Tokens tk, boolean backIfNotMatch, boolean skipUselessToken) {
        int oldIndex = index;
        int oldOffset = offset;
        int oldLength = length;
        int oldLine = line;
        int oldColumn = column;
        TokenState oldCurrentTokenState = currentTokenState;
        TokenState oldLastTokenState = lastTokenState;
        while (true) {
            Tokens token = nextToken();
            if (token == tk) {
                return true;
            }
            if (token == Lexer.Tokens.EOF) {
                return false;
            }
            if (skipUselessToken) {
                if (token == Tokens.WHITESPACE || token == Tokens.NEWLINE || token == Tokens.LINE_COMMENT ||
                        token == Tokens.OPERATOR || token == Tokens.SEMICOLON) {
                    continue;
                } else {
                    if (backIfNotMatch) {
                        index = oldIndex;
                        offset = oldOffset;
                        length = oldLength;
                        line = oldLine;
                        column = oldColumn;
                        currentTokenState = oldCurrentTokenState;
                        lastTokenState = oldLastTokenState;
                    }
                    return false;
                }
            }
        }
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

    private char peekChar(int offset) {
        return source.charAt(offset);
    }

    private TokenState wrapState(Tokens token) {
        return new TokenState(token, length, offset);
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
            return scanNumber();
        }

        // keyword
        if (isLetter(ch)) {
            return scanIdentifier();
        }

        //为点
        if (ch == '.') {
            char nextChar = peekCharWithLength(1);

            if (nextChar == '.') {
                // ..
                return Tokens.OP_KEYWORD;
            }

            if (isLetter(nextChar)) {
                // 这里是移除点
                index++;
                /*length--;*/
                Tokens result = scanIdentifier();
                //还是得加回来的。
                index--;
                return result;
            }


            return Tokens.DOT;
        }

        //处理字符串
        if (ch == '"') {
            return scanString();
        }

        //处理opcode
        //有的opcode使用多个符号，需要消费长度
        if (ch == '~') {
            // ~ ~= ~()
            ch = peekCharWithLength();
            if (ch == '=') {
                // ~=
                length++;
            } else if (ch == '(' && (((length++) > 0) && matchBracket('(', ')'))) {
                // ~()
                length++;
            }
            return Tokens.OP_KEYWORD;
        } else if (ch == '/') {
            if (matchBracket('/', '/')) {
                // '//'
                length++;
            }
            return Tokens.OP_KEYWORD;
        } else if (ch == '&') {
            return Tokens.OP_KEYWORD;
        } else if (ch == '#') {
            return Tokens.OP_KEYWORD;
        } else if (ch == '|') {
            return Tokens.OP_KEYWORD;
        } else if (ch == '+') {
            return Tokens.OP_KEYWORD;
        } else if (ch == '*') {
            return Tokens.OP_KEYWORD;
        } else if (ch == '^') {
            return Tokens.OP_KEYWORD;
        } else if (ch == '=' && matchBracket('=', '=')) {
            // ==
            length++;
            return Tokens.OP_KEYWORD;
        } else if (ch == '{' && matchBracket('{', '}')) {
            // {}
            length++;
            return Tokens.OP_KEYWORD;
        } else if (ch == '[' && matchBracket('[', ']')) {
            // {}
            length++;
            return Tokens.OP_KEYWORD;
        } else if (ch == '!' && matchBracket('!', '=')) {
            // {}
            length++;
            return Tokens.OP_KEYWORD;
        } else if (ch == '[' && matchBracket('[', ']')) {
            // []
            length++;
            return Tokens.OP_KEYWORD;
        } else if (ch == '-') {
            // - -()
            ch = peekCharWithLength();
            if (ch == '(' && (((length++) > 0) && matchBracket('(', ')'))) {
                // -()
                length++;
            } else if (ch == '>') {
                // ->
                length++;
                return Tokens.OPERATOR;
            }
            return Tokens.OP_KEYWORD;
        } else if (ch == '<') {
            // < <= <<
            if (matchBracket('<', '=')) {
                // <=
                length++;
            } else if (matchBracket('<', '<')) {
                // <<
                length++;
            }
            return Tokens.OP_KEYWORD;
        } else if (ch == '>') {
            // > >= >>

            if (matchBracket('>', '=')) {
                // >=
                length++;
            } else if (matchBracket('>', '>')) {
                // >>
                length++;
            }
            return Tokens.OP_KEYWORD;
        }

        return Tokens.UNKNOWN;
    }

    private Tokens scanNumber() {
        char ch;
        boolean hasDot = false;
        while (offset + length < bufferLen) {
            boolean isDigit = isDigit((ch = peekCharWithLength()));
            if (!isDigit && ch == '.') {
                if (hasDot) {
                    throw new RuntimeException("错误的语法！在 (" + line + "," + (column + length) + ") 处 重复定义了.");
                } else {
                    hasDot = true;
                }
            } else if (!isDigit) {
                break;
            }
            length++;
        }
        return hasDot ? Tokens.FLOAT_LITERAL : Tokens.INTEGER_LITERAL;
    }

    private boolean matchBracket(char left, char right) {
        char currentLeft = peekCharWithLength(-1);
        char currentRight = peekCharWithLength();
        return currentLeft == left && currentRight == right;
    }

    protected final void throwIfNeeded() {
        if (offset + length == bufferLen) {
            throw new RuntimeException("太大的Token！考虑语法错误");
        }
    }


    protected final boolean isNumber(String str) {
        boolean isNumber = true;
        int index = 0;
        while (index < str.length()) {
            if (!isDigit(str.charAt(index))) {
                isNumber = false;
                break;
            }
            index++;
        }
        return isNumber;
    }

    protected Tokens scanString() {
        throwIfNeeded();

        char current = 0;

        //由于有转义符号的存在，不能直接判断是否为"\"
        while (true) {
            current = peekCharWithLength();

            length++;

            if (current == '"' || current == '\n') {
                break;
            }

            if (current == '\\') {
                current = peekCharWithLength();
                if (current == '\\' || current == 't' || current == 'f' || current == 'n' || current == 'r' || current == '0' || current == '\"' || current == 'b' || current == '\'') {
                    length++;
                }
            }


            if (offset + length >= bufferLen) {
                throw new RuntimeException("缺少正常的\"");
            }

        }


        return Tokens.STRING;
    }

    protected Tokens scanIdentifier() {
        throwIfNeeded();

        //对于标识符来说，只要不遇到空格符就是合法的
        //其他校检我暂时懒得做了
        //呃不对，还有注释啥的。。
        char ch;
        while (!isWhitespace((ch = peekCharWithLength()))) {
            if (isSymbol(ch)) {
                break;
            }
            length++;
        }


        String tokenText = getTokenText();

        for (String keyword : functionKeywords) {
            if (tokenText.startsWith(keyword)) {
                return Tokens.FUNCTION_KEYWORD;
            }
        }

        for (String keyword : protoKeywords) {
            if (tokenText.startsWith(keyword)) {
                return Tokens.PROTO_KEYWORD;
            }
        }

        for (String keyword : opCodeKeyWords) {
            if (tokenText.startsWith(keyword)) {
                return Tokens.OP_KEYWORD;
            }
        }

        // opxx
        if (tokenText.startsWith("op")) {
            String tokenTextSub = tokenText.substring(2, tokenText.length() - 1);
            boolean isNumber = isNumber(tokenTextSub);
            if (isNumber) {
                return Tokens.OP_KEYWORD;
            }
        }


        // goto_
        if (tokenText.startsWith("goto_")) {
            String tokenTextSub = tokenText.substring(5, tokenText.length() - 1);
            boolean isNumber = isNumber(tokenTextSub);
            if (isNumber) {
                return Tokens.OP_KEYWORD;
            }
        }

        // rxx,uxx,kxx,pxx
        if (tokenText.length() > 1 && (tokenText.charAt(0) == 'r' || tokenText.charAt(0) == 'k' ||
                tokenText.charAt(0) == 'u' || tokenText.charAt(0) == 'p')) {

            String tokenTextSub = tokenText.substring(1, tokenText.length() - 1);
            boolean isNumber = isNumber(tokenTextSub);
            if (isNumber) {
                return Tokens.OP_ARG;
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
        return (c >= '0' && c <= '9');
    }

    protected static boolean isSymbol(char c) {
        return (c == '$' || c == '>' || c == '<' || c == ';' || c == '.');
    }

    protected static boolean isWhitespace(char c) {
        return (c == '\t' || c == ' ' || c == '\f' || c == '\n' || c == '\r');
    }


    protected static class TokenState {
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
        IDENTIFIER,
        INTEGER_LITERAL, FLOAT_LITERAL,
        STRING,
        DOT, OPERATOR, SEMICOLON,
        PROTO_KEYWORD, CODE_KEYWORD, FUNCTION_KEYWORD,
        OP_KEYWORD, OP_ARG, VALUE_KEYWORD,
    }
}
