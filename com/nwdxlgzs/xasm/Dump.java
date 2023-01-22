package com.nwdxlgzs.xasm;

import static com.nwdxlgzs.xasm.TValue.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Dump extends defines {
    private boolean strip;
    private Proto proto;
    private ByteArrayOutputStream stream;


    public Dump(Proto proto, boolean strip) {
        this.proto = proto;
        this.strip = strip;
    }

    private void writeHeader() throws IOException {
        writeLiteral(LUA_SIGNATURE);
        writeByte(LUAC_VERSION[0]);
        writeByte(LUAC_FORMAT[0]);
        writeLiteral(LUAC_DATA);
        writeByte((byte) sizeof_int);
        writeByte((byte) sizeof_size_t);
        writeByte((byte) sizeof_Instruction);
        writeByte((byte) sizeof_lua_Integer);
        writeByte((byte) sizeof_lua_Number);
        writeLiteral(LUAC_INT);// 0x5678
        writeLiteral(LUAC_NUM);// 370.5
    }

    private void writeCode(Proto f) throws IOException {
        int sizecode = f.sizecode();
        writeInt(sizecode);
        for (int i = 0; i < sizecode; i++) {
            writeInstruction(f.code[i]);
        }
    }

    private void writeConstants(Proto f) throws IOException {
        int sizek = f.sizek();
        writeInt(sizek);
        for (int i = 0; i < sizek; i++) {
            TValue o = f.k[i];
            writeByte((byte) o.tt_);
            switch (o.tt_) {
                case LUA_TNIL:
                    break;
                case LUA_TBOOLEAN:
                    writeByte((byte) (o.getBoolean() ? 1 : 0));
                    break;
                case LUA_TNUMFLT:
                    writeNumber(o.getFLT());
                    break;
                case LUA_TNUMINT:
                    writeInteger(o.getINT());
                    break;
                case LUA_TSHRSTR:
                case LUA_TLNGSTR:
                    writeString(o);
                    break;
                default:
//                    throw new RuntimeException("无效的常量类型");
                    break;
            }
        }
    }

    private void writeProtos(Proto f) throws IOException {
        int sizep = f.sizep();
        writeInt(sizep);
        for (int i = 0; i < sizep; i++) {
            writeFunction(f.p[i], f.source);
        }
    }

    private void writeUpvalues(Proto f) throws IOException {
        int sizeupvalues = f.sizeupvalues();
        writeInt(sizeupvalues);
        for (int i = 0; i < sizeupvalues; i++) {
            Upvaldesc uv = f.upvalues[i];
            writeByte((byte) (uv.instack ? 1 : 0));
            writeByte((byte) uv.idx);
        }
    }

    private void writeDebug(Proto f) throws IOException {
        if (strip) {
            writeInt(0);
            writeInt(0);
            writeInt(0);
        } else {
            int sizelineinfo = f.sizelineinfo();
            writeInt(sizelineinfo);
            for (int i = 0; i < sizelineinfo; i++) {
                writeInt(f.lineinfo[i]);
            }
            int sizelocvars = f.sizelocvars();
            writeInt(sizelocvars);
            for (int i = 0; i < sizelocvars; i++) {
                LocVar lv = f.locvars[i];
                writeString(lv.varname);
                writeInt(lv.startpc);
                writeInt(lv.endpc);
            }
            int sizeupvalues = f.sizeupvalues();
            writeInt(sizeupvalues);
            for (int i = 0; i < sizeupvalues; i++) {
                Upvaldesc uv = f.upvalues[i];
                writeString(uv.name);
            }
        }
    }

    private void writeFunction(Proto f, TValue source) throws IOException {
        if (strip || f.source == null || f.source.equals(source)) {
            writeString(NIL);
        } else {
            writeString(f.source);
        }
        writeInt(f.linedefined);
        writeInt(f.lastlinedefined);
        writeByte(f.numparams);
        writeByte(f.is_vararg);
        writeByte(f.maxstacksize);
        writeCode(f);
        writeConstants(f);
        writeUpvalues(f);
        writeProtos(f);
        writeDebug(f);
    }

    public byte[] dump() throws IOException {
//        try {
        stream = new ByteArrayOutputStream();
        writeHeader();
        writeByte((byte) (proto.sizeupvalues() & 0xFF));
        writeFunction(proto, NIL);
        byte[] ret = stream.toByteArray();
        stream.close();
        return ret;
//        } catch (Throwable e) {
//            StringBuilder sb = new StringBuilder();
//            sb.append(e.getMessage()).append("\n");
//            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
//                sb.append(stackTraceElement.toString()).append("\n");
//            }
//            throw new RuntimeException(sb.toString());
//        }
    }

    private void writeByte(byte b) {
        stream.write(b);
    }

    private void writeLiteral(String s) throws IOException {
        writeLiteral(s.getBytes());
    }

    private void writeLiteral(byte[] s) throws IOException {
        stream.write(s);
    }

    private void writeInt(int x) throws IOException {
        byte[] b = int2bytes(x);
        stream.write(b);
    }

    private void writeNumber(double x) throws IOException {
        byte[] b = FLT2bytes(x);
        stream.write(b);
    }

    private void writeInteger(long x) throws IOException {
        byte[] b = INT2bytes(x);
        stream.write(b);
    }

    private void writeString(TValue o) throws IOException {
        if (o == null || o == TValue.NIL || o.tt_ == LUA_TNIL) {
            writeByte((byte) 0);
            return;
        }
        byte[] bytes = o.getStringBytes();
        int size = bytes.length + 1;//加上尾0
        if (size < 0xFF) {
            writeByte((byte) size);
        } else {
            writeByte((byte) 0xFF);
            writeInt(size);
        }
        writeLiteral(bytes);
    }

    private void writeInstruction(Instruction i) throws IOException {
        int x = i.value();
        byte[] b = Instruction2bytes(x);
        stream.write(b);
    }

}
