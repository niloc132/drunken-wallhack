package com.colinalworth.gwt.beans.shared;


import com.google.web.bindery.autobean.shared.Splittable;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;
/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

public class LazySplittable extends DummySplittable {
    private final ByteBuffer in;

    private final boolean isKeyed;
    private IntBuffer out;

    private LazySplittable(ByteBuffer json1) {
        this(json1, false, false);
    }

    private LazySplittable(ByteBuffer in, boolean topLevel, boolean isKeyed) {
        this.isKeyed = isKeyed;
        this.in = in;
        out = IntBuffer.allocate(in.remaining() / 2);
        encode(true, isKeyed);
        out.flip();
        out = IntBuffer.allocate(out.remaining()).put(out);
    }

    public static Splittable create(ByteBuffer src) {
        while(src.hasRemaining()&&Character.isWhitespace(((ByteBuffer) src.mark()).get()));
        return createSplittable(src,((ByteBuffer) src.reset()).get());
    }
    public static Splittable createSplittable(ByteBuffer in1, byte b) {
        char x = (char) b;
        switch (b) {
            case '{':
                return new LazySplittable(in1, false, true);
            case '[':
                return new LazySplittable(in1, false, false);
            case '"':
                return parseString(in1);
            case '-':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return parseNum((ByteBuffer) in1.reset());
            case 't':
            case 'f':
            case 'n':
                return parseBool((ByteBuffer) in1.reset());
            default:
                break;
        }
        return null;
    }


    private static Splittable parseString(final ByteBuffer in1) {

        ByteBuffer slice = in1.slice();
        ByteSplittable.consumeString(slice);

        ByteBuffer flip = (ByteBuffer) slice.flip();
        if (flip.limit() > 1) {
            if ('"' == flip.get(flip.limit() - 1)) {
                if ('\\' != flip.get(flip.limit() - 2)) {
                    flip.limit(flip.limit() - 1);
                }
            }
        }

        final String decode = String.valueOf(UTF_8.decode((ByteBuffer) flip));
        LazySplittable lazySplittable = new LazySplittable(in1, false, false) {

            @Override
            public String asString() {

                return decode;
            }

            @Override
            public boolean isString() {
                return true;
            }

        };
        return lazySplittable;
    }

    static Splittable parseBool(final ByteBuffer in1) {
        final boolean aBoolean, isBoolean;
        byte b = ((ByteBuffer) in1.reset()).get();
        aBoolean = 't' == b;
        isBoolean = 'n' != b;
        return new DummySplittable() {
            @Override
            public boolean asBoolean() {
                return aBoolean;
            }

            @Override
            public boolean isBoolean() {
                return isBoolean;
            }
        };
    }

    private static Splittable parseNum(final ByteBuffer in1) {

        final double aDouble = Double.parseDouble(UTF_8.decode((ByteBuffer) consumeNumber(in1.slice()).flip()).toString());

        return new DummySplittable() {
            @Override
            public boolean isNumber() {
                return true;
            }

            @Override
            public double asNumber() {
                return aDouble;
            }
        };
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
                    esign=true;
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

    @Override
    public Splittable get(int i) {
        if (!isIndexed()) throw new RuntimeException("json object not indexed");
        in.position(out.get(i));
        byte b;
        if (in.position() > 0) {
            if ('"' == (b = in.get(in.position() - 1)))
                return parseString(in);
        }
        b = ((ByteBuffer) in.mark()).get();
        return createSplittable(in, b);
    }

    @Override
    public Splittable get(String s) {
        out.rewind();
        if (!isKeyed) throw new RuntimeException("must have int keys");
        byte[] bytes = s.getBytes();
        while (out.hasRemaining()) {
            int c = 0;
            int i = out.get();
            byte b = -1;
            in.position(i);
            while (in.hasRemaining() && (b = in.get()) == bytes[c++] && c < bytes.length) ;
            if (c < bytes.length || !in.hasRemaining() || in.get() != '"') continue;

            while (in.hasRemaining()) {
                b = ((ByteBuffer) in.mark()).get();
                Splittable splittable = createSplittable(in, b);
                if (null != splittable) return splittable;
            }

        }
        return null;/* new LazySplittable(ByteBuffer.allocate(0),false,true);*/
    }

    @Override
    public boolean isIndexed() {
        return !isKeyed && !isString() && !isBoolean() && !isNumber();
    }

    @Override
    public boolean isKeyed() {
        return isKeyed;
    }

    void encode(boolean keep, boolean isKeyed) {

        boolean seekable = true;
        byte b = -1;
        searching:
        while (in.hasRemaining()) {
            while (in.hasRemaining() && Character.isWhitespace(b = ((ByteBuffer) in.mark()).get())) ;
            char x = (char) b;
            switch (b) {
                case '{':
                    if (keep && seekable) {
                        out.put(in.position() - 1);
                    }
                    encode(false, true);
                    break;
                case '[':
                    if (keep && seekable) {
                        out.put(in.position() - 1);
                    }
                    encode(false, false);
                    break;

                case '"':
                    if (keep && seekable) {
                        out.put(in.position());
                    }
                    ByteSplittable.consumeString((ByteBuffer) in);
                    if (keep && seekable && isKeyed) {
                        seekable = false;
                        continue searching;
                    }
                    break;
                case '-':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    if (keep && seekable) out.put(in.position() - 1);

                    consumeNumber(in);
                    break;

                case 't':
                case 'f':
                case 'n':
                    if (keep && seekable) {
                        out.put(in.position() - 1);
                    }
                    while (in.hasRemaining() && Character.isAlphabetic(in.get())) ;
                    break;

                case '}':
                case ']':
                    return;
                default:
                    continue searching;

            }
            seekable = true;
        }
    }

    public static enum Encoding {keyed, indexed, quoted, numeric, symbolic}
}
