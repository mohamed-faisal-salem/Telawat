package com.aap.quraankareem;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class SurahFragment extends Fragment {

    public static SurahFragment newInstance(String baseUrl, String readerName, String recitationName, List<Integer> surahList) {
        SurahFragment fragment = new SurahFragment();
        Bundle args = new Bundle();
        args.putString("baseUrl", baseUrl);
        args.putString("readerName", readerName);
        args.putString("recitationName", recitationName);
        args.putIntegerArrayList("surahList", new ArrayList<>(surahList));
        fragment.setArguments(args);
        return fragment;
    }


    private RecyclerView surahRecyclerView;
    private MediaPlayer mediaPlayer;
    private List<Surah> surahList = new ArrayList<>();
    private SurahAdapter surahAdapter;
    private boolean isSurahPlaying = false;
    private int currentSurahPosition = -1;
    private boolean isRepeating = false;
    private boolean isShuffling = false;
    private Runnable stopRunnable;
    private Handler stopHandler = new Handler();
    private TextView reader_name;
    private ImageView download;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_surah, container, false);
        download = view.findViewById(R.id.download);
        download.setOnClickListener(v -> Toast.makeText(getContext(), "قريبا 🤍", Toast.LENGTH_SHORT).show());
        reader_name = view.findViewById(R.id.readerName);
        surahRecyclerView = view.findViewById(R.id.surahRecyclerView);
        surahRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mediaPlayer = new MediaPlayer();
        surahAdapter = new SurahAdapter(surahList, getContext());
        surahRecyclerView.setAdapter(surahAdapter);

        if (getArguments() != null) {
            String baseUrl = getArguments().getString("baseUrl");
            String readerName = getArguments().getString("readerName");
            String recitationName = getArguments().getString("recitationName"); // استرجاع recitationName
            reader_name.setText(readerName + " - " + recitationName);
            loadSurahList(baseUrl, readerName, recitationName); // إضافة recitationName كمعلمة
        }

        surahAdapter.setOnItemClickListener(position -> {
            Surah selectedSurah = surahList.get(position);
            String surahName = selectedSurah.getSurahName();
            String surahUrl = selectedSurah.getSurahUrl();
            String readerName2 = selectedSurah.getReaderName();
            String recitationName2 = selectedSurah.getRecitationName();
            String currentSurahPosition = selectedSurah.getCurrentSurahPosition();

            Intent serviceIntent = new Intent(getActivity(), MediaService.class);
            serviceIntent.putExtra("surahUrl", surahUrl);
            serviceIntent.putExtra("surahName", surahName);
            serviceIntent.putExtra("readerName", readerName2);
            serviceIntent.putExtra("recitationName", recitationName2);
            serviceIntent.putExtra("currentSurahPosition", currentSurahPosition);
            serviceIntent.putExtra("surahList", new ArrayList<>(surahList));
            getActivity().startService(serviceIntent);
        });

        mediaPlayer.setOnCompletionListener(mp -> onSurahCompletion());
        return view;
    }

    private void loadSurahList(String baseUrl, String readerName, String recitationName) {
        surahList.clear();
        List<SurahMetadata> surahMetadataList = parseQuranXml();

        List<Integer> allowedSurahs = getArguments().getIntegerArrayList("surahList");
        if (allowedSurahs == null || allowedSurahs.isEmpty()) {
            allowedSurahs = new ArrayList<>();
            for (int i = 1; i <= 114; i++) {
                allowedSurahs.add(i);
            }
        }

        List<SurahMetadata> filteredSurahs = filterSurahList(surahMetadataList, allowedSurahs);

        for (SurahMetadata surahMetadata : filteredSurahs) {
            String formattedIndex = String.format(Locale.ENGLISH, "%03d", surahMetadata.getIndex());
            String surahUrl = baseUrl + formattedIndex + ".mp3";
            surahList.add(new Surah(
                    surahMetadata.getName(),
                    surahUrl,
                    readerName,
                    formattedIndex,
                    recitationName,
                    surahMetadata.getIndex()
            ));
        }
        surahAdapter.notifyDataSetChanged();
    }


    private List<SurahMetadata> filterSurahList(List<SurahMetadata> allSurahs, List<Integer> allowedSurahs) {
        List<SurahMetadata> filtered = new ArrayList<>();
        for (SurahMetadata surah : allSurahs) {
            if (allowedSurahs.contains(surah.getIndex())) {
                filtered.add(surah);
            }
        }
        return filtered;
    }


    private List<SurahMetadata> parseQuranXml() {
        List<SurahMetadata> surahMetadataList = new ArrayList<>();
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.quran);
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inputStream, null);

            int eventType = parser.getEventType();
            SurahMetadata currentSurah = null;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (tagName.equalsIgnoreCase("sura")) {
                            currentSurah = new SurahMetadata();
                            currentSurah.setIndex(Integer.parseInt(parser.getAttributeValue(null, "index")));
                            currentSurah.setName(parser.getAttributeValue(null, "name"));
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if (tagName.equalsIgnoreCase("sura") && currentSurah != null) {
                            surahMetadataList.add(currentSurah);
                        }
                        break;
                }
                eventType = parser.next();
            }
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return surahMetadataList;
    }

    private void onSurahCompletion() {
        if (isRepeating) {
            playSurah(currentSurahPosition); // إعادة تشغيل نفس السورة
        } else if (!isShuffling) {
            if (currentSurahPosition < surahList.size() - 1) {
                playSurah(currentSurahPosition + 1);
            }
        } else {
            // إذا كان التشغيل العشوائي مفعلًا، اختر السورة التالية عشوائيًا
            playNextSurah();
        }
    }

    private void playSurah(int position) {
        Surah selectedSurah = surahList.get(position);
        String surahUrl = selectedSurah.getSurahUrl();
        String surahName = selectedSurah.getSurahName();
        String readerName = selectedSurah.getReaderName();
        String recitationName = selectedSurah.getRecitationName();
        String currentSurahPosition = selectedSurah.getCurrentSurahPosition();

        Intent serviceIntent = new Intent(getActivity(), MediaService.class);
        serviceIntent.putExtra("surahUrl", surahUrl);
        serviceIntent.putExtra("surahName", surahName);
        serviceIntent.putExtra("readerName", readerName);
        serviceIntent.putExtra("recitationName", recitationName);
        serviceIntent.putExtra("currentSurahPosition", currentSurahPosition);
        serviceIntent.putExtra("surahList", new ArrayList<>(surahList));
        getActivity().startService(serviceIntent);
    }

    private void playNextSurah() {
        if (isShuffling) {
            Random random = new Random();
            int randomPosition = random.nextInt(surahList.size());
            playSurah(randomPosition);
        } else {
            if (currentSurahPosition < surahList.size() - 1) {
                playSurah(currentSurahPosition + 1);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}