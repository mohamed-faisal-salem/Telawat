package com.aap.quraankareem;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.RippleDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.aap.quraankareem.ui.dashboard.DashboardFragment;
import com.aap.quraankareem.ui.home.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.realgear.multislidinguppanel.Adapter;
import com.realgear.multislidinguppanel.MultiSlidingUpPanelLayout;
import com.realgear.multislidinguppanel.PanelStateListener;


public class MainActivity extends AppCompatActivity implements MediaService.BottomSheetController {

    private boolean doubleBackToExitPressedOnce = false;

    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 0;
    private boolean timerRunning = false;
    private TextView timerTextView;
    private MediaService mediaService =new MediaService();
    private boolean isRepeatEnabled = false;
    private boolean isShuffleEnabled = false;
    private boolean isBound = false;
    private boolean expand= false;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private TextView surahNameTextView, readerNameTextView, surahNameTv, timeText, timeSurah;
    private SeekBar seekBar;
    View bottomSheet;
    boolean isPlaying2 = false;
    boolean hideBottomSheetCheck=false;

    private static final int REQUEST_CODE = 1001;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        BottomNavigationView navView = findViewById(R.id.nav_view);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        // إعداد BottomNavigationView
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard,R.id.navigation_folders, R.id.navigation_notifications)
                .build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        // إضافة Listener للتنقل بين الفراجمنتات
        navView.setOnNavigationItemSelectedListener(item -> {
            // إزالة SurahFragment إذا كان موجودًا
            removeSurahFragment();
            removeSurahListFragment();
            removeRecitationsFragment();

            // التنقل إلى الفراجمنت المحدد
            navController.navigate(item.getItemId());
            return true;
        });

        surahNameTextView = findViewById(R.id.surah_name);
        readerNameTextView = findViewById(R.id.readerName);
        surahNameTv = findViewById(R.id.surahName);
        if (ContextCompat.checkSelfPermission(
                this, // إضافة السياق هنا (this إذا كانت Activity)
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED) {
            // الخطأ الثاني: تعديل طريقة استدعاء requestPermissions
            ActivityCompat.requestPermissions(
                    this, // النشاط الحالي (Activity)
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE
            );
        }
        timerTextView = findViewById(R.id.timer);
        findViewById(R.id.timeSelectorButton).setOnClickListener(v -> showTimerDialog());
        findViewById(R.id.timer).setOnClickListener(v -> showActiveTimerDialog());

        timeText = findViewById(R.id.timeText);
        timeSurah = findViewById(R.id.timeSurah);
        seekBar = findViewById(R.id.seekBar);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaService != null) {
                    mediaService.getMediaPlayer().seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });


        bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }


        findViewById(R.id.bottom_sheet).setVisibility(View.VISIBLE);


        // تعيين حالات التوسيع والتصغير
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    expand = true;
                    // عند التوسيع الكامل، يتم إخفاء navView بالكامل
                    navView.setVisibility(View.GONE);
                    findViewById(R.id.collapsed_layout).setVisibility(View.GONE);
                    findViewById(R.id.expanded_layout).setVisibility(View.VISIBLE);
                    findViewById(R.id.expanded_layout).setAlpha(1f);

                    if (mediaService != null) {
                        updatePlayPauseButton(mediaService.getMediaPlayer().isPlaying());
                    }
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    expand = false;
                    navView.setVisibility(View.VISIBLE);
                    findViewById(R.id.collapsed_layout).setVisibility(View.VISIBLE);
                    findViewById(R.id.collapsed_layout).setAlpha(1f);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                navView.setAlpha(1 - slideOffset);


                // جعل collapsed_layout يختفي تدريجياً عند التوسيع
                findViewById(R.id.collapsed_layout).setAlpha(1 - slideOffset);
                findViewById(R.id.collapsed_layout).setScaleY(1 - (slideOffset * 0.2f));  // تقليل الحجم بنسبة 20%
                findViewById(R.id.collapsed_layout).setScaleX(1 - (slideOffset * 0.2f));
                if (slideOffset > 0.9) {  // ضمان أنه يختفي بالكامل عند التوسيع
                    findViewById(R.id.collapsed_layout).setVisibility(View.GONE);
                } else {
                    findViewById(R.id.collapsed_layout).setVisibility(View.VISIBLE);
                }
                // تقليل الحجم تدريجياً حتى يصبح صغير جداً
                float scale = 1 - (slideOffset * 0.3f); // يصغر حتى 70% من حجمه الأصلي
                navView.setScaleY(scale);
                navView.setScaleX(scale);

                // عند اقتراب التوسيع الكامل، إخفاء العنصر نهائيًا
                if (slideOffset > 0.9) {
                    navView.setVisibility(View.GONE);
                } else {
                    navView.setVisibility(View.VISIBLE);
                }
            }
        });

        ImageView repeat, shuddle;
        repeat = findViewById(R.id.repeat);
        shuddle = findViewById(R.id.shuddle);
        findViewById(R.id.repeat).setOnClickListener(v -> {
            isRepeatEnabled = !isRepeatEnabled;
            repeat.setImageResource(isRepeatEnabled ? R.drawable.baseline_repeat_one_24 : R.drawable.baseline_repeat_24);
            Intent intent = new Intent("UPDATE_REPEAT");
            intent.putExtra("isRepeatEnabled", isRepeatEnabled);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        });

