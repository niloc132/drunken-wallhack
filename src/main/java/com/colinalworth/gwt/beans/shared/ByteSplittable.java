package com.colinalworth.gwt.beans.shared;

import com.google.web.bindery.autobean.shared.Splittable;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ByteSplittable implements Splittable {

  private static final int TYPE_BITS = 3;
  private static final int MASK = 0x7;
  private static final int PARENT = 0x4;//also mask for primitive vs parent
  private static final int END_MASK = 0x1;

  private static final int NULL = 0x0;
  private static final int STRING = 0x1;
  private static final int NUMBER = 0x2;
  private static final int BOOLEAN = 0x3;

  private static final int OBJECT_START = PARENT;//with OBJ_MASK checks if is object
  private static final int OBJECT_END = PARENT + END_MASK;
  private static final int ARRAY_START = PARENT + 0x2;//with OBJ_MASK checks if is array
  private static final int ARRAY_END = PARENT + 0x2 + END_MASK;


  private static boolean isPrimitive(int offset) {
    return (offset & PARENT) == 0;
  }
  private static boolean isString(int offset) {
    return (offset & MASK) == STRING;
  }
  private static boolean isNumber(int offset) {
    return (offset & MASK) == NUMBER;
  }
  private static boolean isBoolean(int offset) {
    return (offset & MASK) == BOOLEAN;
  }

  private static boolean isObject(int offset) {
    return (offset & MASK) == OBJECT_START;
  }
  private static boolean isArray(int offset) {
    return (offset & MASK) == ARRAY_START;
  }

  private static class OffsetCollector {
    private final ByteBuffer buffer;
    private final IntBuffer offsets;

    private int offset;
    private byte peek;

    private OffsetCollector(ByteBuffer buffer) {
      this.buffer = buffer;
      this.offsets = IntBuffer.allocate(buffer.limit() / 2 + 1);
    }

    private IntBuffer getOffsets() {
      return offsets;
    }

    public void collect() {
      consumeWhitespace(buffer);

      collectValue();

      buffer.rewind();
      offsets.flip();
      offsets.get();
    }

    private void collectPairs() {
      final int lastOffset = offset;
      final int initialOffset = buffer.position() - 1;
      consumeWhitespace(buffer);
      peek = buffer.get(buffer.position());
      if (peek == '}') {
        buffer.get();
        consumeWhitespace(buffer);
        return;
      }
      while (buffer.hasRemaining()) {
        peek = buffer.get(buffer.position());


        switch (peek) {//TODO drop switch when we consume a string
          case '"':
            offsets.put(((buffer.position() - initialOffset) << TYPE_BITS) + STRING);
            buffer.get();
            consumeString(buffer);
            consumeColonAndWhitespace(buffer);
            offset = initialOffset;
            collectValue();//value - TODO consume type, and combine with offset above

            //either comma or }
            if (!consumeWhitespaceAndOptionalComma(buffer)) {
              peek = buffer.get();
              assert peek == '}' : Character.getName(peek);
              consumeWhitespace(buffer);
              return;
            }
            break;
          default:
            assert false : "invalid token " + Character.getName(peek);
        }
      }
    }
    private void collectItems() {
      final int lastOffset = offset;
      final int initialOffset = buffer.position() - 1;
      consumeWhitespace(buffer);
      peek = buffer.get(buffer.position());
      if (peek == ']') {
        buffer.get();
        consumeWhitespace(buffer);
        return;
      }
      while (buffer.hasRemaining()) {
        offset = initialOffset;
        collectValue();
        if (!consumeWhitespaceAndOptionalComma(buffer)) {
          //whitespace consumed, failed to find comma, must be ]
          peek = buffer.get();
          assert peek == ']' : Character.getName(peek);
          consumeWhitespace(buffer);
          return;
        }
      }
    }

    private void collectValue() {
      final int position = (buffer.position() - offset) << TYPE_BITS;
      int lastOffset, lastPosition;
      switch (buffer.get()) {
        case '{':
          offsets.put(position + OBJECT_START);
          lastPosition = offsets.position();
          offsets.put(0);
          collectPairs();
          offsets.put(lastPosition, offsets.position());
          return;
        case '[':
          offsets.put(position + ARRAY_START);
          lastPosition = offsets.position();
          offsets.put(0);
          collectItems();
          offsets.put(lastPosition, offsets.position());
          return;
        case '"':
          offsets.put(position + STRING);
          consumeString(buffer);
          break;
        case 'n':
          offsets.put(position + NULL);
          consume(buffer, "ull");
          break;
        case 't':
          offsets.put(position + BOOLEAN);
          consume(buffer, "rue");
          break;
        case 'f':
          offsets.put(position + BOOLEAN);
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
          offsets.put(position + NUMBER);
          consumeNumber(buffer);
          break;
        default:
          assert false : "Unexpected " + Character.getName(buffer.get(buffer.position() - 1));
      }
      consumeWhitespace(buffer);
    }

  }

  @SuppressWarnings("PointlessArithmeticExpression")
  private static IntBuffer collectOffsets(ByteBuffer buffer) {
    OffsetCollector collector = new OffsetCollector(buffer);
    collector.collect();
    return collector.getOffsets();
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

  private static boolean consumeWhitespaceAndOptionalComma(ByteBuffer buffer) {
    consumeWhitespace(buffer);
    if (buffer.hasRemaining()) {
      byte peek = buffer.get(buffer.position());
      if (peek == ',') {
        buffer.get();
        consumeWhitespace(buffer);
        return true;
      }
    }
    return false;
  }

  private static void consumeColonAndWhitespace(ByteBuffer buffer) {
    consumeWhitespace(buffer);
    consume(buffer, ":");
    consumeWhitespace(buffer);
  }


  private final ByteBuffer buffer;
  private final IntBuffer offsets;
  private final Map<String, Object> reified = new HashMap<>();

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
    buffer.position(start + 1);//consumeNumber assumes already consumed the first one
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
    buffer.position(start + 1);//consume assumes we already consumed the open quote
    consumeString(buffer);
    int end = buffer.position() - 2;//ignore both quotes

    byte[] bytes = new byte[end - start];
    buffer.position(start + 1);//ignore leading quote
    buffer.get(bytes);
    //consume moves the marker, move it back and position back to 0
    buffer.position(start).mark();

    return new String(bytes);
  }

  @Override
  public Splittable deepCopy() {
    return new ByteSplittable(buffer.duplicate(), offsets.duplicate());
  }

  @Override
  public Splittable get(int index) {
    //skip end index
    offsets.get();
    int i = 0;
    while (offsets.hasRemaining()) {
      int next = offsets.get();
      int endOfCurrent = -1;
      if (!isPrimitive(next)) {
        endOfCurrent = offsets.get(offsets.position());
      }
      if (i == index) {
        //we've found the right index
        break;
      }
      i++;

      if (!isPrimitive(next)) {
        //if we just finished a non-primitive, fast forward ahead to the end of that
        offsets.position(endOfCurrent);
      }
    }
    IntBuffer newOffsets = offsets.duplicate();
    // if we're looking at a parent, consume one more to skip the end position
    newOffsets.position(offsets.position());
    newOffsets.mark();

    ByteBuffer newBuffer = buffer.duplicate();
    newBuffer.position(buffer.position() + (offsets.get(offsets.position() - 1) >> TYPE_BITS));
    newBuffer.mark();

    offsets.reset();
    buffer.reset();
    return new ByteSplittable(newBuffer, newOffsets);
  }

  @Override
  public Splittable get(String key) {
    //skip end index
    offsets.get();
    while (offsets.hasRemaining()) {
      int keyOffset = offsets.get();
      int endOfCurrent = -1;
      if (!isPrimitive(keyOffset)) {
        endOfCurrent = offsets.get(offsets.position());
      }

      //even numbered entries are keys
      if (isString(keyOffset) && matches(buffer, (keyOffset >> TYPE_BITS) + buffer.position() + 1, key, true)) {
        //advance one more to the value
        offsets.get();
        break;
      }

      if (!isPrimitive(keyOffset)) {
        //if we just finished a non-primitive, fast forward ahead to the end of that
        offsets.position(endOfCurrent);
      }
    }
    IntBuffer newOffsets = offsets.duplicate();
    newOffsets.position(offsets.position());
    newOffsets.mark();

    ByteBuffer newBuffer = buffer.duplicate();
    newBuffer.position(buffer.position() + (offsets.get(offsets.position() - 1) >> TYPE_BITS));
    newBuffer.mark();

    offsets.reset();
    buffer.reset();
    return new ByteSplittable(newBuffer, newOffsets);
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
    return isBoolean(getFirstTypeDetails());
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
    return isNumber(getFirstTypeDetails());
  }

  @Override
  public boolean isReified(String key) {
    return reified.containsKey(key);
  }

  @Override
  public boolean isString() {
    return isString(getFirstTypeDetails());
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
    return buffer.position();
  }

  private int getFirstTypeDetails() {
    return offsets.get(offsets.position() - 1);
  }

}
