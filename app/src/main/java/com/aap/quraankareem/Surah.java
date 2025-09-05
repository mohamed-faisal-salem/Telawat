package com.aap.quraankareem;

import android.os.Parcel;
import android.os.Parcelable;

public class Surah implements Parcelable {
    private String surahName;
    private String surahUrl;
    private String readerName;
    private String currentSurahPosition;
    private String recitationName;
    private int surahNumber;

    public Surah(String surahName, String surahUrl, String readerName, String currentSurahPosition, String recitationName, int surahNumber) {
        this.surahName = surahName;
        this.surahUrl = surahUrl;
        this.readerName = readerName;
        this.currentSurahPosition = currentSurahPosition;
        this.recitationName = recitationName;
        this.surahNumber = surahNumber;
    }


    public String getRecitationName() {
        return recitationName;
    }
    public void setRecitationName(String recitationName) {
        this.recitationName = recitationName;
    }

    public String getSurahName() {
        return surahName;
    }

    public void setSurahName(String surahName) {
        this.surahName = surahName;
    }

    public String getSurahUrl() {
        return surahUrl;
    }

    public void setSurahUrl(String surahUrl) {
        this.surahUrl = surahUrl;
    }

    public String getReaderName() {
        return readerName;
    }

    public void setReaderName(String readerName) {
        this.readerName = readerName;
    }

    public String getCurrentSurahPosition() {
        return currentSurahPosition;
    }

    public void setCurrentSurahPosition(String currentSurahPosition) {
        this.currentSurahPosition = currentSurahPosition;
    }
    public int getSurahNumber() {
        return surahNumber;
    }

    public void setSurahNumber(int surahNumber) {
        this.surahNumber = surahNumber;
    }

    // Parcelable implementation
    protected Surah(Parcel in) {
        surahName = in.readString();
        surahUrl = in.readString();
        readerName = in.readString();
        currentSurahPosition = in.readString();
        recitationName = in.readString();
    }

    public static final Creator<Surah> CREATOR = new Creator<Surah>() {
        @Override
        public Surah createFromParcel(Parcel in) {
            return new Surah(in);
        }

        @Override
        public Surah[] newArray(int size) {
            return new Surah[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(surahName);
        dest.writeString(surahUrl);
        dest.writeString(readerName);
        dest.writeString(currentSurahPosition);
        dest.writeString(recitationName);
    }
}