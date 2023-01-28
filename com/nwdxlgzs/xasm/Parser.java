package com.nwdxlgzs.xasm;

import java.util.ArrayList;

import static com.nwdxlgzs.xasm.defines.*;

public class Parser {
    /*
     * 切块解析，所以Parser责任同时切块和解析
     */
    private final CharSequence source;
    private final ArrayList<String> allProtoXasm = new ArrayList<>();
    private final ArrayList<Proto> allProto = new ArrayList<>();

    public Parser(CharSequence source) {
        this.source = source;
    }

    public void splitXasm() {
        Lexer lexer = new Lexer(source);
        boolean onFunction = false;
        int functionStart = 0;
        while (true) {
            Lexer.Tokens token = lexer.nextToken();
            if (token == Lexer.Tokens.EOF) {
                break;
            }
            if (token == Lexer.Tokens.FUNCTION_KEYWORD) {
                String txt = lexer.getTokenText();
                if (".function".equals(txt)) {
                    onFunction = true;
                    functionStart = lexer.getIndex();
                } else if (onFunction && ".end".equals(txt)) {
                    onFunction = false;
                    allProtoXasm.add(source.subSequence(functionStart, lexer.getIndex()).toString());
                }
            }
        }
    }

    public void parse() {
        if (allProtoXasm.size() == 0 || allProto.size() != 0) {
            splitXasm();
        }
        String xasm;
        Proto proto;
        Lexer lexer;
        String txt;
        Lexer.Tokens token = Lexer.Tokens.UNKNOWN;
        for (int i = 0; i < allProtoXasm.size(); i++) {
            proto = new Proto();
            allProto.add(proto);
            xasm = allProtoXasm.get(i);
            lexer = new Lexer(xasm);
//            lexer.gotoNextToken(Lexer.Tokens.FUNCTION_KEYWORD, true);//直接跳到目标
//            lexer.testNextToken(Lexer.Tokens.FUNCTION_KEYWORD, true, true);//测试下一个token是否是目标，如果是则跳到目标，否则按参数回滚
            lexer.gotoNextToken(Lexer.Tokens.FUNCTION_KEYWORD, true);
            txt = lexer.getTokenText();
            if (".function".equals(txt)) {
                lexer.gotoNextToken(Lexer.Tokens.IDENTIFIER, true);
                proto.XasmFuncName = lexer.getTokenText();
                boolean doBreak = false;
                while (true) {
                    if (doBreak) {
                        break;
                    }
                    token = lexer.skipUselessToken();
                    txt = lexer.getTokenText();
                    switch (token) {
                        case PROTO_KEYWORD: {
                            onProtoKeyword(lexer, proto, txt, token, i);
                            continue;
                        }
                        case CODE_KEYWORD: {
                            if (".code-start".equals(txt)) {
                                Lexer.LexerSnapshot snapshot = lexer.snapshot();
                                boolean doBreak_Code = false;
                                while (true) {
                                    if (doBreak_Code) {
                                        break;
                                    }
                                    token = lexer.skipUselessToken();
                                    txt = lexer.getTokenText();
                                    if (Lexer.Tokens.CODE_KEYWORD == token) {
                                        if (".code-end".equals(txt)) {
                                            doBreak_Code = true;
                                            break;
                                        } else {
                                            throw new RuntimeException("解析错误(CODE_KEYWORD.code-start):" + token + "(" + txt + ")不应该在这里。(索引" + i + ")");
                                        }
                                    }
                                }
                            } else {
                                throw new RuntimeException("解析错误(CODE_KEYWORD):" + token + "(" + txt + ")不应该在这里。(索引" + i + ")");
                            }
                            break;
                        }
                        case FUNCTION_KEYWORD: {
                            if (".end".equals(txt)) {
                                doBreak = true;
                            } else {
                                throw new RuntimeException("解析错误(FUNCTION_KEYWORD):" + token + "(" + txt + ")不应该在这里。(索引" + i + ")");
                            }
                            break;
                        }
                        default: {
                            throw new RuntimeException("解析错误(default):" + token + "(" + txt + ")不应该在这里。(索引" + i + ")");
                        }
                    }
                }
            } else {
                throw new RuntimeException("解析错误:" + token + "(" + txt + ")不应该在这里。(索引" + i + ")");
            }
        }

    }

