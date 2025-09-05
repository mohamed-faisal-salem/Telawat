package com.aap.quraankareem;

public class SurahMetadata {
    private int index;
    private String name;

    // Getters and Setters
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // طريقة للحصول على الفهرس بشكل مكون من 3 أرقام
    public String getFormattedIndex() {
        return String.format("%03d", index);
    }
}