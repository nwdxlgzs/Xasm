package com.nwdxlgzs.xasm;

import java.io.Serializable;

public class Upvaldesc implements Serializable {
    public TValue name = TValue.NIL;
    public boolean instack = true;
    public short idx = 0;//实际是byte，但是这傻逼signed，只好用更大范围的short了

    public Upvaldesc() {
    }

    @Override
    public String toString() {
        return "Upvaldesc{" + "\n" +
                "name=" + name + "\n" +
                ", instack=" + instack + "\n" +
                ", idx=" + idx + "\n" +
                '}';
    }
}
