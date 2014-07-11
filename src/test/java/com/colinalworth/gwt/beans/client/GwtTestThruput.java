package com.colinalworth.gwt.beans.client;

import playn.html.TypedArrayHelper;

import com.colinalworth.gwt.beans.shared.ByteSplittable;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.typedarrays.shared.ArrayBuffer;
import com.google.web.bindery.autobean.shared.Splittable;

import java.nio.ByteBuffer;

public class GwtTestThruput extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.colinalworth.gwt.beans.DrunkenWallhack";
  }

  private native ArrayBuffer makeTypedArray(String string) /*-{
    var length = 0, n = string.length;
    for(var i=0; i<n; i++) {
      var val = string.charCodeAt(i);
      if(val >= 128) {
        if(val >= 2048) {
          length += 3;
        } else {
          length += 2;
        }
      } else {
        length += 1;
      }
    }

    var typedArray = new $wnd.Uint8Array(length),
            j = 0;
    for(var i=0; i<n; i++) {
      var val = string.charCodeAt(i);
      if(val >= 128) {
        if(val >= 2048) {
          typedArray[j] = ((val >>> 6) | 192);
          typedArray[j+1] = (val & 63) | 128;
          j += 2;
        } else {
          typedArray[j] = (val >>> 12) | 224;
          typedArray[j+1] = ((val >>> 6) & 63) | 128;
          typedArray[j+2] = (val & 63) | 128;
          j += 3;
        }
      } else {
        typedArray[j++] = val;
      }
    }
      return typedArray.buffer;
  }-*/;

  private ByteBuffer makeByteBuffer(String string) {
    return TypedArrayHelper.wrap(makeTypedArray(string));
  }

  public void testFindByKey() {
    ByteSplittable split1 = getByteSplittable("{\"a\":1, \"ab\":2, \"abc\":3, \"z\":[]}");
    ByteSplittable split2 = getByteSplittable("{\"abc\":1, \"ab\":2, \"a\":3, \"z\":[]}");

    assert split1.get("a").asNumber() == 1;
    assert split1.get("ab").asNumber() == 2;
    assert split1.get("abc").asNumber() == 3;
    assert split1.get("z").isIndexed();
    assert split2.get("abc").asNumber() == 1;
    assert split2.get("ab").asNumber() == 2;
    assert split2.get("a").asNumber() == 3;
    assert split2.get("z").isIndexed();

    ByteSplittable split = getByteSplittable("{\"\":\"123\", \"a\":\"\"}");
    assert split.isKeyed();
    assert split.get("").isString();
    assert split.get("").asString().equals("123") : split.get("").asString();

    assert split.get("a").asString().equals("") : split.get("a").asString();
  }

  public void testFindByIndex() {
    ByteSplittable split = getByteSplittable("[0, {\"moreitems\":[false, {}, 0]}, 1, true, \"a\"]");

    assert split.isIndexed();
    assert split.get(0).asNumber() == 0;
    assert split.get(1).isKeyed();
    //noinspection PointlessBooleanExpression
    assert split.get(1).get("moreitems").get(0).asBoolean() == false;
    assert split.get(1).get("moreitems").get(1).isKeyed();
    assert split.get(1).get("moreitems").get(2).asNumber() == 0;
    assert split.get(2).asNumber() == 1;
    //noinspection PointlessBooleanExpression
    assert split.get(3).asBoolean() == true;
    assert split.get(4).asString().equals("a");

  }

  public void testNestedObjects() {
    ByteSplittable split = getByteSplittable("{\"obj\":{\"a\":[{}, 1, \"abc\", true, {\"foo\":2}]}}");

    assert split.get("obj").isKeyed();
    assert split.get("obj").get("a").isIndexed();
    assert split.get("obj").get("a").get(0).isKeyed();
    assert split.get("obj").get("a").get(1).asNumber() == 1;
    assert split.get("obj").get("a").get(2).asString().equals("abc");
    //noinspection PointlessBooleanExpression
    assert split.get("obj").get("a").get(3).asBoolean() == true;
    assert split.get("obj").get("a").get(4).get("foo").asNumber() == 2;
  }

  public void testFindMissingKeys() {
    String s = "{\"obj\":{\"a\":[{}, null, 1, \"abc\", true, {\"foo\":2}]}, \"n\":null}";
    ByteSplittable split = getByteSplittable(s);

    assert split.get("foo") == null;
    assert split.isNull("foo");
    assert split.get("obj").get("asdf") == null;
    assert split.get("obj").isNull("asdf");
    assert split.get("obj").get("a").get(0).get("asdf") == null;
    assert split.get("obj").get("a").get(0).isNull("asdf");

    assert split.get("n") == null;
    assert split.isNull("n");
    assert split.get("obj").get("a").get(1) == null;
    assert split.get("obj").get("a").isNull(1);
  }

  public void testArraySize() {
    Splittable empty1 = getByteSplittable(" [] ");
    assert empty1.size() == 0;

    Splittable empty2 = getByteSplittable("{\"list\":[]}").get("list");
    assert empty2.size() == 0;

    Splittable three = getByteSplittable(" [ 1, 2, null ] ");
    assert three.size() == 3;

    Splittable nestedFour = getByteSplittable("[[],[],[true], [\"a\", {\"b\":0}]]");
    assert nestedFour.size() == 4;
    assert nestedFour.get(0).size() == 0;
    assert nestedFour.get(3).size() == 2;
  }

  public void testObjectKeys() {
    Splittable empty1 = getByteSplittable(" {} ");
    assert empty1.getPropertyKeys().isEmpty();

    Splittable empty2 = getByteSplittable(" [ { } ] ").get(0);
    assert empty2.getPropertyKeys().isEmpty();

    Splittable hasKeys1 = getByteSplittable("{\"a\":0, \"b\":{}, \"c\":[]}");
    assert hasKeys1.getPropertyKeys().size() == 3;
    assert hasKeys1.getPropertyKeys().contains("a");
    assert hasKeys1.getPropertyKeys().contains("b");
    assert hasKeys1.getPropertyKeys().contains("c");

    Splittable hasKeys2 = getByteSplittable("{\"a\":[{\"a\":0, \"b\":{\"a\":1}, \"c\":[]}]}").get("a").get(0);
    assert hasKeys2.getPropertyKeys().size() == 3;
    assert hasKeys2.getPropertyKeys().contains("a");
    assert hasKeys2.getPropertyKeys().contains("b");
    assert hasKeys2.getPropertyKeys().contains("c");
  }

  private ByteSplittable getByteSplittable(String string) {
    return new ByteSplittable(makeByteBuffer(string));
  }
}
