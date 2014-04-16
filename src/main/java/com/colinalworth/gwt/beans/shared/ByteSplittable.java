package com.colinalworth.gwt.beans.shared;

import com.google.web.bindery.autobean.shared.Splittable;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ByteSplittable implements Splittable {
  private static final ByteBuffer TRUE = ByteBuffer.wrap("true".getBytes());
//  private static final ByteBuffer NULL = ByteBuffer.wrap("null".getBytes());

  private static final int STAY = 0x0;       //xx00 means same depth
  private static final int END = 0x1;        //xx01 means decr depth
  private static final int START_OBJ = 0x2;  //001x means incr depth
  private static final int START_ARRAY = START_OBJ;//hmm, bits aren't right, use 0x2 for now

  private static final int NULL = 0x0 << 2;   //000x could be null or obj or array
  private static final int STRING = 0x1 <<2;  //010x means string
  private static final int NUMBER = 0x2 << 2; //100x means number
  private static final int BOOLEAN = 0x3 << 2;//110x means boolean


  private static IntBuffer collectOffsets(ByteBuffer buffer) {
    IntBuffer offsets = IntBuffer.allocate(buffer.limit() / 2 + 1);

    int nextPos = STAY;
    int lastOffset;
    consumeWhitespace(buffer);
    while(buffer.hasRemaining()) {
      int position = buffer.position();
      byte token = buffer.get();
      switch (token) {
        case ':':
          //ignore
          //TODO treat each pair as a pair? nothing special?
          break;
        case '{':
          offsets.put(lastOffset = (position << 4) + (nextPos == END ? STAY : START_OBJ));
          consumeWhitespace(buffer);
          continue;
        case '[':
          offsets.put(lastOffset = (position << 4) + (nextPos == END ? STAY : START_ARRAY));
          consumeWhitespace(buffer);
          continue;
        case '}':
        case ']':
          consumeCommaAndWhitespace(buffer);
          nextPos = END;
          continue;
        case '"':
          //add 1 for open quote
          offsets.put(lastOffset = (position << 4) + nextPos + STRING);
          consumeString(buffer);
          break;
        case 'n':
          offsets.put(lastOffset = (position << 4) + nextPos + NULL);
          consume(buffer, "ull");
          break;
        case 't':
          offsets.put(lastOffset = (position << 4) + nextPos + BOOLEAN);
          consume(buffer, "rue");
          break;
        case 'f':
          offsets.put(lastOffset = (position << 4) + nextPos + BOOLEAN);
          consume(buffer, "alse");
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
          offsets.put(lastOffset = (position << 4) + nextPos + NUMBER);
          consumeNumber(buffer);
          break;
        default:
          assert false : "Unexpected " + Character.getName(token);
      }
      nextPos = STAY;
      consumeCommaOrColonAndWhitespace(buffer);
    }

    buffer.rewind();
    offsets.flip();
    offsets.get();
    return offsets;
  }

  private static void consumeWhitespace(ByteBuffer buffer) {
    do {
      buffer.mark();
    } while (buffer.hasRemaining() && Character.isWhitespace(buffer.get()));
    buffer.reset();
  }

  private static void consumeString(ByteBuffer buffer) {
    //TODO unicode wat?
    while (buffer.hasRemaining()) {
      byte current = buffer.get();
      switch (current) {
        case '"':
          return;
        case '\\':
//          if (!buffer.hasRemaining()) {
//            throw new IllegalStateException("can't end mid-string in an escape sequence");
//          }
          byte next = buffer.get();
          switch (next) {
            case '"':
            case '\\':
            case '/':
            case 'b':
            case 'f':
            case 'n':
            case 'r':
            case 't':
              continue;
            case 'u':
              byte[] next4 = new byte[4];
              buffer.get(next4);
              for (int i = 0; i < next4.length; i++) {
                if (!(next4[i] >= '0' && next4[i] <= '9') &&
                        !(next4[i] >='a' && next4[i] <= 'f'))
                  throw new IllegalStateException("Illegal unicode: " + Arrays.toString(next4));
              }
            default:
              throw new IllegalStateException("Illegal escape: \\" + Character.getName(next));

          }
      }
    }
    throw new IllegalStateException("can't end mid-string");

  }

  private static void consume(ByteBuffer buffer, String remaining) {
    buffer.mark();
    int offset = 0;
    while (buffer.hasRemaining() && offset < remaining.length()) {
      byte next = buffer.get();
      char c = remaining.charAt(offset++);
      if (next != c) {
        throw new IllegalStateException("Expected " + c + ", saw " + ((char)next) + " at " + (buffer.position() - 1));
      }
    }
  }

  private static void consumeNumber(ByteBuffer buffer) {
    buffer.mark();
    byte next = buffer.get(buffer.position() - 1);//zeroth digit, 0-9 or -
    if (next == '-') {
      if (!buffer.hasRemaining() || !Character.isDigit(next = buffer.get())) {
        throw new IllegalStateException("'-' is not a legal number");
      }
      //next has been advanced, and is 0-9, we're legal, mark it
      buffer.mark();
    }
    if (next != '0') {
      //we must be 1-9 at this state, since we consumed '-' above
//      if (!Character.isDigit(next)) {
//        //next isn't a number, exit
//        return;
//      }
      //going into this loop next is 1-9
      while (buffer.hasRemaining() && Character.isDigit(next)) {
        buffer.mark();
        next = buffer.get();
      }
    } else {
      //0, so mark, then advance
      buffer.mark();
      next = buffer.get();
    }
    if (next == '.') {
//      buffer.mark();//not a legal stopping point, no mark
      next = buffer.get();
      if (!Character.isDigit(next)) {
        //blow up, the '.' must be followed by a digit
        throw new IllegalStateException("'.' must be followed by a digit");
      }
      while (Character.isDigit(next)) {
        buffer.mark();
        next = buffer.get();
      }
    }

    if (next == 'e' || next == 'E') {
      next = buffer.get();
      if (next == '+' || next == '-') {
        next = buffer.get();
      }
      if (!Character.isDigit(next)) {
        //blow up, (e|E)(+|-|) must be followed by a digit
        throw new IllegalStateException("'e' must be followed by a digit");
      }

      while (Character.isDigit(next)) {
        buffer.mark();
        next = buffer.get();
      }
    }
    buffer.reset();


  }

  private static void consumeCommaAndWhitespace(ByteBuffer buffer) {
    consumeWhitespace(buffer);
    if (buffer.hasRemaining()) {
      byte peek = buffer.get(buffer.position());
      if (peek == ',') {
        buffer.get();
        consumeWhitespace(buffer);
      }
    }
    consumeWhitespace(buffer);
  }

  private static void consumeCommaOrColonAndWhitespace(ByteBuffer buffer) {
    consumeWhitespace(buffer);
    if (buffer.hasRemaining()) {
      byte peek = buffer.get(buffer.position());
      if (peek == ':' || peek == ',') {
        buffer.get();
        consumeWhitespace(buffer);
      }
    }
  }


  private final ByteBuffer buffer;
  private final IntBuffer offsets;
  private final Map<String, Object> reified = new HashMap<String, Object>();

  public ByteSplittable(ByteBuffer buffer) {
    this(buffer, collectOffsets(buffer));
  }

  public ByteSplittable(ByteBuffer buffer, IntBuffer offsets) {
    this.buffer = buffer;
    this.offsets = offsets;

    //ensure both buffers have their marker defined at their current position
    buffer.mark();
    offsets.mark();
  }

  @Override
  public boolean asBoolean() {
    return matches(buffer, getFirstIndex(), "true", false);
  }

  @Override
  public double asNumber() {
//    ByteBuffer copy = buffer.duplicate();
    int start = getFirstIndex();
    buffer.position(start + 1);//assume already consumed the first one
    consumeNumber(buffer);
    int end = buffer.position();

    byte[] bytes = new byte[end - start];
    buffer.position(start);
    buffer.get(bytes);
    //consume moves the marker, move it back and position back to 0
    buffer.rewind().mark();

    return Double.parseDouble(new String(bytes));
  }

  @Override
  public void assign(Splittable parent, int index) {
    //TODO writing...
  }

  @Override
  public void assign(Splittable parent, String propertyName) {
    //TODO writing...
  }

  @Override
  public String asString() {
    int start = getFirstIndex();
    buffer.position(start + 1);//assume already consumed the first one
    consumeString(buffer);
    int end = buffer.position() - 2;//ignore both quotes

    byte[] bytes = new byte[end - start];
    buffer.position(start + 1);//ignore leading quote
    buffer.get(bytes);
    //consume moves the marker, move it back and position back to 0
    buffer.rewind().mark();

    return new String(bytes);
  }

  @Override
  public Splittable deepCopy() {
    return new ByteSplittable(buffer.duplicate(), offsets.duplicate());
  }

  @Override
  public Splittable get(int index) {
    int depth = 0;
    int i = 0;
    while (offsets.hasRemaining()) {
      int next = offsets.get();
      //check if we are ending the previous object, and adjust depth
      if ((next & END) == END) {
        depth--;
        if (depth < 0) {
          offsets.reset();
          return null;
        }
      }
      //if we're at depth 0, see if we've got the right index
      if (depth == 0) {
        if (i == index) {
          break;
        }
        i++;
      }
      //finally, if we are the start to a new layer, increase depth
      if ((next & START_OBJ) == START_OBJ || (next & START_ARRAY) == START_ARRAY) {
        depth++;
      }
    }
    IntBuffer newOffsets = offsets.duplicate();
    newOffsets.position(offsets.position());
    newOffsets.mark();

    offsets.reset();
    buffer.reset();
    return new ByteSplittable(buffer, newOffsets);
  }

  @Override
  public Splittable get(String key) {
    int depth = 0;
    int i = 0;
    while (offsets.hasRemaining()) {
      int next = offsets.get();
      if ((next & START_OBJ) == START_OBJ || (next & START_ARRAY) == START_ARRAY) {
        depth++;
      } else if ((next & END) == END) {
        depth--;
      }
      if (depth == 0) {

        //even numbered entries are keys
        if (i % 2 == 0 && (next & STRING) == STRING && matches(buffer, (next >> 4) + 1, key, true)) {
          //advance one more to the value
          offsets.get();
          break;
        }
        i++;
      } else if (depth < 1) {
        offsets.reset();
        return null;
      }
    }
    IntBuffer newOffsets = offsets.duplicate();
    newOffsets.position(offsets.position());
    newOffsets.mark();

    offsets.reset();
    buffer.reset();
    return new ByteSplittable(buffer, newOffsets);
  }

  private boolean matches(ByteBuffer buffer, int index, String target, boolean endsWithQuote) {
    if (buffer.limit() < index + target.length() + 1) {//1 for the close quote
      return false;
    }
    int i;
    for (i = 0; i < target.length(); i++) {
      if (buffer.get(i + index) != target.charAt(i)) {
        return false;
      }
    }
    return !endsWithQuote || buffer.get(i + index) == '"';//must end in close quote
  }

  public ByteBuffer getByteBufferPayload() {
    return buffer;
  }

  @Deprecated
  @Override
  public String getPayload() {
    return getByteBufferPayload().toString();
  }

  @Override
  public List<String> getPropertyKeys() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Object getReified(String key) {
    return reified.get(key);
  }

  @Override
  public boolean isBoolean() {
    return buffer.get(getFirstIndex()) == 't' || buffer.get(getFirstIndex()) == 'f';
  }

  @Override
  public boolean isIndexed() {
    return buffer.get(getFirstIndex()) == '[';
  }

  @Override
  public boolean isKeyed() {
    return buffer.get(getFirstIndex()) == '{';
  }

  @Override
  public boolean isNull(int index) {
    return get(index) == null;
  }

  @Override
  public boolean isNull(String key) {
    return get(key) == null;
  }

  @Override
  public boolean isNumber() {
    byte first = buffer.get(getFirstIndex());

    return (first >= '0' && first <= '9') || first == '-';
  }

  @Override
  public boolean isReified(String key) {
    return reified.containsKey(key);
  }

  @Override
  public boolean isString() {
    return buffer.get(getFirstIndex()) == '"';
  }

  @Override
  public boolean isUndefined(String key) {
    return false;
  }

  @Override
  public void setReified(String key, Object object) {
    reified.put(key, object);
  }

  @Override
  public void setSize(int i) {
    //TODO writing...
  }

  @Override
  public int size() {
    return 0;  //To change body of implemented methods use File | Settings | File Templates.
  }


  private int getFirstIndex() {
    return offsets.get(offsets.position() - 1) >> 4;
  }

}
