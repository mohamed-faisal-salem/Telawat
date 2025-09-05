// Reader.java
package com.aap.quraankareem;

import java.io.Serializable;
import java.util.List;

public class Reader implements Serializable {
    private String name;
    private String baseUrl;
    private String profileImageUrl;
    private List<Recitation> recitations;
    private long followers = -1;
    private List<Integer> surahList;



    public void setFollowers(long followers) {
        this.followers = followers;
    }

    public long getFollowers() {
        return followers;
    }


    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<Recitation> getRecitations() {
        return recitations;
    }

    public void setRecitations(List<Recitation> recitations) {
        this.recitations = recitations;
    }
    public List<Integer> getSurahList() {
        return surahList;
    }

    public void setSurahList(List<Integer> surahList) {
        this.surahList = surahList;
    }
}
