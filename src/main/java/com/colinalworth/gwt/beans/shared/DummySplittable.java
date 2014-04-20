package com.colinalworth.gwt.beans.shared;

import com.google.web.bindery.autobean.shared.Splittable;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by jim on 4/19/14.
 */
public class DummySplittable implements Splittable {
    public static void consumeString(ByteBuffer buffer) {
        //TODO unicode wat?
        while (buffer.hasRemaining()) {
            byte current = buffer.get();
            switch (current) {
                case '"':
                    return;
                case '\\':
                    byte next = buffer.get();
                    switch (next) {
                        case 'u':
                            buffer.position(buffer.position() + 4);
                        default:
                    }
            }
        }
    }

    public static ByteBuffer consumeNumber(ByteBuffer slice) {
        boolean sign = false, digits1 = false, dot = false, digits2 = false, etoken = false, esign = false, edigits = false;
        byte b = ((ByteBuffer) slice.mark()).get();
        sign = b == '-' || b == '+';
        if (!sign) slice.reset();

        while (slice.hasRemaining()) {
            while (slice.hasRemaining() && Character.isDigit(b = ((ByteBuffer) slice.mark()).get())) ;
            char x = (char) b;
            switch (b) {
                case '.':
                    if (dot) throw new NumberFormatException("extra dot");
                    dot = true;
                case 'E':
                case 'e':
                    if (etoken/*||!(digits1||digits2)*/)
                        throw new NumberFormatException("missing digits or redundant exponent");
                    etoken = true;
                case '+':
                case '-':
                    if (esign /*|| !etoken || edigits*/) throw new NumberFormatException("bad exponent sign");
                    esign = true;
                default:
                    if (Character.isDigit(b)) {
//                        boolean x = !dot ? digits1 : !etoken ? digits2 : edigits |= true;
                        if (!etoken)
                            if (!dot)
                                digits1 = true;
                            else
                                digits2 = true;
                        else
                            edigits = true;
                    } else {
                        return (ByteBuffer) slice.reset();
                    }
            }
        }
        return null;
    }

    public static void consumeScope(ByteBuffer in) {

        byte b;
        int depth = 1;
        while (in.hasRemaining() && depth > 0) {
            b = in.get();
            switch (b) {
                case '[':
                case '{':
                    depth++;
                    break;
                case ']':
                case '}':
                    depth--;
                    break;
                case '"':
                    consumeString(in);
                    break;
            }
        }
    }

    /**
     * compares buffer to byte[];  assumes quote at end of string.  proceeds to quote on failure;
     *
     * @param bytes key to compare to
     * @param in1 buffer to consume
     * @return true on match.
     */
    static public  boolean strcmp(byte[] bytes, ByteBuffer in1) {
        int c = 0;
        while (in1.hasRemaining() && in1.get() == bytes[c++] && c < bytes.length) ;
        boolean b = in1.get() != '"';
        if(b)while(in1.hasRemaining() && '"' != in1.get());
        return !(c < bytes.length || !in1.hasRemaining() || b);
    }

    @Override
    public boolean asBoolean() {
        return false;
    }

    @Override
    public double asNumber() {
        return 0;
    }

    @Override
    public void assign(Splittable splittable, int i) {

    }

    @Override
    public void assign(Splittable splittable, String s) {

    }

    @Override
    public String asString() {
        return null;
    }

    @Override
    public Splittable deepCopy() {
        return null;
    }

    @Override
    public Splittable get(int i) {
        return null;
    }

    @Override
    public Splittable get(String s) {
        return null;
    }

    @Override
    public String getPayload() {
        return null;
    }

    @Override
    public List<String> getPropertyKeys() {
        return null;
    }

    @Override
    public Object getReified(String s) {
        return null;
    }

    @Override
    public boolean isBoolean() {
        return false;
    }

    @Override
    public boolean isIndexed() {
        return false;
    }

    @Override
    public boolean isKeyed() {
        return false;
    }

    @Override
    public boolean isNull(int i) {
        return false;
    }

    @Override
    public boolean isNull(String s) {
        return false;
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public boolean isReified(String s) {
        return false;
    }

    @Override
    public boolean isString() {
        return false;
    }

    @Override
    public boolean isUndefined(String s) {
        return false;
    }

    @Override
    public void setReified(String s, Object o) {

    }

    @Override
    public void setSize(int i) {

    }

    @Override
    public int size() {
        return 0;
    }
}
