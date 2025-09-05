package com.aap.quraankareem;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.Collections;
import java.util.List;


public class ReaderAdapter extends RecyclerView.Adapter<ReaderAdapter.ReaderViewHolder> {

    private List<Reader> readerList;
    private Context context;
    private ReaderItemClickListener listener;

    // Constructor
    public ReaderAdapter(List<Reader> readerList, Context context, ReaderItemClickListener listener) {
        this.readerList = readerList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReaderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_reader, parent, false);
        return new ReaderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReaderViewHolder holder, int position) {
        Reader reader = readerList.get(position);
        holder.readerName.setText(reader.getName());
        holder.readerName.setSelected(true);
        int p = position + 1;
        holder.numberOfReaderName.setText("" + p + " -");


        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReaderClicked(reader);  // مرر الكائن كاملًا
            }
        });
    }

    @Override
    public int getItemCount() {
        return readerList.size();
    }
    public void updateList(List<Reader> newList) {
        // ترتيب القائمة بناءً على عدد المتابعين (تنازلي)
        Collections.sort(newList, (r1, r2) -> Long.compare(r2.getFollowers(), r1.getFollowers()));

        readerList = newList;
        notifyDataSetChanged();
    }

    public static class ReaderViewHolder extends RecyclerView.ViewHolder {
        TextView readerName, numberOfReaderName;

        public ReaderViewHolder(@NonNull View itemView) {
            super(itemView);
            readerName = itemView.findViewById(R.id.readerName);
            numberOfReaderName = itemView.findViewById(R.id.numberOfReaderName);
        }
    }

    public interface ReaderItemClickListener {
        void onReaderClicked(Reader reader);  // غيّر المُعامل إلى Reader
    }
}