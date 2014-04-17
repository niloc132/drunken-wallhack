package com.colinalworth.gwt.beans.shared;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ThruputTest {
  private static final int WARMUP_SECONDS = 10;
  private String[] sizes = {
//          "1k", "1k+whitespace",
          "5k", "5k+whitespace",
          "20k", "20k+whitespace"};

  private byte[] readFile(String size, int file) throws IOException {
    Path path = Paths.get("target", "test-classes", size, file + ".json");
    return Files.readAllBytes(path);
  }

  public void warmupBBSplit() throws IOException {
    long start = System.currentTimeMillis();
    while (start + 1000 * WARMUP_SECONDS > System.currentTimeMillis()) {
      List<String> guids = new ArrayList<>(4 * 5 * 20);
      for (String size : sizes) {
        for (int i = 1; i <= 5; i++){
          ByteBuffer bb = ByteBuffer.wrap(readFile(size, i));
          ByteSplittable splittable = new ByteSplittable(bb);
          guids.add(splittable.get("guid").asString());
        }
      }
    }
  }

  @Test
  public void test20kBBSplit() throws IOException {
    warmupBBSplit();

    int bytes = 0;
    int time = 0;
    List<String> guids = new ArrayList<>();
    while (time < 1000) {
      for (int i = 1; i <=5; i++) {
        ByteBuffer bb = ByteBuffer.wrap(readFile("20k", i));
        bytes += bb.limit();
        long start = System.currentTimeMillis();
        ByteSplittable splittable = new ByteSplittable(bb);
        guids.add(splittable.get("guid").asString());
        guids.add(splittable.get("items").get(4).get("guid").asString());
        time += System.currentTimeMillis() - start;
      }
    }
    System.out.println(bytes + " bytes in " + (((double)time) / 1000.0) + " seconds");
  }


  public void warmupGson() throws IOException {
    JsonParser gson = new JsonParser();
    long start = System.currentTimeMillis();
    while (start + 1000 * WARMUP_SECONDS > System.currentTimeMillis()) {
      List<String> guids = new ArrayList<>(4 * 5 * 20);
      for (String size : sizes) {
        for (int i = 1; i <= 5; i++){
          String json = new String(readFile(size, i));
          JsonElement outer = gson.parse(json);
          guids.add(outer.getAsJsonObject().get("guid").getAsString());
        }
      }
    }
  }
  @Test
  public void test20kGson() throws IOException {
    warmupGson();
    JsonParser gson = new JsonParser();

    int bytes = 0;
    int time = 0;
    List<String> guids = new ArrayList<>();
    while (time < 1000) {
      for (int i = 1; i <=5; i++) {
        byte[] ba = readFile("20k", i);
        bytes += ba.length;
        String json = new String(ba);
        long start = System.currentTimeMillis();
        JsonElement outer = gson.parse(json);
        guids.add(outer.getAsJsonObject().get("guid").getAsString());
        guids.add(outer.getAsJsonObject().get("items").getAsJsonArray().get(4).getAsJsonObject().get("guid").getAsString());
        time += System.currentTimeMillis() - start;
      }
    }
    System.out.println(bytes + " bytes in " + (((double)time) / 1000.0) + " seconds");
  }

}