    private static void onProtoKeyword(Lexer lexer, Proto proto, String txt, Lexer.Tokens token, int i) {
        if (".sub-parse".equals(txt)) {//这个是给人看的，解析工具其实可以不认的，只是告诉人这个函数会解析子函数
            lexer.gotoNextToken(Lexer.Tokens.VALUE_KEYWORD, true);
        } else if (".source".equals(txt)) {
            token = lexer.skipUselessToken();
            txt = lexer.getTokenText();
            if (Lexer.Tokens.STRING == token) {
                proto.source = readAsTString(txt);
            } else if (Lexer.Tokens.VALUE_KEYWORD == token && ("null".equals(txt) || "nil".equals(txt))) {
                proto.source = TValue.createNil();
            } else {
                throw new RuntimeException("解析错误(PROTO_KEYWORD.source):" + token + "(" + txt + ")不应该在这里。(索引" + i + ")");
            }
        } else if (".is_vararg".equals(txt)) {
            lexer.gotoNextToken(Lexer.Tokens.INTEGER_LITERAL, true);
            txt = lexer.getTokenText();
            proto.is_vararg = (byte) (Integer.parseInt(txt) & 0xff);
        } else if (".maxstacksize".equals(txt)) {
            lexer.gotoNextToken(Lexer.Tokens.INTEGER_LITERAL, true);
            txt = lexer.getTokenText();
            proto.maxstacksize = (byte) (Integer.parseInt(txt) & 0xff);
        } else if (".numparams".equals(txt)) {
            lexer.gotoNextToken(Lexer.Tokens.INTEGER_LITERAL, true);
            txt = lexer.getTokenText();
            proto.numparams = (byte) (Integer.parseInt(txt) & 0xff);
        } else if (".linedefined".equals(txt)) {
            lexer.gotoNextToken(Lexer.Tokens.INTEGER_LITERAL, true);
            txt = lexer.getTokenText();
            proto.linedefined = Integer.parseInt(txt);
        } else if (".lastlinedefined".equals(txt)) {
            lexer.gotoNextToken(Lexer.Tokens.INTEGER_LITERAL, true);
            txt = lexer.getTokenText();
            proto.lastlinedefined = Integer.parseInt(txt);
        } else if (".locvars".equals(txt)) {
            token = lexer.skipUselessToken();
            txt = lexer.getTokenText();
            int idx = proto.sizelocvars();
            if (Lexer.Tokens.STRING == token) {
                proto.locvars = realloc(proto.locvars, idx + 1);
                proto.locvars[idx] = new LocVar();
                proto.locvars[idx].varname = readAsTString(txt);
            } else if (Lexer.Tokens.VALUE_KEYWORD == token && ("null".equals(txt) || "nil".equals(txt))) {
                proto.locvars = realloc(proto.locvars, idx + 1);
                proto.locvars[idx] = new LocVar();
                proto.locvars[idx].varname = TValue.createNil();
            } else {
                throw new RuntimeException("解析错误(PROTO_KEYWORD.locvars):" + token + "(" + txt + ")不应该在这里。(索引" + i + ")");
            }
            lexer.gotoNextToken(Lexer.Tokens.INTEGER_LITERAL, true);
            txt = lexer.getTokenText();
            proto.locvars[idx].startpc = Integer.parseInt(txt);
            lexer.gotoNextToken(Lexer.Tokens.INTEGER_LITERAL, true);
            txt = lexer.getTokenText();
            proto.locvars[idx].endpc = Integer.parseInt(txt);
        } else if (".upvaldesc".equals(txt)) {
            token = lexer.skipUselessToken();
            txt = lexer.getTokenText();
            int idx = proto.sizeupvalues();
            if (Lexer.Tokens.STRING == token) {
                proto.upvalues = realloc(proto.upvalues, idx + 1);
                proto.upvalues[idx] = new Upvaldesc();
                proto.upvalues[idx].name = readAsTString(txt);
            } else if (Lexer.Tokens.VALUE_KEYWORD == token && ("null".equals(txt) || "nil".equals(txt))) {
                proto.upvalues = realloc(proto.upvalues, idx + 1);
                proto.upvalues[idx] = new Upvaldesc();
                proto.upvalues[idx].name = TValue.createNil();
            } else {
                throw new RuntimeException("解析错误(PROTO_KEYWORD.upvaldesc):" + token + "(" + txt + ")不应该在这里。(索引" + i + ")");
            }
            lexer.gotoNextToken(Lexer.Tokens.INTEGER_LITERAL, true);
            txt = lexer.getTokenText();
            proto.upvalues[idx].idx = (byte) (Integer.parseInt(txt) & 0xff);
            lexer.gotoNextToken(Lexer.Tokens.VALUE_KEYWORD, true);
            txt = lexer.getTokenText();
            if ("true".equals(txt)) {
                proto.upvalues[idx].instack = true;
            } else if ("false".equals(txt)) {
                proto.upvalues[idx].instack = false;
            } else {
                throw new RuntimeException("解析错误(PROTO_KEYWORD.upvaldesc):" + token + "(" + txt + ")不应该在这里。(索引" + i + ")");
            }
        } else {
            throw new RuntimeException("解析错误(PROTO_KEYWORD):" + token + "(" + txt + ")不应该在这里。(索引" + i + ")");
        }
    }

    public ArrayList<String> getAllProtoXasm() {
        return allProtoXasm;
    }

    public ArrayList<Proto> getAllProto() {
        return allProto;
    }
}
