package com.aap.quraankareem.ui.home;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aap.quraankareem.MainActivity;
import com.aap.quraankareem.Recitation;
import com.aap.quraankareem.SurahFragment;
import com.aap.quraankareem.R;
import com.aap.quraankareem.Reader;
import com.aap.quraankareem.ReaderAdapter;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.xmlpull.v1.XmlPullParser;


public class HomeFragment extends Fragment {

    private RecyclerView readerRecyclerView;
    private ReaderAdapter readerAdapter;
    private List<Reader> readerList;
    boolean checkcleareditText = true;
    private AppBarLayout toolbar;
    private ProgressDialog progressDialog;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);



        EditText searchEditText = view.findViewById(R.id.searchEditText);
        ImageView clearIcon = view.findViewById(R.id.clearIcon);
        toolbar = view.findViewById(R.id.appBar);
        // تهيئة ProgressDialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("جاري تحميل البيانات...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        readerList = parseReadersXml();


        readerRecyclerView = view.findViewById(R.id.readerRecyclerView);
        readerRecyclerView = view.findViewById(R.id.readerRecyclerView);
        readerRecyclerView.setVisibility(View.GONE);

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);
        readerRecyclerView.setLayoutManager(layoutManager);
        readerAdapter = new ReaderAdapter(readerList, getContext(), new ReaderAdapter.ReaderItemClickListener() {
            @Override
            public void onReaderClicked(Reader readerName) {
                Log.d("ReaderAdapter", "Reader clicked: " + readerName);

                // استدعاء دالة التنقل في MainActivity
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToRecitationsFragment(readerName);
                }
            }
        });

        readerRecyclerView.setAdapter(readerAdapter);

        fetchFollowersAndSort();




// مراقبة النص داخل EditText
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterReaders(s.toString()); // تنسيق القائمة بناءً على النص المدخل
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
        ImageButton searchOpen = view.findViewById(R.id.search_open);
        searchOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view.findViewById(R.id.searchContainer).setVisibility(View.VISIBLE);
            }
        });
        view.findViewById(R.id.share).setOnClickListener(v -> shareAppLink());
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

    private void filterReaders(String query) {
        List<Reader> filteredList = new ArrayList<>();

        if (query.isEmpty()) {
            // إذا كان حقل البحث فارغًا، اعرض القائمة الكاملة
            filteredList.addAll(readerList);
        } else {
            // تصفية القائمة بناءً على النص المدخل
            String lowerCaseQuery = query.toLowerCase();
            for (Reader reader : readerList) {
                if (reader.getName().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(reader);
                }
            }
        }

        // تحديث الـ Adapter بالقائمة المصفاة
        readerAdapter.updateList(filteredList);
    }
    private void fetchFollowersAndSort() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        final AtomicInteger completedTasks = new AtomicInteger(0);

        for (Reader reader : readerList) {
            firestore.collection("readers").document(reader.getName())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            long followers = task.getResult().getLong("followers");
                            reader.setFollowers(followers);
                        } else {
                            reader.setFollowers(-1); // إشارة إلى فشل الجلب
                        }

                        if (completedTasks.incrementAndGet() == readerList.size()) {
                            if (allDataFetchedSuccessfully()) {
                                sortAndUpdateUI();
                            } else {
                                showOriginalOrder();
                            }
                        }
                    });
        }
    }

    private boolean allDataFetchedSuccessfully() {
        for (Reader reader : readerList) {
            if (reader.getFollowers() == -1) {
                return false;
            }
        }
        return true;
    }

    private void showOriginalOrder() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                readerRecyclerView.setVisibility(View.VISIBLE);
                readerAdapter.updateList(readerList);
            });
        }
    }
    private void sortAndUpdateUI() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // إخفاء الـ ProgressDialog
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                // ترتيب القائمة حسب المتابعين
                Collections.sort(readerList, (r1, r2) -> Long.compare(r2.getFollowers(), r1.getFollowers()));

                // عرض الـ RecyclerView
                readerRecyclerView.setVisibility(View.VISIBLE);
                readerAdapter.updateList(readerList);
            });
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // إخفاء الـ ProgressDialog إذا تم تدمير الـ Fragment
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }


    private List<Reader> parseReadersXml() {
        List<Reader> readers = new ArrayList<>();
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.readers);
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inputStream, null);

            int eventType = parser.getEventType();
            Reader currentReader = null;
            Recitation currentRecitation = null;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (tagName.equalsIgnoreCase("reader")) {
                            currentReader = new Reader();
                            currentReader.setRecitations(new ArrayList<>());
                        } else if (tagName.equalsIgnoreCase("recitation")) {
                            currentRecitation = new Recitation();
                        } else if (currentRecitation != null) {  // ✅ الربط بالتلاوة الحالية
                            if (tagName.equalsIgnoreCase("name")) {
                                currentRecitation.setName(parser.nextText());
                            } else if (tagName.equalsIgnoreCase("base_url")) {
                                currentRecitation.setBaseUrl(parser.nextText());
                            } else if (tagName.equalsIgnoreCase("surah_list")) {
                                String surahListStr = parser.nextText();
                                List<Integer> surahList = new ArrayList<>();
                                for (String surah : surahListStr.split(",")) {
                                    surahList.add(Integer.parseInt(surah.trim()));
                                }
                                currentRecitation.setSurahList(surahList);
                            }
                        } else if (currentReader != null) {  // ✅ الربط بالقارئ الحالي
                            if (tagName.equalsIgnoreCase("name")) {
                                currentReader.setName(parser.nextText());
                            } else if (tagName.equalsIgnoreCase("profileImageUrl")) {
                                currentReader.setProfileImageUrl(parser.nextText());
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if (tagName.equalsIgnoreCase("reader")) {
                            readers.add(currentReader);
                        } else if (tagName.equalsIgnoreCase("recitation")) {
                            if (currentRecitation != null) {
                                currentRecitation.setReaderName(currentReader.getName());
                                currentReader.getRecitations().add(currentRecitation);
                            }
                            currentRecitation = null;
                        }
                        break;
                }
                eventType = parser.next();
            }
            inputStream.close();
        } catch (Exception e) {
            Log.e("XML_PARSING", "خطأ أثناء تحليل XML: " + e.getMessage());
            e.printStackTrace();
        }
        return readers;
    }



}