package com.aap.quraankareem;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {

    private List<String> folderList;
    private OnFolderClickListener onFolderClickListener;
    private Context context; // إضافة متغير context


    // Constructor
    public FolderAdapter(Context context, List<String> folderList, OnFolderClickListener onFolderClickListener) {
        this.context = context;
        this.folderList = folderList;
        this.onFolderClickListener = onFolderClickListener;
    }


    @Override
    public FolderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FolderViewHolder holder, int position) {
        String folderName = folderList.get(position);
        holder.folderName.setText(folderName);

        // عند الضغط على الفولدر
        holder.itemView.setOnClickListener(v -> {
            if (onFolderClickListener != null) {
                showRecitationsDialog(v.getContext(), folderName);
            }
        });

        holder.numberOfReader.setText(position+1 + " -");
    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }
    private void showRecitationsDialog(Context context, String readerName) {
        List<String> recitations = getRecitationsForReader(readerName);

        if (recitations.isEmpty()) {
            Toast.makeText(context, "لا توجد روايات لهذا القارئ", Toast.LENGTH_SHORT).show();
            return;
        }

        // إنشاء الدايلوج
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_custom_recitations_list, null);
        builder.setView(dialogView);

        // تهيئة العناصر
        RecyclerView recyclerView = dialogView.findViewById(R.id.recitations_list_recycler_view);


        AlertDialog dialog = builder.create();


        dialog.show();
        RecitationListAdapter adapter = new RecitationListAdapter(recitations, recitationName -> {
            if (onFolderClickListener != null) {
                onFolderClickListener.onRecitationSelected(readerName, recitationName);
            }
            // إغلاق الدايلوج بعد الاختيار
            dialog.dismiss();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);
    }
    public void updateList(List<String> newList) {
        folderList = newList;
        notifyDataSetChanged(); // تحديث الـ RecyclerView
    }
    private List<String> getRecitationsForReader(String readerName) {
        List<String> recitations = new ArrayList<>();
        // الحصول على مسار مجلد القارئ
        File readerDir = new File(context.getFilesDir(), "القران الكريم/" + readerName);

        // تسجيل المسار للتأكد من صحته
        Log.d("FolderAdapter", "Checking directory: " + readerDir.getAbsolutePath());

        if (readerDir.exists() && readerDir.isDirectory()) {
            File[] files = readerDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        recitations.add(file.getName());
                        // تسجيل اسم الرواية التي تمت إضافتها
                        Log.d("FolderAdapter", "Added recitation: " + file.getName());
                    }
                }
            }
        } else {
            Log.e("FolderAdapter", "Directory does not exist: " + readerDir.getAbsolutePath());
        }
        return recitations;
    }




    // تعريف الواجهة لتعامل مع الضغط على الفولدر
    public interface OnFolderClickListener {
        void onRecitationSelected(String readerName, String recitationName);
    }

    // ViewHolder لعرض الفولدرات
    public static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView folderName, numberOfReader;

        public FolderViewHolder(View itemView) {
            super(itemView);
            folderName = itemView.findViewById(R.id.folderNameTextView);
            numberOfReader = itemView.findViewById(R.id.numberOfReaderName);
        }
    }
}

