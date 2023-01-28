package com.nwdxlgzs.xasm;

import java.io.Serializable;

import static com.nwdxlgzs.xasm.defines.*;

public class TValue implements Serializable {
    public static final int LUA_TNIL = 0;
    public static final int LUA_TBOOLEAN = 1;
    public static final int LUA_TNUMBER = 3;
    public static final int LUA_TNUMFLT = LUA_TNUMBER | (0 << 4);
    public static final int LUA_TNUMINT = LUA_TNUMBER | (1 << 4);
    public static final int LUA_TSTRING = 4;
    public static final int LUA_TSHRSTR = LUA_TSTRING | (0 << 4);
    public static final int LUA_TLNGSTR = LUA_TSTRING | (1 << 4);
    public static final int LUAI_MAXSHORTLEN = 40;

    public static final TValue NIL = TValue.createNil();
    public static final TValue TRUE = TValue.createBoolean(true);
    public static final TValue FALSE = TValue.createBoolean(false);
    public static final TValue EMPTY_STRING = TValue.createString("");

    public Object value_;
    public int tt_;

    public TValue() {
    }

    public void getNil() {
        if (tt_ != LUA_TNIL) {
            throw new RuntimeException("TValue不是LUA_TNIL");
        }
    }

    public boolean getBoolean() {
        if (tt_ != LUA_TBOOLEAN) {
            throw new RuntimeException("TValue不是LUA_TBOOLEAN");
        }
        return (boolean) value_;
    }

    public String getString() {
        if (tt_ != LUA_TSHRSTR && tt_ != LUA_TLNGSTR) {
            throw new RuntimeException("TValue不是LUA_TSHRSTR或LUA_TLNGSTR");
        }
        return (String) new String((byte[]) value_);
    }

    public byte[] getStringBytes() {
        if (tt_ != LUA_TSHRSTR && tt_ != LUA_TLNGSTR) {
            throw new RuntimeException("TValue不是LUA_TSHRSTR或LUA_TLNGSTR");
        }
        return (byte[]) value_;
    }

    public double getFLT() {
        if (tt_ != LUA_TNUMFLT) {
            throw new RuntimeException("TValue不是LUA_TNUMFLT");
        }
        return (double) value_;
    }

    public long getINT() {
        if (tt_ != LUA_TNUMINT) {
            throw new RuntimeException("TValue不是LUA_TNUMINT");
        }
        return (long) value_;
    }

    public static TValue createNil() {
        if (NIL == null) {
            TValue tv = new TValue();
            tv.tt_ = LUA_TNIL;
            return tv;
        }
        return NIL;
    }

    public static TValue createBoolean(boolean b) {
        if (b) {
            if (TRUE == null) {
                TValue tv = new TValue();
                tv.tt_ = LUA_TBOOLEAN;
                tv.value_ = true;
                return tv;
            }
            return TRUE;
        } else {
            if (FALSE == null) {
                TValue tv = new TValue();
                tv.tt_ = LUA_TBOOLEAN;
                tv.value_ = false;
                return tv;
            }
            return FALSE;
        }
    }

    public static TValue createNumber(double d) {
        TValue tv = new TValue();
        tv.tt_ = LUA_TNUMFLT;
        tv.value_ = d;
        return tv;
    }

    public static TValue createInteger(long l) {
        TValue tv = new TValue();
        tv.tt_ = LUA_TNUMINT;
        tv.value_ = l;
        return tv;
    }

    public static TValue createString(String str) {
        return createString(str.getBytes());
    }

    public static TValue createString(byte[] data) {
        if (data == null || data.length == 0) {
            if (EMPTY_STRING == null) {//初始化EMPTY_STRING
                TValue tv = new TValue();
                tv.tt_ = LUA_TSHRSTR;
                tv.value_ = new byte[0];
                return tv;
            }
            return EMPTY_STRING;
        }
        TValue ts = new TValue();
        ts.value_ = data;
        ts.tt_ = data.length <= LUAI_MAXSHORTLEN ? LUA_TSHRSTR : LUA_TLNGSTR;
        return ts;
    }

    public static TValue createString(byte[] data, int length) {
        return createString(data, 0, length);
    }

    public static TValue createString(byte[] data, int offset, int length) {
        byte[] newData = new byte[length];
        System.arraycopy(data, offset, newData, 0, length);
        return createString(newData);
    }

