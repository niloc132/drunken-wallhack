package com.colinalworth.gwt.beans.shared;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;

import java.util.List;

public interface Results {
  public interface ABF extends AutoBeanFactory {
    AutoBean<Results> wrapper();
  }


  String getGuid();
  List<Item> getItems();


  public interface Item {
    int getId();
    String getGuid();
    boolean isActive();
    String getBalance();
    String getPicture();
    int getAge();
    String getName();
    String getGender();
    String getCompany();
    String getEmail();
    String getPhone();
    String getAddress();
    String getAbout();
    String getRegistered();
    double getLatitude();
    double getLongitude();
    List<String> getTags();
    List<Friend> getFriends();
    String getGreeting();
    String getFavoriteFruit();
  }
  public interface Friend {
    int getId();
    String getName();
  }
}
