package com.aap.quraankareem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RecitationAdapter extends RecyclerView.Adapter<RecitationAdapter.ViewHolder> {
    private List<Recitation> recitations;
    private Context context;
    private OnRecitationClickListener listener;

    public RecitationAdapter(List<Recitation> recitations, Context context, OnRecitationClickListener listener) {
        this.recitations = recitations;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recitation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recitation recitation = recitations.get(position);
        holder.recitationName.setText(recitation.getName());  // عرض اسم الرواية

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecitationClicked(recitation);  // تمرير رابط الرواية واسم القارئ
            }
        });
    }

    @Override
    public int getItemCount() {
        return recitations.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView recitationName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            recitationName = itemView.findViewById(R.id.recitationName);
        }
    }

    public interface OnRecitationClickListener {
        void onRecitationClicked(Recitation recitation);
    }
}