
forked from niloc132/drunken-wallhack

GWT AutoBean splittable interface and teach it how to read JSON from
a ByteBuffer. Parses the structure of the JSON in a single pass, so you can access a few properties and move on.


ByteSplittable:
  Best option for fully populating a json object graph from the stream
Shallow:
  Best option for querying an attribute at a time 
Dummy: 
  Demonstrates the java optimization potential of reading a given stream of bytes without processing it.


 * How fast? https://github.com/niloc132/drunken-wallhack/blob/master/src/test/java/com/colinalworth/gwt/beans/shared/ThruputTest.java