// زر Shuffle
        findViewById(R.id.shuddle).setOnClickListener(v -> {
            isShuffleEnabled = !isShuffleEnabled;
            shuddle.setImageResource(isShuffleEnabled ? R.drawable.baseline_shuffle_w_24 : R.drawable.baseline_shuffle_24);
            Intent intent = new Intent("UPDATE_SHUFFLE");
            intent.putExtra("isShuffleEnabled", isShuffleEnabled);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        });

        findViewById(R.id.nextButton).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MediaService.class);
            intent.setAction("ACTION_NEXT");
            startService(intent);
        });

        findViewById(R.id.previousButton).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MediaService.class);
            intent.setAction("ACTION_PREVIOUS");
            startService(intent);
        });

        findViewById(R.id.collapsedBtn).setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        });

        findViewById(R.id.collapsed_layout).setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });

        findViewById(R.id.play_pause_button).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MediaService.class);
            intent.setAction("ACTION_PLAY_PAUSE");
            startService(intent);
        });
        findViewById(R.id.playButton).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MediaService.class);
            intent.setAction("ACTION_PLAY_PAUSE");
            startService(intent);
        });



        Handler handler = new Handler();
        Runnable stopServiceRunnable;
        findViewById(R.id.bottom_sheet).setVisibility(View.GONE);


        stopServiceRunnable = new Runnable() {
                @Override
                public void run() {
                    Intent serviceIntent = new Intent(MainActivity.this, MediaService.class);
                    bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
                }
            };
            handler.postDelayed(stopServiceRunnable, 2600);
        stopServiceRunnable = new Runnable() {
                @Override
                public void run() {
                    if (hideBottomSheetCheck){
                        findViewById(R.id.bottom_sheet).setVisibility(View.GONE);
                    } else {
                        findViewById(R.id.bottom_sheet).setVisibility(View.VISIBLE);
                    }
                }
            };
            handler.postDelayed(stopServiceRunnable, 3000);

        restoreSurahState();
        findViewById(R.id.share).setOnClickListener(v -> shareAppLink());


    }
    private void shareAppLink() {
        String appLink = "https://play.google.com/store/apps/details?id=com.aap.quraankareem&hl=ar";
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "تحميل تطبيق قرآن كريم");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "قم بتحميل تطبيق تلاوات الآن:\n" + appLink);
        startActivity(Intent.createChooser(shareIntent, "مشاركة التطبيق عبر"));
    }
    private void showTimerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("مدة التلاوة");

        String[] durations = {"5 دقائق", "10 دقائق", "20 دقيقة", "30 دقيقة",
                "ساعة", "3 ساعات", "5 ساعات", "10 ساعات", "يوم"};

        builder.setItems(durations, (dialog, which) -> {
            switch (which) {
                case 0: startTimer(5 * 60 * 1000); break;
                case 1: startTimer(10 * 60 * 1000); break;
                case 2: startTimer(20 * 60 * 1000); break;
                case 3: startTimer(30 * 60 * 1000); break;
                case 4: startTimer(60 * 60 * 1000); break;
                case 5: startTimer(3 * 60 * 60 * 1000); break;
                case 6: startTimer(5 * 60 * 60 * 1000); break;
                case 7: startTimer(10 * 60 * 60 * 1000); break;
                case 8: startTimer(24 * 60 * 60 * 1000); break;
            }
        });

        builder.show();
    }

    private void showActiveTimerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("الوقت المتبقي");
        builder.setMessage("متبقي: " + timerTextView.getText());

        builder.setPositiveButton("إلغاء", (dialog, which) -> {
            cancelTimer();
            dialog.dismiss();
        });


        builder.show();
    }

    private void startTimer(long millisInFuture) {
        // إخفاء الزر وإظهار المؤقت
        findViewById(R.id.timeSelectorButton).setVisibility(View.GONE);
        findViewById(R.id.timer).setVisibility(View.VISIBLE);

        timeLeftInMillis = millisInFuture;

        countDownTimer = new CountDownTimer(millisInFuture, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerText();
            }

            @Override
            public void onFinish() {
                timerRunning = false;

                finishAffinity();
                mediaService.onDestroy();
                Intent intent = new Intent(MainActivity.this, MediaService.class);
                mediaService.stopService(intent);

            }
        }.start();

        timerRunning = true;
    }

    private void updateTimerText() {
        int hours = (int) (timeLeftInMillis / 1000) / 3600;
        int minutes = (int) ((timeLeftInMillis / 1000) % 3600) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;

        String timeFormatted;
        if (hours > 0) {
            timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            timeFormatted = String.format("%02d:%02d", minutes, seconds);
        }

        timerTextView.setText(timeFormatted);
    }

    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            timerRunning = false;
            // إعادة إظهار العناصر
            findViewById(R.id.timeSelectorButton).setVisibility(View.VISIBLE);
            findViewById(R.id.timer).setVisibility(View.GONE);
            // إيقاف السيرفيس
            mediaService.stopSelf();
        }
    }
    private void removeSurahListFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        // البحث عن الفراغمنت باستخدام الـ Container ID الصحيح
        Fragment surahListFragment = fragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main);

        if (surahListFragment != null && surahListFragment instanceof SurahListFragment) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.remove(surahListFragment);
            transaction.commit();

            // إزالة الفراغمنت من الـ BackStack إذا لزم الأمر
            fragmentManager.popBackStack("surah_list_tag", FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    private void removeSurahFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment surahFragment = fragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main);

        if (surahFragment != null && surahFragment instanceof SurahFragment) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.remove(surahFragment);
            transaction.commit();
        }
    }
    private void removeRecitationsFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment recitationFragment = fragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main);

        if (recitationFragment != null && recitationFragment instanceof RecitationsFragment) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.remove(recitationFragment);
            transaction.commit();
        }
    }



    @Override
    public void expandBottomSheet() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        findViewById(R.id.expanded_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.collapsed_layout).setVisibility(View.GONE);
        expand = true;
    }

    @Override
    public void collapseBottomSheet() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        findViewById(R.id.expanded_layout).setVisibility(View.GONE);
        findViewById(R.id.collapsed_layout).setVisibility(View.VISIBLE);
        expand = false;
    }



    @Override
    public void updateBottomSheetContent(String surahName, String readerName, String recitationName) {
        if (surahNameTextView != null || readerNameTextView != null) {
            surahNameTextView.setText("سورة " + surahName + " - " + readerName + " - " + recitationName);
            readerNameTextView.setText(readerName + " - " + recitationName);
            surahNameTv.setText("سورة " + surahName);
            surahNameTextView.setSelected(true);

        }else {
            Log.e("MainActivity", "TextViews are null! Cannot update bottom sheet.");
        }

    }
    private void hideBottomSheet() {
        runOnUiThread(() -> {
            Log.d("MainActivity", "Hiding BottomSheet...");
            bottomSheet.setVisibility(View.GONE);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        });
    }

    @Override
    public void showBottomSheet() {

        findViewById(R.id.bottom_sheet).setVisibility(View.VISIBLE);
        expand = true;

        Log.d("MainActivity", "Showing BottomSheet...");
        // عند التوسيع
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        findViewById(R.id.collapsed_layout).setVisibility(View.GONE);
        findViewById(R.id.expanded_layout).setVisibility(View.VISIBLE);
    }
    @Override
    public void updatePlayPauseButton(boolean isPlaying) {
        ImageView closeButton = findViewById(R.id.playButton);
        ImageView playPauseButton = findViewById(R.id.play_pause_button);

        float rotationAngle = isPlaying ? 0 : 360f;
        ObjectAnimator rotation = ObjectAnimator.ofFloat(closeButton, "rotation", rotationAngle);
        rotation.setDuration(300);
        rotation.start();

        closeButton.postDelayed(() -> {
            if (isPlaying) {
                closeButton.setImageResource(R.drawable.baseline_pause_24);
                playPauseButton.setImageResource(R.drawable.baseline_pause_24);
            } else {
                closeButton.setImageResource(R.drawable.baseline_play_arrow_24);
                playPauseButton.setImageResource(R.drawable.baseline_play_arrow_24);
            }
        }, 150); // تغيير الأيقونة في منتصف الدوران للحصول على تأثير سلس
    }

    private BroadcastReceiver seekBarReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("UPDATE_REPEAT".equals(intent.getAction())) {
                isRepeatEnabled = intent.getBooleanExtra("isRepeatEnabled", false);
                Log.d("MediaService", "Repeat updated: " + isRepeatEnabled);
            } else if ("UPDATE_SHUFFLE".equals(intent.getAction())) {
                isShuffleEnabled = intent.getBooleanExtra("isShuffleEnabled", false);
                Log.d("MediaService", "Shuffle updated: " + isShuffleEnabled);
            }
            if ("UPDATE_SEEK_BAR".equals(intent.getAction())) {
                int currentPosition = intent.getIntExtra("currentPosition", 0);
                int duration = intent.getIntExtra("duration", 0);

                runOnUiThread(() -> {
                    if (seekBar != null) {
                        seekBar.setMax(duration);
                        seekBar.setProgress(currentPosition);
                    }
                    if (timeText != null) {
                        timeText.setText(formatTime(currentPosition));
                    }
                    if (timeSurah != null) {
                        timeSurah.setText(formatTime(duration));
                    }
                });
            }
        }
    };


    @Override
    public void onBackPressed() {
        if (expand) {
            collapseBottomSheet();
            return;
        }

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        if (navController.getCurrentDestination() == null) return;

        int currentDestinationId = navController.getCurrentDestination().getId();

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment surahFragment = fragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main);
        Fragment surahListFragment = fragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main);
        Fragment recitationsFragment = fragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main);

        if ((surahFragment instanceof SurahFragment)) {
            Toast.makeText(mediaService, "مش خارج وبمزاجي 😝", Toast.LENGTH_SHORT).show();
        } else if ((surahListFragment instanceof SurahListFragment)) {
            removeSurahListFragment();
            navController.navigate(R.id.navigation_dashboard);
        } else if ((recitationsFragment instanceof  RecitationsFragment)) {
            removeRecitationsFragment();
            navController.navigate(R.id.navigation_home);
        }
        // إذا كان في HomeFragment
        else if (currentDestinationId == R.id.navigation_home) {
            // تفعيل الضغط المزدوج للخروج
            if (doubleBackToExitPressedOnce) {
                finishAffinity(); // إغلاق التطبيق تمامًا
                mediaService.onDestroy();
                Intent intent = new Intent(this, MediaService.class);
                mediaService.stopService(intent);
                return;
            }
            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "اضغط مرة أخرى للخروج", Toast.LENGTH_SHORT).show();
            new Handler(Looper.getMainLooper()).postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
        }
        else {
            navController.navigate(R.id.navigation_home);
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Handler handler = new Handler();
        Runnable stopServiceRunnable;
        if (!isPlaying2) {
            Log.d("", "!is Playing");
            Intent intent = new Intent(this, MediaService.class);
            stopServiceRunnable = new Runnable() {
                @Override
                public void run() {
                    mediaService.stopService(intent);
                    Log.d("", "Service stopped after 1/2 hour");
                }
            };

            handler.postDelayed(stopServiceRunnable, 1000000); // 1 ساعة
        }
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;

        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void restoreSurahState() {
        SharedPreferences prefs = getSharedPreferences("PlayerPrefs", MODE_PRIVATE);
        String surahName = prefs.getString("surahName", null);
        String readerName = prefs.getString("readerName", null);
        String recitationName = prefs.getString("recitationName", null);
        String mediaPlayer = prefs.getString("mediaPlayer", null);
        String surahUrl = prefs.getString("surahUrl", null);
        String currentPosition = prefs.getString("currentPosition", null);
        boolean isPlaying = prefs.getBoolean("isPlaying", false);
        String duration = prefs.getString("duration", null);


        // إضافة Log للتحقق من القيم عند الاستعادة
        Log.d("MainActivity", "Restored Surah State - Name: " + surahName + ", Reader: " + readerName + ", isPlaying: " + isPlaying);
        Log.d("MainActivity", "Restored Surah State - mediaPlayer: " + mediaPlayer);


        if (surahName == null || readerName == null || surahUrl == null || duration == null || currentPosition == null || mediaPlayer == null) {
            Log.d("MainActivity", "No saved surah found. Hiding BottomSheet.");
            hideBottomSheet();
            if (mediaService != null) {
                stopService(new Intent(MainActivity.this, MediaService.class));
            } else {
                Log.e("MainActivity", "mediaService is null, cannot stop service.");
            }


        } else {
            Log.d("MainActivity", "Surah found. Showing BottomSheet." + readerName +"\n"+ surahUrl+"\n" + duration+"\n" + currentPosition+"\n" + mediaPlayer);
            updateBottomSheetContent(surahName, readerName, recitationName);
            updatePlayPauseButton(isPlaying);
        }
    }
    private BroadcastReceiver hideBottomSheetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            runOnUiThread(() -> {
                if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    bottomSheet.setVisibility(View.GONE);
                    Log.d("MainActivity", "BottomSheet hidden after MediaService stopped.");

                    hideBottomSheetCheck = true;
                }

            });
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("UPDATE_SEEK_BAR");
        LocalBroadcastManager.getInstance(this).registerReceiver(seekBarReceiver, filter);
        IntentFilter filter2 = new IntentFilter("UPDATE_SURAH_INFO");
        LocalBroadcastManager.getInstance(this).registerReceiver(surahInfoReceiver, filter2);

        IntentFilter filter3 = new IntentFilter("PLAY_SURAH_ACTION");
        LocalBroadcastManager.getInstance(this).registerReceiver(playSurahReceiver, filter3);

        IntentFilter filter4 = new IntentFilter("HIDE_BOTTOM_SHEET_ACTION");
        LocalBroadcastManager.getInstance(this).registerReceiver(hideBottomSheetReceiver, filter4);
        if (timerRunning) {
            findViewById(R.id.timeSelectorButton).setVisibility(View.GONE);
            findViewById(R.id.timer).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.timeSelectorButton).setVisibility(View.VISIBLE);
            findViewById(R.id.timer).setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(seekBarReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(surahInfoReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(playSurahReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(hideBottomSheetReceiver);
    }


    private BroadcastReceiver playSurahReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("PLAY_SURAH_ACTION".equals(intent.getAction())) {
                showBottomSheet();
            }
        }
    };

    private String formatTime(int millis) {
        int hours = millis / (1000 * 60 * 60);
        int minutes = (millis / (1000 * 60)) % 60;
        int seconds = (millis / 1000) % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaService.LocalBinder binder = (MediaService.LocalBinder) service;
            mediaService = binder.getService();
            mediaService.setBottomSheetController(MainActivity.this);  // تمرير التحكم
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };
    public void navigateToSurahListFragment(String folderName, String recitationName) {
        // إنشاء الفراجمنت الجديد
        SurahListFragment surahListFragment = new SurahListFragment();

        // تمرير البيانات إلى الفراجمنت الجديد
        Bundle args = new Bundle();
        args.putString("folder_name", folderName);
        args.putString("recitationName", recitationName);
        surahListFragment.setArguments(args);

        // التنقل إلى الفراجمنت الجديد باستخدام FragmentManager
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // استبدال الحاوية بالفراجمنت الجديد
        transaction.replace(R.id.nav_host_fragment_activity_main, surahListFragment);
        transaction.addToBackStack(null); // إضافة إلى BackStack
        transaction.commit();

    }

    public void navigateToSurahFragment(String baseUrl, String readerName, String recitationName, List<Integer> surahList) {
        SurahFragment fragment = SurahFragment.newInstance(baseUrl, readerName, recitationName, surahList);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment_activity_main, fragment)
                .addToBackStack(null)
                .commit();
    }


    private BroadcastReceiver surahInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("UPDATE_SURAH_INFO".equals(intent.getAction())) {
                String updatedSurahName = intent.getStringExtra("surahName");
                String updatedReaderName = intent.getStringExtra("readerName");
                String updatedRecitationName = intent.getStringExtra("recitationName");
                Log.d("MainActivity", "Received Surah Update: " + updatedSurahName + " - " + updatedReaderName +" - "+ updatedRecitationName);
                updateBottomSheetContent(updatedSurahName, updatedReaderName, updatedRecitationName);
            }
        }
    };
    public void navigateToRecitationsFragment(Reader reader) {  // تأكد من أن المُعامل من نوع Reader
        RecitationsFragment fragment = RecitationsFragment.newInstance(reader);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment_activity_main, fragment)
                .addToBackStack(null)
                .commit();
    }
}