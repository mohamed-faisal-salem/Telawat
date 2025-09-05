package com.aap.quraankareem;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class SurahAdapter extends RecyclerView.Adapter<SurahAdapter.SurahViewHolder> {
    private List<Surah> surahList;
    private Context context;
    private OnItemClickListener onItemClickListener;
    public SurahAdapter(List<Surah> surahList, Context context) {
        this.surahList = surahList;
        this.context = context;
    }
    public static class SurahViewHolder extends RecyclerView.ViewHolder {
        TextView surahName, readerName;
        ConstraintLayout playButton;
        TextView currentSurahPosition;
        ImageView play;
        public ProgressBar progressBar;
        public ImageView download;
        public SurahViewHolder(View itemView) {
            super(itemView);
            readerName = itemView.findViewById(R.id.readerName);
            surahName = itemView.findViewById(R.id.surahName);
            playButton = itemView.findViewById(R.id.rlt_surah);
            progressBar = itemView.findViewById(R.id.progressBar);
            download = itemView.findViewById(R.id.download);
            currentSurahPosition = itemView.findViewById(R.id.currentSurahPosition);
        }
    }
    @Override
    public SurahViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_surah, parent, false);
        return new SurahViewHolder(view);
    }
    @Override
    public void onBindViewHolder(SurahViewHolder holder, int position, List<Object> payloads) {
        if (!payloads.isEmpty()) {
            Object payload = payloads.get(0);
            if (payload instanceof Integer) {
                int progress = (Integer) payload;
                if (progress == -1) {
                    holder.progressBar.setVisibility(View.GONE);
                    holder.download.setImageResource(R.drawable.baseline_check_circle_outline_24);
                    holder.download.setVisibility(View.VISIBLE);
                    holder.download.setEnabled(false);
                } else {
                    holder.progressBar.setProgress(progress);
                }
            }
            return;
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    @Override
    public void onBindViewHolder(SurahViewHolder holder, int position) {
        Surah surah = surahList.get(position);
        holder.readerName.setText(surah.getReaderName() + " - " + surah.getRecitationName());
        holder.surahName.setText("سورة " + surah.getSurahName());
        holder.currentSurahPosition.setText(surah.getCurrentSurahPosition());
        Log.d("AdapterCheck", "Surah Name: " + surah.getSurahName() + ", URL: " + surah.getSurahUrl());
        String readerName = surah.getReaderName();
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(position);
            }
        });
        String surahNameFile = surah.getSurahName() + Integer.parseInt(surah.getCurrentSurahPosition()) + ".mp3";
        String surahNameFile2 = surah.getSurahName() + ".mp3";
        File privateFolder = new File(
                context.getFilesDir(),
                "القران الكريم/" + readerName + "/" + surah.getRecitationName()
        );
        File surahFile = new File(privateFolder, surahNameFile);
        File surahFile2 = new File(privateFolder, surahNameFile2);
        Log.d("FileCheck", "File Path: " + surahFile.getAbsolutePath());
        if (surahFile.exists() || surahFile2.exists()) {
            Log.d("FileCheck", "File Exists: Yes");
            holder.download.setImageResource(R.drawable.baseline_check_circle_outline_24);
            holder.download.setEnabled(false);
            holder.itemView.findViewById(R.id.download).setVisibility(View.VISIBLE);
            holder.progressBar.setVisibility(View.GONE);
            Log.d("DownloadComplete", "تم تحميل السورة: " + surah.getSurahName());
        } else {
            Log.d("FileCheck", "File Exists: No");
            holder.download.setImageResource(R.drawable.baseline_download_24);
            holder.itemView.findViewById(R.id.download).setVisibility(View.VISIBLE);
            holder.progressBar.setVisibility(View.GONE);
        }
        holder.itemView.findViewById(R.id.download).setOnClickListener(v -> {
            holder.itemView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            holder.itemView.findViewById(R.id.download).setVisibility(View.GONE);
            downloadSurahUsingHttpURLConnection(surah, holder, readerName);
        });
        holder.playButton.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(position);
            }
        });
    }
    @Override
    public int getItemCount() {
        return surahList.size();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    public interface OnItemClickListener {
        void onItemClick(int position);
    }
    private void downloadSurahUsingHttpURLConnection(Surah surah, SurahViewHolder holder, String readerName) {
        new Thread(() -> {
            try {
                File downloadFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "القران الكريم");
                if (!downloadFolder.exists()) downloadFolder.mkdirs();

                File readerFolder = new File(downloadFolder, readerName);
                if (!readerFolder.exists()) readerFolder.mkdirs();

                URL url = new URL(surah.getSurahUrl());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP error code: " + responseCode);
                }

                String folderName = "القران الكريم/" + readerName + "/" + surah.getRecitationName();
                File privateFolder = new File(context.getFilesDir(), folderName);
                if (!privateFolder.exists()) privateFolder.mkdirs();

                String surahName = surah.getSurahName().replaceAll("[^a-zA-Z0-9أ-ي]", "_");
                File surahFile = new File(privateFolder, surahName + Integer.parseInt(surah.getCurrentSurahPosition()) + ".mp3");

                InputStream inputStream = connection.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(surahFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;
                long fileSize = connection.getContentLength();

                // ✅ إظهار البروجريس بار من البداية
                holder.itemView.post(() -> {
                    holder.progressBar.setProgress(0);
                    holder.progressBar.setVisibility(View.VISIBLE);
                    holder.download.setVisibility(View.GONE);
                });

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    int progress = fileSize > 0 ? (int) ((totalBytesRead * 100) / fileSize) : 0;

                    // ✅ تحديث الـ ProgressBar كل 1% فقط لتحسين الأداء
                    if (progress % 1 == 0) {
                        holder.progressBar.post(() -> holder.progressBar.setProgress(progress));
                    }
                }
                outputStream.close();
                inputStream.close();
                connection.disconnect();

                // ✅ إخفاء البروجريس بار بعد التحميل
                holder.itemView.post(() -> {
                    holder.download.setImageResource(R.drawable.baseline_check_circle_outline_24);
                    holder.download.setVisibility(View.VISIBLE);
                    holder.download.setEnabled(false);
                });

            } catch (IOException e) {
                e.printStackTrace();
                holder.itemView.post(() -> {
                    holder.progressBar.setVisibility(View.GONE);
                    holder.download.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

}