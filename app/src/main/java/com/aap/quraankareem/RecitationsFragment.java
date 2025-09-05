package com.aap.quraankareem;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class RecitationsFragment extends Fragment {
    private TextView readerName, followersCount;
    private Button followButton;
    private DatabaseReference readerRef;
    private boolean isFollowing = false;
    private RecyclerView recitationsRecyclerView;
    private RecitationAdapter adapter;
    private List<Recitation> recitations;
    private ConnectivityManager connectivityManager;
    private ValueEventListener followersListener;
    private ValueEventListener followingListener;
    private DatabaseReference userFollowRef;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressDialog progressDialog;
    boolean na = false;


    public static RecitationsFragment newInstance(Reader reader) {
        RecitationsFragment fragment = new RecitationsFragment();
        Bundle args = new Bundle();
        args.putSerializable("reader", reader);  // تمرير كائن القارئ
        fragment.setArguments(args);
        return fragment;
    }
    private String userId;
    private String deviceId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recitations, container, false);

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);

        deviceId = sharedPreferences.getString("device_id", null);

        if (deviceId == null) {
            String androidId = Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID);

            if (androidId == null || androidId.equals("9774d56d682e549c")) {
                deviceId = UUID.randomUUID().toString();
            } else {
                deviceId = androidId;
            }

            // حفظ deviceId في SharedPreferences
            sharedPreferences.edit().putString("device_id", deviceId).apply();
        }

        userId = deviceId;


        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // إعادة تحميل البيانات
            checkInternetConnection();
            swipeRefreshLayout.setRefreshing(false); // إيقاف رمز التحديث
        });


        readerName = view.findViewById(R.id.readerName);

        followersCount = view.findViewById(R.id.followersCount);
        followButton = view.findViewById(R.id.followButton);

        connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (getArguments() != null) {
            Reader reader = (Reader) getArguments().getSerializable("reader");

            readerName = view.findViewById(R.id.readerName);
            readerName.setText("القارئ " + reader.getName());
            readerName.setSelected(true);

            recitations = reader.getRecitations();

            ImageView profileImage = view.findViewById(R.id.profileImage);
            Glide.with(this)
                    .load(reader.getProfileImageUrl())
                    .placeholder(R.drawable.baseline_person_24)
                    .error(R.drawable.baseline_person_24)
                    .into(profileImage);

            readerRef = FirebaseDatabase.getInstance().getReference("readers").child(reader.getName());

            checkInternetConnection();

            followButton.setOnClickListener(v -> {
                if (isInternetConnected()) {
                    followButton.setEnabled(false); // تعطيل الزر
                    toggleFollow(reader.getName());
                } else {
                    Toast.makeText(getContext(), "لا يوجد اتصال بالإنترنت", Toast.LENGTH_SHORT).show();
                }
            });
        }

        recitationsRecyclerView = view.findViewById(R.id.recitationsRecyclerView);

        adapter = new RecitationAdapter(recitations, getContext(), this::onRecitationClicked);
        recitationsRecyclerView.setAdapter(adapter);
        recitationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        return view;
    }

    private boolean isInternetConnected() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void checkInternetConnection() {
        if (isInternetConnected()) {
            showLoadingDialog(); // عرض ProgressDialog

            // بدء مؤقت 5 ثوانٍ
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (progressDialog != null && progressDialog.isShowing()) {
                    // إذا لم يتم جلب البيانات خلال 5 ثوانٍ
                    followButton.setText("انترنت ضعيف");
                    followButton.setEnabled(false);
                    followButton.setBackgroundColor(getResources().getColor(R.color.gray));
                    hideLoadingDialog();

                    followButton.setEnabled(false);
                    followButton.setBackgroundColor(getResources().getColor(R.color.gray));
                }
            }, 5000); // 5 ثوانٍ

            // جلب البيانات
            Reader reader = (Reader) getArguments().getSerializable("reader");
            fetchFollowersCount(reader.getName());
            readerName.setText("القارئ " + reader.getName());
            readerName.setSelected(true);
            recitations = reader.getRecitations();
            checkIfFollowing(reader.getName());
        } else {
            // إذا لم يكن هناك اتصال بالإنترنت
            followersCount.setText("لا يوجد اتصال بالإنترنت");
            followButton.setText("لا يوجد اتصال");
            followButton.setEnabled(false);
            followButton.setBackgroundColor(getResources().getColor(R.color.gray));
        }
    }


    private void checkIfFollowing(String readerName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userFollowRef = db.collection("user_following").document(userId).collection("following").document(readerName);

        userFollowRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                isFollowing = task.getResult().exists();
                updateFollowButton(); // تحديث الزر بناءً على حالة المتابعة
            }

            // إخفاء ProgressDialog بعد جلب البيانات
            hideLoadingDialog();
        });
    }


    private void showLoadingDialog() {
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("جاري تحميل البيانات...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void updateFollowButton() {
        if (!isAdded()) return;
        if (isFollowing) {
            followButton.setText("إلغاء المتابعة");
            followButton.setEnabled(true); // إعادة تمكين الزر
            followButton.setBackgroundColor(getResources().getColor(R.color.blue_tech));
        } else {
            followButton.setText("متابعة");
            followButton.setEnabled(true); // إعادة تمكين الزر
            followButton.setBackgroundColor(getResources().getColor(R.color.electric_blue));
        }
    }

    private void fetchFollowersCount(String readerName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference readerRef = db.collection("readers").document(readerName);

        readerRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists() && documentSnapshot.contains("followers")) {
                long followers = documentSnapshot.getLong("followers");
                followersCount.setText("عدد المتابعين:  " + followers + " متابع");
            } else {
                followersCount.setText("عدد المتابعين:  0 متابع");
            }

            // إخفاء ProgressDialog بعد جلب البيانات
            hideLoadingDialog();
        }).addOnFailureListener(e -> {
            followersCount.setText("عدد المتابعين غير متاح");

            // إخفاء ProgressDialog في حالة الفشل
            hideLoadingDialog();
        });
    }



    private void toggleFollow(String readerName) {
        if (!isInternetConnected()) {
            Toast.makeText(getContext(), "لا يوجد اتصال بالإنترنت", Toast.LENGTH_SHORT).show();
            return;
        }

        na = false; // إعادة تعيين na
        showLoadingDialog(); // عرض ProgressDialog

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                Toast.makeText(getContext(), "انترنت ضعيف", Toast.LENGTH_SHORT).show();
                hideLoadingDialog();
                na = true;
            }
        }, 5000);

        if (!na) {
            // تنفيذ عملية المتابعة أو الإلغاء
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userFollowRef = db.collection("user_following").document(userId).collection("following").document(readerName);
            DocumentReference readerRef = db.collection("readers").document(readerName);

            if (isFollowing) {
                // إلغاء المتابعة
                userFollowRef.delete().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        isFollowing = false;
                        updateFollowButton();

                        // تقليل عدد المتابعين بمقدار -1
                        readerRef.update("followers", FieldValue.increment(-1)).addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                fetchFollowersCount(readerName); // تحديث العدد فورًا
                            }

                            // إخفاء ProgressDialog بعد إكمال العملية
                            hideLoadingDialog();
                        });
                    } else {
                        // إخفاء ProgressDialog في حالة الفشل
                        hideLoadingDialog();
                    }
                });
            } else {
                // التأكد من وجود القارئ في Firestore قبل التحديث
                readerRef.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().exists()) {
                        // إذا لم يكن القارئ موجودًا، يتم إنشاؤه لأول مرة
                        Map<String, Object> readerData = new HashMap<>();
                        readerData.put("name", readerName);
                        readerData.put("followers", 0);

                        readerRef.set(readerData).addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                followReader(userFollowRef, readerRef, readerName);
                            } else {
                                // إخفاء ProgressDialog في حالة الفشل
                                hideLoadingDialog();
                            }
                        });
                    } else {
                        followReader(userFollowRef, readerRef, readerName);
                    }
                });
            }
        }
    }


    private void followReader(DocumentReference userFollowRef, DocumentReference readerRef, String readerName) {
        userFollowRef.set(new HashMap<>()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                isFollowing = true;
                updateFollowButton();

                // زيادة عدد المتابعين بمقدار +1
                readerRef.update("followers", FieldValue.increment(1)).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        fetchFollowersCount(readerName); // تحديث العدد فورًا
                    }

                    // إخفاء ProgressDialog بعد إكمال العملية
                    hideLoadingDialog();
                });
            } else {
                // إخفاء ProgressDialog في حالة الفشل
                hideLoadingDialog();
            }
        });
    }






    private void onRecitationClicked(Recitation recitation) {
        String baseUrl = recitation.getBaseUrl();
        String readerName = recitation.getReaderName();
        String recitationName = recitation.getName();

        // 🔍 استخراج القارئ من الـ arguments
        Reader reader = (Reader) getArguments().getSerializable("reader");
        if (reader == null) {
            Log.e("RecitationsFragment", "لم يتم العثور على القارئ!");
            return;
        }

        // 🔍 البحث عن الرواية المطلوبة داخل القارئ
        List<Integer> allowedSurahs = new ArrayList<>();
        for (Recitation r : reader.getRecitations()) {
            if (r.getName().equals(recitationName)) {
                allowedSurahs = r.getSurahList() != null ? r.getSurahList() : new ArrayList<>();
                break;
            }
        }

        // 🚀 التنقل إلى `SurahFragment` مع تمرير `surah_list`
        ((MainActivity) getActivity()).navigateToSurahFragment(baseUrl, readerName, recitationName, allowedSurahs);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // إزالة الـ Listeners عند تدمير الـ Fragment
        if (readerRef != null && followersListener != null) {
            readerRef.child("followers").removeEventListener(followersListener);
        }
        if (userFollowRef != null && followingListener != null) {
            userFollowRef.removeEventListener(followingListener);
        }
    }
}






