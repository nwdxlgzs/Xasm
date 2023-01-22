package com.nwdxlgzs.xasm;

import java.io.Serializable;

public class LocVar implements Serializable {
    public TValue varname = TValue.NIL;
    public int startpc = 0;
    public int endpc = 0;

    public LocVar() {
    }

    @Override
    public String toString() {
        return "LocVar{" + "\n" +
                "varname=" + varname + "\n" +
                ", startpc=" + startpc + "\n" +
                ", endpc=" + endpc + "\n" +
                '}';
    }
}
