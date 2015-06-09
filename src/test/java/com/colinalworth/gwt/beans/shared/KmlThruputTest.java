package com.colinalworth.gwt.beans.shared;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.web.bindery.autobean.shared.Splittable;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.charset.StandardCharsets.UTF_8;

public class KmlThruputTest {
    private static final int WARMUP_SECONDS = 10;
    public static final String SRC = "https://raw.githubusercontent.com/automenta/traytention/master/data/climateviewer.json";
    URI src;

//    private List<Path> files;
    private boolean readGuids = false;
    private Object content;
    private ByteBuffer byteBuffer;

    public KmlThruputTest() {
    }

    @Before
    public void getPaths() throws IOException {

        URL url = new URL(SRC);
        HttpURLConnection openConnection = (HttpURLConnection) url.openConnection();
        int contentLength = openConnection.getContentLength();
        byteBuffer = ByteBuffer.allocateDirect(contentLength);
        try (InputStream inputStream = openConnection.getInputStream()) {
            ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream);
            while(byteBuffer.hasRemaining()) {
                int read = readableByteChannel.read(byteBuffer);
                assert read>=0;
            }

        }
        byteBuffer.rewind();
        
    }


    @Test
    public void testPlaceboSplit() throws IOException {
AtomicLong time=new AtomicLong(0);
        {
            final double[] bytes = {0};
            final List<String> guids = new ArrayList<>();
            while (time.get() < 1000) {
                  {
             
                      {
                          ByteBuffer bb = (ByteBuffer) this.byteBuffer.rewind();
                          bytes[0] += bb.limit();
                        long start = System.currentTimeMillis();
                        while (bb.hasRemaining()) bb.get();

                          time .addAndGet(System.currentTimeMillis() - start);

                     }
                } ;
            }

            System.out.println("Placebo on " + SRC);
            System.out.println(bytes[0] + " bytes in " + (((double) time.get())/ 1000.0) + " seconds, " + bytes[0] / time.get() * 1000.0 / 1024.0 / 1024.0 + "mb/second");
        }

    }
    public void warmupBBSplit() throws IOException {
        long start = System.currentTimeMillis();
        while (start + 1000 * WARMUP_SECONDS > System.currentTimeMillis()) {
            final List<String> guids = new ArrayList<>(4 * 5 * 20);
              {
               {
                   {
                        
                        new ByteSplittable((ByteBuffer) byteBuffer.rewind());
                     }
                } ;
            }
        }
    }

    @Test
    public void testBBSplit() throws IOException {
        warmupBBSplit();
        AtomicLong time=new AtomicLong(0);

        {
            final double[] bytes = {0};
            
            final List<String> guids = new ArrayList<>();
            while (time .get()< 1000) {
                {
                {
                    bytes[0] += byteBuffer.rewind().limit();
                        long start = System.currentTimeMillis();
                        ByteSplittable splittable = new ByteSplittable(byteBuffer);
                        if (readGuids) {
                            guids.add(splittable.get("guid").asString());
                            guids.add(splittable.get("items").get(4).get("guid").asString());
                        }
                    time .addAndGet(System.currentTimeMillis() - start);

                    }
                };
            }

            System.out.println("ByteBufferSplittable on " + SRC);
            System.out.println(bytes[0] + " bytes in " + (time .get()/ 1000.0) + " seconds, " + bytes[0] / time .get()* 1000.0 / 1024.0 / 1024.0 + "mb/second");
        }

    }

    public void warmupLazySplit() throws IOException {
        long start = System.currentTimeMillis();
        while (start + 1000 * WARMUP_SECONDS > System.currentTimeMillis()) {
            final List<String> guids = new ArrayList<>(4 * 5 * 20);
      {
             {
                       {
                        ByteBuffer bb = (ByteBuffer) byteBuffer.rewind();
                        ShallowSplittable.create(bb);
                     }
                } ;
            }
        }
    }

    @Test
    public void testLazySplit() throws IOException {
//        warmupLazySplit();
        AtomicLong time=new AtomicLong(0);

     {
            final double[] bytes = {0};
            final List<String> guids = new ArrayList<>();
            while (time .get()< 1000) {  {
                    {
                        ByteBuffer bb = (ByteBuffer) byteBuffer.rewind();
                        bytes[0] += bb.limit();
                        long start = System.currentTimeMillis();
                        Splittable splittable = ShallowSplittable.create(bb);
                        if (readGuids) {
                            guids.add(splittable.get("guid").asString());
                            guids.add(splittable.get("items").get(4).get("guid").asString());
                        }
                        time .addAndGet( System.currentTimeMillis() - start);

                     }
                };
            }

            System.out.println("ShallowSplittable on " + SRC);
            System.out.println(bytes[0] + " bytes in " + (time .get()/ 1000.0) + " seconds, " + bytes[0] / time .get()* 1000.0 / 1024.0 / 1024.0 + "mb/second");
        }

    }

    public void warmupGson() throws IOException {
        final JsonParser gson = new JsonParser();
        long start = System.currentTimeMillis();
        while (start + 1000 * WARMUP_SECONDS > System.currentTimeMillis()) {
            final List<String> guids = new ArrayList<>(4 * 5 * 20);
          {
                  {
                 {
                        String json = String.valueOf(UTF_8.decode((ByteBuffer) byteBuffer.rewind()));
                        JsonElement outer = gson.parse(json);
                        if (readGuids) {
                            guids.add(outer.getAsJsonObject().get("guid").getAsString());
                        }
                     }
                } ;
            }
        }
    }

    @Test
    public void testGson() throws IOException {
        warmupGson();
        final JsonParser gson = new JsonParser();
        AtomicLong time=new AtomicLong(0);

       {
           final double[] bytes = {0};
            final List<String> guids = new ArrayList<>();
            while (time .get()< 1000) {
                 {   {
                        String json = String.valueOf(UTF_8.decode((ByteBuffer) byteBuffer.rewind()));
                        long start = System.currentTimeMillis();
                        JsonElement outer = gson.parse(json);
                        if (readGuids) {
                            guids.add(outer.getAsJsonObject().get("guid").getAsString());
                            guids.add(outer.getAsJsonObject().get("items").getAsJsonArray().get(4).getAsJsonObject().get("guid").getAsString());
                        }
                        time .addAndGet( System.currentTimeMillis() - start);
                        
                    }
                };
            }

            System.out.println("Gson's JsonParser on " + SRC);
            double v = time.get();
            System.out.println(bytes[0] + " bytes in " + (v / 1000.0) + " seconds, " + bytes[0] / time.get() * 1000.0 / 1024.0 / 1024.0 + "mb/second");
        }

    }
}
