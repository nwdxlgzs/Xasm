package com.nwdxlgzs.xasm;


import java.io.Serializable;

import static com.nwdxlgzs.xasm.defines.*;

public class Proto implements Serializable {
    public String XasmFuncName;//.function XX中的XX
    public byte numparams = 0;
    public byte is_vararg = 0;
    public byte maxstacksize = 0;
    public int linedefined = 0;
    public int lastlinedefined = 0;
    public TValue[] k = null;
    public Instruction[] code = null;
    public Proto[] p = null;
    public Integer[] lineinfo = null;
    public LocVar[] locvars = null;
    public Upvaldesc[] upvalues = null;
    public TValue source = TValue.NIL;

    public int sizeupvalues() {
        return upvalues == null ? 0 : upvalues.length;
    }

    public void sizeupvalues(int newsize) {
        upvalues = realloc(upvalues, newsize);
    }

    public int sizek() {
        return k == null ? 0 : k.length;
    }

    public void sizek(int newsize) {
        k = realloc(k, newsize);
    }

    public int sizecode() {
        return code == null ? 0 : code.length;
    }

    public void sizecode(int newsize) {
        code = realloc(code, newsize);
    }

    public int sizelineinfo() {
        return lineinfo == null ? 0 : lineinfo.length;
    }

    public void sizelineinfo(int newsize) {
        lineinfo = realloc(lineinfo, newsize);
    }

    public int sizep() {
        return p == null ? 0 : p.length;
    }

    public void sizep(int newsize) {
        p = realloc(p, newsize);
    }

    public int sizelocvars() {
        return locvars == null ? 0 : locvars.length;
    }

    public void sizelocvars(int newsize) {
        locvars = realloc(locvars, newsize);
    }

    public Proto() {
    }

    public Proto findSub(String path) {
        if (path == null || path.isEmpty() || "main".equals(path)) return this;
        if (path.startsWith("main/")) path = path.substring(5);
        if (path.startsWith("/")) path = path.substring(1);
        String[] split = path.split("/");
        Proto f = this;
        for (String s : split) {
            int index = Integer.parseInt(s);
            if (index < 0 || index >= f.sizep()) return null;
            f = f.p[index];
        }
        if (f == this) return null;
        return f;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Proto{" + "\n" + "numparams=").append(numparams).append("\n")
                .append(", is_vararg=").append(is_vararg).append("\n")
                .append(", maxstacksize=").append(maxstacksize).append("\n")
                .append(", sizeupvalues=").append(sizeupvalues()).append("\n")
                .append(", sizek=").append(sizek()).append("\n")
                .append(", sizecode=").append(sizecode()).append("\n")
                .append(", sizelineinfo=").append(sizelineinfo()).append("\n")
                .append(", sizep=").append(sizep()).append("\n")
                .append(", sizelocvars=").append(sizelocvars()).append("\n")
                .append(", linedefined=").append(linedefined).append("\n")
                .append(", lastlinedefined=").append(lastlinedefined).append("\n");
        sb.append(", k=");
        if (k != null) {
            sb.append("[" + "\n");
            for (TValue tValue : k) {
                sb.append(tValue).append(",\n");
            }
            sb.append("]" + "\n");
        } else {
            sb.append("null" + "\n");
        }
        sb.append(", code=");
        if (code != null) {
            sb.append("[" + "\n");
            for (Instruction instruction : code) {
                sb.append(instruction).append(",\n");
            }
            sb.append("]" + "\n");
        } else {
            sb.append("null" + "\n");
        }
        sb.append(", p=");
        if (p != null) {
            sb.append("[" + "\n");
            for (Proto proto : p) {
                sb.append(proto).append(",\n");
            }
            sb.append("]" + "\n");
        } else {
            sb.append("null" + "\n");
        }
        sb.append(", lineinfo=");
        if (lineinfo != null) {
            sb.append("[" + "\n");
            for (Integer integer : lineinfo) {
                sb.append(integer).append(",\n");
            }
            sb.append("]" + "\n");
        } else {
            sb.append("null" + "\n");
        }
        sb.append(", locvars=");
        if (locvars != null) {
            sb.append("[" + "\n");
            for (LocVar locVar : locvars) {
                sb.append(locVar).append(",\n");
            }
            sb.append("]" + "\n");
        } else {
            sb.append("null" + "\n");
        }
        sb.append(", upvalues=");
        if (upvalues != null) {
            sb.append("[" + "\n");
            for (Upvaldesc upvaldesc : upvalues) {
                sb.append(upvaldesc).append(",\n");
            }
            sb.append("]" + "\n");
        } else {
            sb.append("null" + "\n");
        }
        sb.append(", source=").append(source).append("\n")
                .append('}');
        return sb.toString();
    }
}
