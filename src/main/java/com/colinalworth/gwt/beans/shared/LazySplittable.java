/*
package com.colinalworth.gwt.beans.shared;

import com.google.web.bindery.autobean.shared.Splittable;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

*/
/**
 * Created by jim on 4/20/14.
 *//*

public class LazySplittable extends DummySplittable {
    private ByteBuffer in;

    private LazySplittable(ByteBuffer in) {
        this.in = in;
    }

    public static Splittable create(ByteBuffer src) {
        while (src.hasRemaining() && Character.isWhitespace(((ByteBuffer) src.mark()).get())) ;
        return createSplittable(src, ((ByteBuffer) src.reset()).get());
    }

    private static Splittable createSplittable(final ByteBuffer src, byte b) {
        switch (b) {
            case '{':
                return new LazySplittable(src) {
                    public ByteBuffer in = src.slice();

                    @Override
                    public boolean isKeyed() {
                        return true;
                    }

                    @Override
                    public Splittable get(String s) {

                        byte b;
                        while (in.hasRemaining()) {
                            b = in.get();
                            switch (b) {
                                case '"':
                                    if (strcmp(s.getBytes(StandardCharsets.UTF_8), src)) {
                                        return new LazySplittable(src);
                                    } //skip a whole value
                                    ShallowSplittable.create(src);
                                    break;
                            }
                        }
                        return null;
                    }
                };
            case '[':
                return new LazySplittable(src) {
                    public ByteBuffer in = src.slice();

                    @Override
                    public boolean isIndexed() {
                        return true;
                    }

                    @Override
                    public Splittable get(int i) {

                        while (in.hasRemaining()) {
                            ShallowSplittable s;
                            while(0<i--) {
                                while (src.hasRemaining() && Character.isWhitespace(((ByteBuffer) src.mark()).get())) ;
                                ShallowSplittable.createSplittable(src, ((ByteBuffer) src.reset()).get());
                            }
                        }
                        return null;
                    }
                };
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

}
*/
