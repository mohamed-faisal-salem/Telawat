package com.aap.quraankareem;

import java.io.Serializable;
import java.util.List;


public class Recitation implements Serializable {
    private String name;
    private String baseUrl;
    private String readerName;
    private List<Integer> surahList;
    public List<Integer> getSurahList() {
        return surahList;
    }

    public void setSurahList(List<Integer> surahList) {
        this.surahList = surahList;
    }
    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getReaderName() {
        return readerName;
    }

    public void setReaderName(String readerName) {
        this.readerName = readerName;
    }
}