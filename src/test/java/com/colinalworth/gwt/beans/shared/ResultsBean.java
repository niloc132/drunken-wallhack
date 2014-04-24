package com.colinalworth.gwt.beans.shared;

import java.util.List;

public class ResultsBean implements Results {
  private String guid;
  List<ItemBean> items;

  public String getGuid() {
    return guid;
  }

  public List<Item> getItems() {
    return (List) items;
  }

  public static class ItemBean implements Item {
    private int id;
    private String guid;
    private boolean active;
    private String balance;
    private String picture;
    private int age;
    private String name;
    private String gender;
    private String company;
    private String email;
    private String phone;
    private String address;
    private String about;
    private String registered;
    private double latitude;
    private double longitude;
    private List<String> tags;
    private List<FriendBean> friends;
    private String greeting;
    private String favoriteFruit;

    public int getId() {
      return id;
    }

    public String getGuid() {
      return guid;
    }

    public boolean isActive() {
      return active;
    }

    public String getBalance() {
      return balance;
    }

    public String getPicture() {
      return picture;
    }

    public int getAge() {
      return age;
    }

    public String getName() {
      return name;
    }

    public String getGender() {
      return gender;
    }

    public String getCompany() {
      return company;
    }

    public String getEmail() {
      return email;
    }

    public String getPhone() {
      return phone;
    }

    public String getAddress() {
      return address;
    }

    public String getAbout() {
      return about;
    }

    public String getRegistered() {
      return registered;
    }

    public double getLatitude() {
      return latitude;
    }

    public double getLongitude() {
      return longitude;
    }

    public List<String> getTags() {
      return tags;
    }

    public List<Friend> getFriends() {
      return (List)friends;
    }

    public String getGreeting() {
      return greeting;
    }

    public String getFavoriteFruit() {
      return favoriteFruit;
    }
  }
  public static class FriendBean implements Friend {
    private int id;
    private String name;

    public int getId() {
      return id;
    }

    public String getName() {
      return name;
    }
  }
}