    public String toWarpString() {
        if (tt_ == LUA_TNIL) {
            return "\"nil\"";
        } else if (tt_ == LUA_TBOOLEAN) {
            return (boolean) value_ ? "\"true\"" : "\"false\"";
        } else if (tt_ == LUA_TNUMFLT) {
            return "\"" + String.valueOf((double) value_) + "\"";
        } else if (tt_ == LUA_TNUMINT) {
            return "\"" + String.valueOf((long) value_) + "\"";
        } else if (tt_ == LUA_TSHRSTR || tt_ == LUA_TLNGSTR) {
            byte[] data = (byte[]) value_;
            StringContentLevel level = TStringCheck(data);
            switch (level) {
                case STR_CONTENT_BUFFER: {
                    StringBuilder sb = new StringBuilder();
                    sb.append("\"");
                    for (int i = 0; i < data.length; i++) {
                        sb.append(String.format("\\x%02X", data[i]));
                    }
                    sb.append("\"");
                    return sb.toString();
                }
                case STR_CONTENT_STRING:
                default: {
                    StringBuilder sb = new StringBuilder();
                    sb.append("\"");
                    int start = 0;
                    for (int i = 0; i < data.length; i++) {
                        byte b = data[i];
                        boolean startTrans = true;
                        switch (data[i]) {
                            case '"': {
                                b = '\"';
                                break;
                            }
                            case '\0': {
                                b = '0';
                                break;
                            }
                            case '\\': {
                                b = '\\';
                                break;
                            }
                            case '\b': {
                                b = 'b';
                                break;
                            }
                            case '\f': {
                                b = 'f';
                                break;
                            }
                            case '\n': {
                                b = 'n';
                                break;
                            }
                            case '\r': {
                                b = 'r';
                                break;
                            }
                            case '\t': {
                                b = 't';
                                break;
                            }
                            default: {
                                startTrans = false;
                                break;
                            }
                        }
                        if (startTrans) {
                            sb.append(new String(data, start, i - start));
                            sb.append("\\");
                            sb.append((char) b);
                            start = i + 1;
                        }
                    }
                    if (start < data.length) {
                        sb.append(new String(data, start, data.length - start));
                    }
                    sb.append("\"");
                    return sb.toString();
                }
            }
        } else {
            return "\"\"";
        }
    }

    public String toVarString() {
        if (tt_ == LUA_TSHRSTR || tt_ == LUA_TLNGSTR) {
            return toWarpString();
        } else {
            return toString();
        }
    }

    @Override
    public String toString() {
        switch (tt_) {
            case LUA_TNIL: {
                return "nil";
            }
            case LUA_TBOOLEAN: {
                return (boolean) value_ ? "true" : "false";
            }
            case LUA_TNUMFLT: {
                return String.valueOf((double) value_);
            }
            case LUA_TNUMINT: {
                return String.valueOf((long) value_);
            }
            case LUA_TSHRSTR:
            case LUA_TLNGSTR: {
                if (value_ == null) {
                    return "null";
                }
                return new String((byte[]) value_);
            }
            default: {
                return "unknown type";
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof TValue) {
            TValue otherTValue = (TValue) other;
            if (tt_ != otherTValue.tt_) {
                //LUA_TNUMFLT和LUA_TNUMINT可以相互转换
                if (tt_ == LUA_TNUMFLT && otherTValue.tt_ == LUA_TNUMINT) {
                    return (double) value_ == (long) otherTValue.value_;
                } else if (tt_ == LUA_TNUMINT && otherTValue.tt_ == LUA_TNUMFLT) {
                    return (long) value_ == (double) otherTValue.value_;
                }
                return false;
            }
            switch (tt_) {
                case LUA_TNIL: {
                    return true;
                }
                case LUA_TBOOLEAN: {
                    return (boolean) value_ == (boolean) otherTValue.value_;
                }
                case LUA_TNUMFLT: {
                    return (double) value_ == (double) otherTValue.value_;
                }
                case LUA_TNUMINT: {
                    return (long) value_ == (long) otherTValue.value_;
                }
                case LUA_TSHRSTR:
                case LUA_TLNGSTR: {
                    if (value_ == null) {
                        return otherTValue.value_ == null;
                    }
                    byte[] data = (byte[]) value_;
                    byte[] otherData = (byte[]) otherTValue.value_;
                    if (data.length != otherData.length) {
                        return false;
                    }
                    for (int i = 0; i < data.length; i++) {
                        if (data[i] != otherData[i]) {
                            return false;
                        }
                    }
                }
                default: {
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        switch (tt_) {
            case LUA_TNIL: {
                return 0;
            }
            case LUA_TBOOLEAN: {
                return (boolean) value_ ? 1 : 0;
            }
            case LUA_TNUMFLT: {
                return (int) (double) value_;
            }
            case LUA_TNUMINT: {
                return (int) (long) value_;
            }
            case LUA_TSHRSTR:
            case LUA_TLNGSTR: {
                if (value_ == null) {
                    return 0;
                }
                byte[] data = (byte[]) value_;
                int hash = 0;
                for (int i = 0; i < data.length; i++) {
                    hash = hash * 131 + data[i];
                }
                return hash;
            }
            default: {
                return -1;
            }
        }
    }
}
