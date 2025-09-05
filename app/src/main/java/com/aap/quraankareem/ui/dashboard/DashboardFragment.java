package com.aap.quraankareem.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aap.quraankareem.FolderAdapter;
import com.aap.quraankareem.MainActivity;
import com.aap.quraankareem.R;
import com.aap.quraankareem.Surah;
import com.aap.quraankareem.SurahAdapter;
import com.aap.quraankareem.SurahListFragment;
import com.aap.quraankareem.databinding.FragmentDashboardBinding;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private AppBarLayout toolbar;
    private TextInputEditText searchEditText;
    List<String> folders;
    FolderAdapter adapter;
    boolean checkcleareditText = true;
    public DashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        // إعادة تعيين حقل البحث والقائمة
        if (searchEditText != null) {
            searchEditText.setText(""); // مسح النص
        }
        if (adapter != null && folders != null) {
            adapter.updateList(folders); // عرض القائمة الكاملة
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        toolbar = view.findViewById(R.id.appBar);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // جلب قائمة الفولدرات
        folders = getFoldersInAppData();
         adapter = new FolderAdapter(getContext(), folders, new FolderAdapter.OnFolderClickListener() {
            @Override
            public void onRecitationSelected(String readerName, String recitationName) {
                SurahListFragment surahListFragment = new SurahListFragment();
                Bundle args = new Bundle();
                args.putString("readerName", readerName); // إرسال اسم القارئ
                args.putString("recitationName", recitationName); // إرسال اسم الرواية
                surahListFragment.setArguments(args);

                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToSurahListFragment(readerName, recitationName);
                }
            }
        });
        ImageButton searchOpen = view.findViewById(R.id.search_open);
        searchOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view.findViewById(R.id.searchContainer).setVisibility(View.VISIBLE);
            }
        });
        view.findViewById(R.id.share).setOnClickListener(v -> shareAppLink());
        recyclerView.setAdapter(adapter);
        EditText searchEditText = view.findViewById(R.id.searchEditText);
        ImageView clearIcon = view.findViewById(R.id.clearIcon);

// مراقبة النص داخل EditText
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFolders(s.toString(), adapter); // تنسيق القائمة بناءً على النص المدخل
                if (s.length() > 0) {
                    clearIcon.setVisibility(View.VISIBLE);
                    checkcleareditText = false;
                } else {
                    checkcleareditText = true;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        clearIcon.setOnClickListener(v -> {
            if (checkcleareditText){
                view.findViewById(R.id.searchContainer).setVisibility(View.GONE);
            }else {
            searchEditText.setText("");
            clearIcon.setBackgroundResource(R.drawable.baseline_close_w_24);
            }
        });

        return view;
    }
    private void shareAppLink() {
        String appLink = "https://play.google.com/store/apps/details?id=com.aap.quraankareem&hl=ar";
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "تحميل تطبيق قرآن كريم");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "قم بتحميل تطبيق تلاوات الآن:\n" + appLink);
        startActivity(Intent.createChooser(shareIntent, "مشاركة التطبيق عبر"));
    }
    private void filterFolders(String query, FolderAdapter adapter) {
        List<String> filteredList = new ArrayList<>();

        if (query.isEmpty()) {
            filteredList.addAll(folders); // استخدام القائمة الأصلية
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (String folder : folders) {
                if (folder.toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(folder);
                }
            }
        }

        adapter.updateList(filteredList);
    }
    // دالة جلب الفولدرات من المجلد "القران الكريم"
    private List<String> getFoldersInAppData() {
        List<String> folderList = new ArrayList<>();

        // تحديد مسار التخزين داخل التطبيق
        File directory = new File(getContext().getFilesDir(), "القران الكريم");

        if (directory.exists() && directory.isDirectory()) {
            // جلب كل الفولدرات داخل "القران الكريم"
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        folderList.add(file.getName()); // إضافة اسم الفولدر
                    }
                }
            }
        }
        return folderList;
    }

}

