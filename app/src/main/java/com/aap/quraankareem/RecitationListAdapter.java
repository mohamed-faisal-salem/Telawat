package com.aap.quraankareem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecitationListAdapter extends RecyclerView.Adapter<RecitationListAdapter.RecitationListViewHolder> {

    private List<String> recitations;
    private OnRecitationListClickListener listener;

    public RecitationListAdapter(List<String> recitations, OnRecitationListClickListener listener) {
        this.recitations = recitations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecitationListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recitation_list, parent, false);
        return new RecitationListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecitationListViewHolder holder, int position) {
        String recitation = recitations.get(position);
        holder.recitationName.setText(recitation);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecitationListClick(recitation);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recitations.size();
    }

    static class RecitationListViewHolder extends RecyclerView.ViewHolder {
        TextView recitationName;

        public RecitationListViewHolder(@NonNull View itemView) {
            super(itemView);
            recitationName = itemView.findViewById(R.id.recitation_name);
        }
    }

    public interface OnRecitationListClickListener {
        void onRecitationListClick(String recitationName);
    }
}