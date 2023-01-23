package com.nwdxlgzs.xasm;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class defines {
    public static byte[] LUA_SIGNATURE = {0x1b, 0x4c, 0x75, 0x61};
    public static byte[] LUAC_VERSION = {0x53};
    public static byte[] LUAC_FORMAT = {0};
    public static byte[] LUAC_DATA = {0x19, (byte) 0x93, 0x0d, 0x0a, 0x1a, 0x0a};
    public static byte[] LUAC_INT = {0x78, 0x56, 0, 0, 0, 0, 0, 0};/* 0x5678 */
    public static byte[] LUAC_NUM = {0, 0, 0, 0, 0, 0x28, 0x77, 0x40};/* 370.5 */

    public static int sizeof_int = 4;
    public static int sizeof_size_t = 4;//nirenr魔改：unsigned int
    public static int sizeof_Instruction = 4;
    public static int sizeof_lua_Integer = 8;
    public static int sizeof_lua_Number = 8;

    public static int SIZE_C = 9;
    public static int SIZE_B = 9;
    public static int SIZE_Bx = SIZE_C + SIZE_B;
    public static int SIZE_A = 8;
    public static int SIZE_Ax = SIZE_C + SIZE_B + SIZE_A;
    public static int SIZE_OP = 6;
    public static int POS_OP = 0;
    public static int POS_A = POS_OP + SIZE_OP;
    public static int POS_C = POS_A + SIZE_A;
    public static int POS_B = POS_C + SIZE_C;
    public static int POS_Bx = POS_C;
    public static int POS_Ax = POS_A;
    public static int MAXARG_Bx = (1 << SIZE_Bx) - 1;
    public static int MAXARG_sBx = MAXARG_Bx >> 1;
    public static int MAXARG_Ax = (1 << SIZE_Ax) - 1;
    public static int MAXARG_A = (1 << SIZE_A) - 1;
    public static int MAXARG_B = (1 << SIZE_B) - 1;
    public static int MAXARG_C = (1 << SIZE_C) - 1;

    public static int MASK1(int n, int p) {
        return ((~((~(int) 0) << (n))) << (p));
    }

    public static int MASK0(int n, int p) {
        return (~MASK1(n, p));
    }

    public static int GET_OPCODE(int i) {
        return ((i >> POS_OP) & MASK1(SIZE_OP, 0));
    }

    public static int SET_OPCODE(int i, int o) {
        return ((i & MASK0(SIZE_OP, POS_OP)) | ((o << POS_OP) & MASK1(SIZE_OP, POS_OP)));
    }

    public static int getarg(int i, int pos, int size) {
        return ((i >> pos) & MASK1(size, 0));
    }

    public static int setarg(int i, int v, int pos, int size) {
        return ((i & MASK0(size, pos)) | ((v << pos) & MASK1(size, pos)));
    }

    public static int GETARG_A(int i) {
        return getarg(i, POS_A, SIZE_A);
    }

    public static int SETARG_A(int i, int v) {
        return setarg(i, v, POS_A, SIZE_A);
    }

    public static int GETARG_B(int i) {
        return getarg(i, POS_B, SIZE_B);
    }

    public static int SETARG_B(int i, int v) {
        return setarg(i, v, POS_B, SIZE_B);
    }

    public static int GETARG_C(int i) {
        return getarg(i, POS_C, SIZE_C);
    }

    public static int SETARG_C(int i, int v) {
        return setarg(i, v, POS_C, SIZE_C);
    }

    public static int GETARG_Bx(int i) {
        return getarg(i, POS_Bx, SIZE_Bx);
    }

    public static int SETARG_Bx(int i, int v) {
        return setarg(i, v, POS_Bx, SIZE_Bx);
    }

    public static int GETARG_sBx(int i) {
        return (GETARG_Bx(i) - MAXARG_sBx);
    }

    public static int SETARG_sBx(int i, int v) {
        return SETARG_Bx(i, v + MAXARG_sBx);
    }

    public static int GETARG_Ax(int i) {
        return getarg(i, POS_Ax, SIZE_Ax);
    }

    public static int SETARG_Ax(int i, int v) {
        return setarg(i, v, POS_Ax, SIZE_Ax);
    }

    public static int CREATE_ABC(int o, int a, int b, int c) {
        return (SET_OPCODE(0, o) | SETARG_A(0, a) | SETARG_B(0, b) | SETARG_C(0, c));
    }

    public static int CREATE_ABx(int o, int a, int bc) {
        return (SET_OPCODE(0, o) | SETARG_A(0, a) | SETARG_Bx(0, bc));
    }

    public static int CREATE_AsBx(int o, int a, int bc) {
        return (SET_OPCODE(0, o) | SETARG_A(0, a) | SETARG_sBx(0, bc));
    }

    public static int CREATE_Ax(int o, int a) {
        return (SET_OPCODE(0, o) | SETARG_Ax(0, a));
    }

    public static int BITRK = 1 << (SIZE_B - 1);

    public static boolean ISK(int x) {
        return (x & BITRK) != 0;
    }

    public static int INDEXK(long r) {
        return ((int) (r) & ~BITRK);
    }

    public static int MAXINDEXRK = BITRK - 1;

    public static int RKASK(int x) {
        return (x | BITRK);
    }

    public static int NO_REG = MAXARG_A;


    /**
     * 重新绑定指令结构的分配
     */
    public static void rebindInstructionStructure() {
        SIZE_Bx = SIZE_C + SIZE_B;
        SIZE_Ax = SIZE_C + SIZE_B + SIZE_A;
        POS_A = POS_OP + SIZE_OP;
        POS_C = POS_A + SIZE_A;
        POS_B = POS_C + SIZE_C;
        POS_Bx = POS_C;
        POS_Ax = POS_A;
        MAXARG_Bx = (1 << SIZE_Bx) - 1;
        MAXARG_sBx = MAXARG_Bx >> 1;
        MAXARG_Ax = (1 << SIZE_Ax) - 1;
        MAXARG_A = (1 << SIZE_A) - 1;
        MAXARG_B = (1 << SIZE_B) - 1;
        MAXARG_C = (1 << SIZE_C) - 1;
        BITRK = 1 << (SIZE_B - 1);
        MAXINDEXRK = BITRK - 1;
        NO_REG = MAXARG_A;
    }

    public static byte[] FLT2bytes(double n) {
        byte[] bytes = new byte[sizeof_lua_Number];
        long l = Double.doubleToRawLongBits(n);
        for (int i = 0; i < sizeof_lua_Number; i++) {
            bytes[i] = (byte) (l & 0xff);
            l >>= 8;
        }
        return bytes;
    }

    public static double bytes2FLT(byte[] bytes) {
        long l = 0;
        for (int i = 0; i < sizeof_lua_Number; i++) {
            l |= (long) (bytes[i] & 0xff) << (i * 8);
        }
        return Double.longBitsToDouble(l);
    }

    public static byte[] INT2bytes(long n) {
        byte[] bytes = new byte[sizeof_lua_Integer];
        for (int i = 0; i < sizeof_lua_Integer; i++) {
            bytes[i] = (byte) (n & 0xff);
            n >>= 8;
        }
        return bytes;
    }

    public static long bytes2INT(byte[] bytes) {
        long l = 0;
        for (int i = 0; i < sizeof_lua_Integer; i++) {
            l |= (long) (bytes[i] & 0xff) << (i * 8);
        }
        return l;
    }

    public static byte[] int2bytes(long n) {
        byte[] bytes = new byte[sizeof_int];
        for (int i = 0; i < sizeof_int; i++) {
            bytes[i] = (byte) (n & 0xff);
            n >>= 8;
        }
        return bytes;
    }

    public static long bytes2int(byte[] bytes) {
        long l = 0;
        for (int i = 0; i < sizeof_int; i++) {
            l |= (long) (bytes[i] & 0xff) << (i * 8);
        }
        return l;
    }

    public static byte[] long2bytes(long n) {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (n & 0xff);
            n >>= 8;
        }
        return bytes;
    }

    public static long bytes2long(byte[] bytes) {
        long l = 0;
        for (int i = 0; i < 8; i++) {
            l |= (long) (bytes[i] & 0xff) << (i * 8);
        }
        return l;
    }

    public static byte[] Instruction2bytes(int i) {
        if (sizeof_Instruction == 4) {
            return int2bytes(i);
        } else {
            return long2bytes(i);
        }
    }

    public static long bytes2Instruction(byte[] bytes) {
        if (sizeof_Instruction == 4) {
            return (long) bytes2int(bytes);
        } else {
            return (long) bytes2long(bytes);
        }
    }

    public static int[] realloc(int[] old, int newsize) {
        if (old == null || newsize == 0) {
            return new int[newsize];
        }
        int[] newarr = new int[newsize];
        System.arraycopy(old, 0, newarr, 0, old.length);
        return newarr;
    }

    public static Integer[] realloc(Integer[] old, int newsize) {
        if (old == null || newsize == 0) {
            return new Integer[newsize];
        }
        Integer[] newarr = new Integer[newsize];
        System.arraycopy(old, 0, newarr, 0, old.length);
        return newarr;
    }

    public static long[] realloc(long[] old, int newsize) {
        if (old == null || newsize == 0) {
            return new long[newsize];
        }
        long[] newarr = new long[newsize];
        System.arraycopy(old, 0, newarr, 0, old.length);
        return newarr;
    }

    public static double[] realloc(double[] old, int newsize) {
        if (old == null || newsize == 0) {
            return new double[newsize];
        }
        double[] newarr = new double[newsize];
        System.arraycopy(old, 0, newarr, 0, old.length);
        return newarr;
    }

    public static byte[] realloc(byte[] old, int newsize) {
        if (old == null || newsize == 0) {
            return new byte[newsize];
        }
        byte[] newarr = new byte[newsize];
        System.arraycopy(old, 0, newarr, 0, old.length);
        return newarr;
    }

    public static char[] realloc(char[] old, int newsize) {
        if (old == null || newsize == 0) {
            return new char[newsize];
        }
        char[] newarr = new char[newsize];
        System.arraycopy(old, 0, newarr, 0, old.length);
        return newarr;
    }

    public static boolean[] realloc(boolean[] old, int newsize) {
        if (old == null || newsize == 0) {
            return new boolean[newsize];
        }
        boolean[] newarr = new boolean[newsize];
        System.arraycopy(old, 0, newarr, 0, old.length);
        return newarr;
    }

    public static short[] realloc(short[] old, int newsize) {
        if (old == null || newsize == 0) {
            return new short[newsize];
        }
        short[] newarr = new short[newsize];
        System.arraycopy(old, 0, newarr, 0, old.length);
        return newarr;
    }

    public static TValue[] realloc(TValue[] old, int newsize) {
        if (old == null || newsize == 0) {
            return new TValue[newsize];
        }
        TValue[] newarr = new TValue[newsize];
        System.arraycopy(old, 0, newarr, 0, old.length);
        return newarr;
    }

    public static Instruction[] realloc(Instruction[] old, int newsize) {
        if (old == null || newsize == 0) {
            return new Instruction[newsize];
        }
        Instruction[] newarr = new Instruction[newsize];
        System.arraycopy(old, 0, newarr, 0, old.length);
        return newarr;
    }

    public static Proto[] realloc(Proto[] old, int newsize) {
        if (old == null || newsize == 0) {
            return new Proto[newsize];
        }
        Proto[] newarr = new Proto[newsize];
        System.arraycopy(old, 0, newarr, 0, old.length);
        return newarr;
    }

    public static LocVar[] realloc(LocVar[] old, int newsize) {
        if (old == null || newsize == 0) {
            return new LocVar[newsize];
        }
        LocVar[] newarr = new LocVar[newsize];
        System.arraycopy(old, 0, newarr, 0, old.length);
        return newarr;
    }

    public static Upvaldesc[] realloc(Upvaldesc[] old, int newsize) {
        if (old == null || newsize == 0) {
            return new Upvaldesc[newsize];
        }
        Upvaldesc[] newarr = new Upvaldesc[newsize];
        System.arraycopy(old, 0, newarr, 0, old.length);
        return newarr;
    }

    public static byte[] readFile(String path) throws IOException {
        FileInputStream fis = new FileInputStream(path);
        byte[] data = new byte[fis.available()];
        fis.read(data);
        fis.close();
        return data;
    }

    public static void writeFile(String path, byte[] data) throws IOException {
        FileOutputStream fos = new FileOutputStream(path);
        fos.write(data);
        fos.close();
    }

    public static String safeToVarString(Proto p, int i) {
        if (i < 0) {
            return "nil";
        }
        if (i >= p.sizek()) {
            return "k" + i;
        }
        return p.k[i].toVarString();
    }

    public static enum StringContentLevel {
        STR_CONTENT_STRING,
        STR_CONTENT_BUFFER,
    }

    public static StringContentLevel TStringCheck(byte[] str) {
        boolean passAllSymbolAnd09azAz = true;//是否全是符号
        int i;
        byte[] str2 = new byte[str.length];
        for (i = 0; i < str.length; i++) {
            if (str[i] < 32 || str[i] > 126) {
                passAllSymbolAnd09azAz = false;
                str2[i] = str[i];
            } else {
                str2[i] = (byte) 'a';//字符有问题，很奇怪
            }
        }
        if (passAllSymbolAnd09azAz) {
            return StringContentLevel.STR_CONTENT_STRING;
        }
        //测试UTF8和GBK编码，没通过那就认为是乱码
        CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
        try {
            decoder.decode(ByteBuffer.wrap(str2));
        } catch (CharacterCodingException e) {
            return StringContentLevel.STR_CONTENT_BUFFER;
        }
        decoder = Charset.forName("GBK").newDecoder();
        try {
            decoder.decode(ByteBuffer.wrap(str2));
        } catch (CharacterCodingException e) {
            return StringContentLevel.STR_CONTENT_BUFFER;
        }
        return StringContentLevel.STR_CONTENT_STRING;
    }
}
