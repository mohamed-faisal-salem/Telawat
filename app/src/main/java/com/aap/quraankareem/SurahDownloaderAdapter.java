package com.aap.quraankareem;

import android.content.Context;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.List;


public class SurahDownloaderAdapter extends RecyclerView.Adapter<SurahDownloaderAdapter.SurahViewHolder> {

    private List<Surah> surahList;
    private OnSurahClickListener onSurahClickListener;


    private Context context;
    private SurahDownloaderAdapter.OnItemClickListener onItemClickListener;


    // Constructor
    public SurahDownloaderAdapter(List<Surah> surahList, OnSurahClickListener onSurahClickListener) {
        this.surahList = surahList;
        this.onSurahClickListener = onSurahClickListener;
        this.context = context;

    }

    public static class SurahViewHolder extends RecyclerView.ViewHolder {
        TextView surahName, surahPosition, readerName;
        ConstraintLayout playButton;
        ImageView download;
        public ProgressBar progressBar;


        public SurahViewHolder(View itemView) {
            super(itemView);
            readerName = itemView.findViewById(R.id.readerName);
            surahName = itemView.findViewById(R.id.surahName);
            playButton = itemView.findViewById(R.id.rlt_surah);
            progressBar = itemView.findViewById(R.id.progressBar);
            download = itemView.findViewById(R.id.download);
            download.setVisibility(View.GONE);
            surahPosition = itemView.findViewById(R.id.currentSurahPosition);
            surahPosition.setVisibility(View.GONE);

        }
    }


    @Override
    public SurahViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_surah, parent, false);
        return new SurahViewHolder(view);

    }

    @Override
    public void onBindViewHolder(SurahViewHolder holder, int position) {
        Surah surah = surahList.get(position);


        holder.surahName.setText("سورة " + surah.getSurahName());
        holder.readerName.setText("" + surah.getReaderName() + " - " + surah.getRecitationName());

        holder.itemView.setOnClickListener(v -> {
            if (onSurahClickListener != null) {
                onSurahClickListener.onSurahClick(surah);
            }
        });

        String readerName = surah.getReaderName();

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(position);
            }
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

    // Interface to handle item click
    public interface OnItemClickListener {
        void onItemClick(int position);
    }
    public interface OnSurahClickListener {
        void onSurahClick(Surah surah);
    }

}

