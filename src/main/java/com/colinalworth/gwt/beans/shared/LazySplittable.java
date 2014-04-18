package com.colinalworth.gwt.beans.shared;


import com.google.web.bindery.autobean.shared.Splittable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

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

public class LazySplittable implements Splittable {
    IntBuffer out;
    private ByteBuffer in;
    private boolean topLevel;

    public LazySplittable(ByteBuffer in, boolean topLevel) {
        this.topLevel = topLevel;
        if (topLevel) {
            System.err.println("<<" + UTF_8.decode(in.duplicate()));
            while (in.hasRemaining() && '{' != in.get()) ;
        }
        this.in = in;
        out = IntBuffer.allocate(in.limit() / 2);
        encode(true);
    }

    public LazySplittable(ByteBuffer in, IntBuffer out) {
        this.out = out;
        this.in = in;
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
        return String.valueOf(UTF_8.decode((ByteBuffer) in.duplicate().rewind()));
    }

    @Override
    public Splittable deepCopy() {
        return null;
    }

    @Override
    public Splittable get(int i) {
        return null;
    }

    public Splittable get(String key) {
        out.rewind();
        byte[] bytes = key.getBytes(UTF_8);

        searching:
        while (out.hasRemaining()) {

            int i = out.get();

            int i1 = i >> 31 & 1;
            Encoding e = Encoding.values()[i1];
            switch (e) {
                case depth:
                    break;
                case token:
                    Token token = Token.values()[i >> 29 & 0b11];

                    switch (token) {
                        case quoted:
                            int offset = (i << 0b11) >> 0b11;
                            ByteBuffer byteBuffer = ByteSplittable.consumeString((ByteBuffer) in.position(offset), 3);
                            if (byteBuffer != null) {
                                for (int j = 0; j < bytes.length; j++)
                                    if (bytes[j] != in.get(offset + j))
                                        continue searching;
                                //matched
                                return reify();
                            }
                        case numeric:
                            break;
                        case symbolic:
                            break;
                    }
            }
        }


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
        return topLevel;
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

    private Splittable reify() {

        if (out.hasRemaining()) {

            int i = out.get();

            Encoding e = Encoding.values()[(i >> 31) & 0b1];
            switch (e) {
                case depth:
                    Depth depth = Depth.values()[(i) >> 30 & 0b1];
                    switch (depth) {
                        case down:
                            Assignment assignment = Assignment.values()[(i) >> 29 & 0b1];
                            int seekTo = (i << 0b111) >> 0b111;
                            int i1 = out.get();
                            int newLimit = (i1 << 2) >> 2;//can this be anything other than a close?

                            final ByteBuffer newBuff = ((ByteBuffer) in.position(seekTo)).duplicate();

                            switch (assignment) {
                                case sequence:
                                    return new ArraySplittable(newBuff);
                                case associative:
                                    return new KeyedSplittable(newBuff);
                            }
                    }
                    break;
                case token:
                    Token token = Token.values()[i >> 29 & 3];

                    int newLimit = out.hasRemaining() ? out.limit() : (out.get() << 4) >> 4;//can this be anything other than a close?

                    switch (token) {
                        case quoted:
                            return new QuotedSplittable();

                        case numeric:
                            return new NumericSplittable();
                        case symbolic:
                            return new SymbolicSplittable();

                    }
            }
        }
        return null;
    }

    enum Encoding {depth, token}

    enum Depth {
        up,
        down,
    }

    enum Assignment {
        sequence, associative,
    }

    enum Token {
        quoted,
        numeric,
        symbolic
    }

    enum interest {keys, resume}

    public static void main(String[] args) throws IOException {
        ByteBuffer wrap = ByteBuffer.wrap(Files.readAllBytes(Paths.get(args[0])));
        System.err.println("");
        while (wrap.hasRemaining() && '{' != wrap.get()) ;
        LazySplittable byteSplittable2 = new LazySplittable(wrap, true);


    }

    void encode(boolean care) {
        byte b = 0;
        while (in.hasRemaining()) {
            while (in.hasRemaining() && Character.isWhitespace(b = in.get())) ;
            Encoding e = null;
            Depth d = null;
            Assignment a = null;
            Token t = null;
            int i = 0;
            switch (b) {
                case '{':
                    if (care) {
                        out.put((e = Encoding.depth).ordinal() << 31 | (d = Depth.down).ordinal() << 30 | (a = Assignment.associative).ordinal() << 29 | (i = (in.position() - 1)));
                    }
                    encode(false);
                    break;
                case '[':
                    if (care)
                        out.put((e = Encoding.depth).ordinal() << 31 | (d = Depth.down).ordinal() << 30 | (a = Assignment.sequence).ordinal() << 29 | (i = (in.position() - 1)));
                    encode(false);
                    break;
                case ']':
                case '}':

                    int i1 = (e = Encoding.depth).ordinal() << 31 | (d = Depth.up).ordinal() << 30 | (i = in.position() - 1);
                    if (care) {
                        out.put(i1);

                        break;
                    } else {
                        debug(care, e, d, a, t, i1);
                        return;
                    }
                case '"':
                    if (care)
                        out.put((e = Encoding.token).ordinal() << 31 | (t = Token.quoted).ordinal() << 30 | (i = in.position()));
                    ByteSplittable.consumeString(in);
                    break;
                default:
                    if (care)
                        switch (b) {
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
                                out.put((e = Encoding.token).ordinal() << 31 | (t = Token.numeric).ordinal() << 30 | (i = in.position() - 1));
                                ByteSplittable.consumeNumber(in);
                                break;

                            case 'n':
                            case 't':
                            case 'f':
                                out.put((e = Encoding.token).ordinal() << 31 | (t = Token.symbolic).ordinal() << 30 | (i = in.position() - 1));
                                while (in.hasRemaining() && Character.isAlphabetic(b = in.get())) ;
                            default:
                                break;
                        }
                    break;
            }
            debug(care, e, d, a, t, i);
        }
        if (care) out.flip();
    }

    private void debug(boolean care, Encoding e, Depth d, Assignment a, Token t, int i) {
        if (care) System.err.println(">>" +
                        (e != null ? "e:" + e + ":" : "") +
                        (d != null ? "d:" + d + ":" : "") +
                        (a != null ? "a:" + a + ":" : "") +
                        (t != null ? "t:" + t + ":" : "") +
                        i
        );
    }

    private static class KeyedSplittable extends LazySplittable {
        public KeyedSplittable(ByteBuffer newBuff) {
            super(newBuff, false);
        }

        @Override
        public boolean isKeyed() {
            return true;
        }
    }

    class QuotedSplittable extends LazySplittable {

        private String string = String.valueOf(UTF_8.decode(in));

        public QuotedSplittable() {
            super((ByteBuffer) ByteSplittable.consumeString(LazySplittable.this.in.duplicate()), false);
        }

        @Override
        public String asString() {
            return string;
        }

        public boolean isString() {
            return true;
        }
    }

    class NumericSplittable extends LazySplittable {

        final private Double aDouble = Double.valueOf(String.valueOf(UTF_8.decode((ByteBuffer) in.rewind())));

        public NumericSplittable() {
            super((ByteBuffer) ByteSplittable.consumeNumber(LazySplittable.this.in.duplicate()), false);
        }

        @Override
        public boolean isNumber() {
            return true;
        }

        @Override
        public double asNumber() {
            return aDouble;
        }

    }

    class SymbolicSplittable extends LazySplittable {
        public SymbolicSplittable() {
            super((ByteBuffer) LazySplittable.this.in.duplicate(), false);
        }

        @Override
        public boolean isUndefined(String s) {
            return 'n' == in.get(0);
        }

        @Override
        public boolean asBoolean() {
            return 't' == in.get(0);
        }

        @Override
        public boolean isBoolean() {
            return 'n' != in.get(0);
        }
    }

    class ArraySplittable extends LazySplittable {
        public ArraySplittable(ByteBuffer newBuff) {
            super(newBuff, false);
        }

        @Override
        public Splittable get(int i) {
            LazySplittable.this.in.position(((IntBuffer) out.position(i)).get((i << 4) >> 4));
            return LazySplittable.this.reify();
        }

        @Override
        public boolean isIndexed() {
            return true;
        }
    }
}



