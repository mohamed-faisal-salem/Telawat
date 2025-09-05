package com.aap.quraankareem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SurahListFragment extends Fragment {

    private RecyclerView recyclerView;
    private List<Surah> surahList;
    TextView reader_name;

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_surah_list, container, false);
        view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        if (getArguments() != null) {

            String readerName = getArguments().getString("folder_name");
            String recitationName2 = getArguments().getString("recitationName"); // هنا يتم تعيين القيمة
            reader_name = view.findViewById(R.id.reader_name);
            reader_name.setText(readerName);

            recyclerView = view.findViewById(R.id.recyclerViewSurahList);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

            // تحميل السور الخاصة بالقارئ
            surahList = getDownloadedSurahs(readerName, recitationName2);

            SurahDownloaderAdapter adapter = new SurahDownloaderAdapter(surahList, surah ->
                    playSurah(surah.getSurahUrl(), surahList.indexOf(surah)));
            recyclerView = view.findViewById(R.id.recyclerViewSurahList);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            surahList = getDownloadedSurahs(readerName, recitationName2);
            recyclerView.setAdapter(adapter);
            adapter.setOnItemClickListener(position -> {
                playSurah(surahList.get(position).getSurahUrl(), position);
            });
            recyclerView.setAdapter(adapter);

        }

        return view;
    }

    private List<Surah> getDownloadedSurahs(String readerName, String recitationName) {
        List<Surah> surahs = new ArrayList<>();

        File baseFolder = new File(requireContext().getFilesDir(), "القران الكريم");
        File recitationFolder = new File(baseFolder, readerName + "/" + recitationName + "/");

        if (recitationFolder.exists() && recitationFolder.isDirectory()) {
            File[] files = recitationFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".mp3")) {
                        String fileName = file.getName().replace(".mp3", "");
                        String surahName = fileName.replaceAll("\\d", "");
                        int surahNumber = extractSurahNumber(fileName); // استخراج الرقم

                        Surah surah = new Surah(
                                surahName,
                                file.getAbsolutePath(),
                                readerName,
                                String.valueOf(System.currentTimeMillis()),
                                recitationName,
                                surahNumber
                        );

                        surahs.add(surah);
                    }
                }
            }
        } else {
            Log.e("SurahListFragment", "المسار غير موجود: " + recitationFolder.getAbsolutePath());
        }

        // ترتيب السور حسب الرقم
        Collections.sort(surahs, (s1, s2) -> Integer.compare(s1.getSurahNumber(), s2.getSurahNumber()));

        return surahs;
    }
    private int extractSurahNumber(String fileName) {
        // تعبير منتظم لاستخراج الأرقام من النص
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(fileName);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group()); // تحويل الرقم إلى int
        }
        return 0; // إذا لم يتم العثور على رقم
    }
    private void playSurah(String surahUrl, int position) {

        Surah selectedSurah = surahList.get(position);
        String surahName = selectedSurah.getSurahName();
        String readerName = selectedSurah.getReaderName();
        String recitationName = selectedSurah.getRecitationName();
        Log.d("SurahListFragment", "تشغيل السورة: " + surahName + " | القارئ: " + readerName + " | الرواية: " + recitationName);
        Intent serviceIntent = new Intent(getActivity(), MediaService.class);
        serviceIntent.putExtra("surahUrl", surahUrl);
        serviceIntent.putExtra("surahName", surahName);
        serviceIntent.putExtra("readerName", readerName);
        serviceIntent.putExtra("recitationName", recitationName);
        serviceIntent.putExtra("currentSurahPosition", String.valueOf(position));
        serviceIntent.putExtra("surahList", new ArrayList<>(surahList));
        getActivity().startService(serviceIntent);
    }
}

