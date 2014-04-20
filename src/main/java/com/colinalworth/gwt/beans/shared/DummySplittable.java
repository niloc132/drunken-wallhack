package com.colinalworth.gwt.beans.shared;

import com.google.web.bindery.autobean.shared.Splittable;

import java.util.List;

/**
 * Created by jim on 4/19/14.
 */
public class DummySplittable implements Splittable{
    @Override
    public boolean asBoolean() {
        return false;
    }

    @Override
    public double asNumber() {
        return 0;
    }

    @Override
    public void assign(Splittable splittable, int i) {

    }

    @Override
    public void assign(Splittable splittable, String s) {

    }

    @Override
    public String asString() {
        return null;
    }

    @Override
    public Splittable deepCopy() {
        return null;
    }

    @Override
    public Splittable get(int i) {
        return null;
    }

    @Override
    public Splittable get(String s) {
        return null;
    }

    @Override
    public String getPayload() {
        return null;
    }

    @Override
    public List<String> getPropertyKeys() {
        return null;
    }

    @Override
    public Object getReified(String s) {
        return null;
    }

    @Override
    public boolean isBoolean() {
        return false;
    }

    @Override
    public boolean isIndexed() {
        return false;
    }

    @Override
    public boolean isKeyed() {
        return false;
    }

    @Override
    public boolean isNull(int i) {
        return false;
    }

    @Override
    public boolean isNull(String s) {
        return false;
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public boolean isReified(String s) {
        return false;
    }

    @Override
    public boolean isString() {
        return false;
    }

    @Override
    public boolean isUndefined(String s) {
        return false;
    }

    @Override
    public void setReified(String s, Object o) {

    }

    @Override
    public void setSize(int i) {

    }

    @Override
    public int size() {
        return 0;
    }
}
