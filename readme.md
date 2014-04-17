Premature optimization of the week - take the GWT AutoBean splittable interface and teach it how to read JSON from
a ByteBuffer. Parses the structure of the JSON in a single pass, so you can access a few properties and move on.

Not designed (or tested) to be a great way to read *every* property, but instead optimizing expressly for the minute
use case of sparse reading.

Tests run in one jvm with 10 seconds (should be much more than required) of warmup against GSON with a few included
sample files. On a 2.4ghz i7 mbp, with the 190mb json file at https://github.com/zeMirco/sf-city-lots-json, GSON sits
at all cores pegged for 10-12 seconds, this Splittable does it in about 1.1s, and for about a quarter of the heap
churn. Included sample files (thanks http://www.json-generator.com/) show a much closer race, with Splittable only
winning by 10-20%.

Downsides (oh yes):
 * incomplete
 * can't traverse structure
 * asking for properties that don't exist? that's an exception.
 * readonly (at present)
 * second commit in this tree is another 15% faster, but doesn't track as nice of offsets
 * strings are left as-is, no escaping is implemented
 * not tested with actual AutoBeans
 * might have problems with any object, array longer than 250mb...
 * generated name on github, unimaginative name in pom.xml

Upsides:
 * stupidly fast, and kind to your heap
 * most issues above can be fixed
 * ought to be gwt-compatible with https://github.com/WeTheInternet/xapi/'s TypedArray support, totally untested


The code:
 * https://github.com/niloc132/drunken-wallhack/blob/master/src/main/java/com/colinalworth/gwt/beans/shared/ByteSplittable.java

The tests:
 * Make sure it can JSON: https://github.com/niloc132/drunken-wallhack/blob/master/src/test/java/com/colinalworth/gwt/beans/shared/ByteSplittableTest.java
 * How fast? https://github.com/niloc132/drunken-wallhack/blob/master/src/test/java/com/colinalworth/gwt/beans/shared/ThruputTest.java