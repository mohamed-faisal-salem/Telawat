package com.aap.quraankareem;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MediaService extends Service {
    private BroadcastReceiver settingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("UPDATE_REPEAT".equals(intent.getAction())) {
                isRepeatEnabled = intent.getBooleanExtra("isRepeatEnabled", false);
                Log.d("MediaService", "Repeat Mode Updated: " + isRepeatEnabled);
            } else if ("UPDATE_SHUFFLE".equals(intent.getAction())) {
                isShuffleEnabled = intent.getBooleanExtra("isShuffleEnabled", false);
                Log.d("MediaService", "Shuffle Mode Updated: " + isShuffleEnabled);
            }
        }
    };

    private MediaPlayer mediaPlayer;
    private final IBinder binder = new LocalBinder();
    private static final String CHANNEL_ID = "MediaChannel";
    private static final int NOTIFICATION_ID = 1;
    private MediaSessionCompat mediaSession;
    private Handler handler; // لتحديث SeekBar باستمرار
    private Runnable updateSeekBarTask;
    private AudioManager audioManager;
    Boolean requestAudioFocus = false;
    private boolean isRepeatEnabled = false;
    private boolean isShuffleEnabled = false;
    private List<Surah> surahList = new ArrayList<>();
    private BottomSheetController bottomSheetController;
    int surahPosition;
    String currentSurah_position, surahName, readerName, recitationName;

    private Handler stopServiceHandler = new Handler();
    private Runnable stopServiceRunnable;
    private boolean isPaused = false;


    public class LocalBinder extends Binder {
        MediaService getService() {
            return MediaService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        if (surahList == null || surahList.isEmpty()) {
            Log.e("MediaService", "surahList is empty!");
            sendHideBottomSheetBroadcast(); // إرسال إشارة لإخفاء البوتوم شيت
        }

        if (surahPosition < 0 || surahPosition >= surahList.size()) {
            Log.e("MediaService", "Invalid surahPosition: " + surahPosition);
            sendHideBottomSheetBroadcast(); // إرسال إشارة لإخفاء البوتوم شيت
        }

        if (surahList != null && !surahList.isEmpty()) {
            Log.d("MediaService", "surahList is loaded with " + surahList.size() + " items");
        } else {
            Log.e("MediaService", "surahList is empty or null");
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("UPDATE_REPEAT");
        filter.addAction("UPDATE_SHUFFLE");

        LocalBroadcastManager.getInstance(this).registerReceiver(settingsReceiver, filter);

        if (getApplicationContext() instanceof BottomSheetController) {
            bottomSheetController = (BottomSheetController) getApplicationContext();
        }
        createNotificationChannel();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(null);


        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        AudioFocusRequest audioFocusRequest = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setOnAudioFocusChangeListener(new AudioManager.OnAudioFocusChangeListener() {
                        @Override
                        public void onAudioFocusChange(int focusChange) {
                            handleAudioFocusChange(focusChange);
                        }
                    })
                    .setAcceptsDelayedFocusGain(true)
                    .setWillPauseWhenDucked(true)
                    .build();
        }

        int result = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            result = audioManager.requestAudioFocus(audioFocusRequest);
        }

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d("AudioFocus", "AudioFocus granted");
            requestAudioFocus = true;
        } else {
            Log.e("AudioFocus", "AudioFocus request failed");
            requestAudioFocus = false;
        }

        mediaSession = new MediaSessionCompat(this, "MediaService");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSessionCallback());
        mediaSession.setActive(true);
        handler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String surahUrl = intent.getStringExtra("surahUrl");
        String action = intent.getAction();
        surahName = intent.getStringExtra("surahName");
        readerName = intent.getStringExtra("readerName");
        recitationName = intent.getStringExtra("recitationName");

        SharedPreferences prefs = getSharedPreferences("PlayerPrefs", MODE_PRIVATE);
        surahName = prefs.getString("surahName", surahName);
        readerName = prefs.getString("readerName", readerName);
        recitationName = prefs.getString("recitationName", recitationName);
        currentSurah_position = intent.getStringExtra("currentSurahPosition");
        if (currentSurah_position != null) {
            surahPosition = Integer.parseInt(currentSurah_position) - 1;
            Log.d("", "SurahPosition" + surahPosition);
        }
        if (surahPosition == -1){
            surahPosition++;
        }
        // استقبال surahList من Intent
        if (intent.hasExtra("surahList")) {
            surahList = intent.getParcelableArrayListExtra("surahList");
            Log.d("MediaService", "surahList received with " + surahList.size() + " items");
        } else {
            Log.e("MediaService", "surahList is missing in Intent");
        }

        if (surahUrl != null) {
            audioManager.abandonAudioFocus(null);

            surahName = intent.getStringExtra("surahName");
            readerName = intent.getStringExtra("readerName");
            recitationName = intent.getStringExtra("recitationName");
            playSurah(surahUrl);
        }
            bottomSheetController.updatePlayPauseButton(mediaPlayer.isPlaying());


        if (action != null) {
            switch (action) {
                case "ACTION_PLAY_PAUSE":
                    if (mediaPlayer.isPlaying()) {
                        pauseSurah();
                    } else {
                        resumeSurah();
                    }
                    if (bottomSheetController != null) {
                        bottomSheetController.updatePlayPauseButton(mediaPlayer.isPlaying());
                    }
                    updateNotification(surahName, mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration());
                    break;
                case "ACTION_NEXT":
                    playNextSurah();
                    break;
                case "ACTION_PREVIOUS":
                    playPreviousSurah();
                    break;
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    private void playSurah(String surahUrl) {
        try {
            if (mediaPlayer != null) {

                audioManager.abandonAudioFocus(null);

                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();

                mediaPlayer = new MediaPlayer(); // إنشاء نسخة جديدة


                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                int result = audioManager.requestAudioFocus(
                        focusChange -> handleAudioFocusChange(focusChange),
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN
                );

                if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    Log.e("MediaPlayer", "AudioFocus request failed");
                    Toast.makeText(this, "تعذر الحصول على التركيز الصوتي", Toast.LENGTH_SHORT).show();
                    return;
                }

                mediaPlayer.setDataSource(surahUrl);

                mediaPlayer.prepareAsync();

                mediaPlayer.setOnPreparedListener(mp -> {
                    startForeground(NOTIFICATION_ID, createNotification(surahName, mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration()));
                    mediaPlayer.start();
                    if (bottomSheetController != null) {
                        bottomSheetController.showBottomSheet();
                        bottomSheetController.updateBottomSheetContent(surahName, readerName, recitationName);
                        bottomSheetController.updatePlayPauseButton(true);
                    }

                    updateNotification(surahName, 0, mediaPlayer.getDuration());
                    saveCurrentSurahState(surahName, readerName, surahUrl, recitationName);
                    startSeekBarUpdater();
                    int duration = mediaPlayer.getDuration();
                    Intent updateIntent = new Intent("UPDATE_SEEK_BAR");
                    updateIntent.putExtra("currentPosition", 0);
                    updateIntent.putExtra("duration", duration);
                    LocalBroadcastManager.getInstance(MediaService.this).sendBroadcast(updateIntent);


                    sendSurahUpdateToMainActivity();
                });

                mediaPlayer.setOnCompletionListener(mp -> {
                    Log.d("MediaService", "Playing next surah. Repeat: " + isRepeatEnabled + ", Shuffle: " + isShuffleEnabled);

                    if (isRepeatEnabled) {
                        playSurah(surahList.get(surahPosition).getSurahUrl());
                    } else if (isShuffleEnabled) {
                        surahPosition = new Random().nextInt(surahList.size());
                        Surah shuffledSurah = surahList.get(surahPosition);
                        playSurah(shuffledSurah.getSurahUrl());

                        // تحديث اسم السورة والقارئ
                        surahName = shuffledSurah.getSurahName();
                        readerName = shuffledSurah.getReaderName();
                        recitationName = shuffledSurah.getRecitationName();

                        // تحديث الإشعار
                        updateNotification(surahName,mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration());

                        // إرسال تحديث إلى MainActivity
                        sendSurahUpdateToMainActivity();
                    }
                    else {
                        playNextSurah();
                    }
                });

                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e("MediaPlayer", "Error occurred: what=" + what + ", extra=" + extra);
                    Log.e("MediaPlayer", "Current Surah: " + surahName + ", Position: " + surahPosition);
                    return true;
                });

            } else {
                Log.e("MediaPlayer", "MediaPlayer is null");
            }
        } catch (Exception e) {
            Log.e("MediaPlayer", "Exception: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "تعذر تشغيل السورة", Toast.LENGTH_SHORT).show();
        }

        Intent updateIntent = new Intent("PLAY_SURAH_ACTION");
        LocalBroadcastManager.getInstance(MediaService.this).sendBroadcast(updateIntent);
    }


    private void handleAudioFocusChange(int focusChange) {
        if (mediaPlayer != null) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    Log.d("AudioFocus", "AudioFocus lost permanently");
                    if (mediaPlayer.isPlaying()) { // تأكد من الحالة
                        pauseSurah();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    Log.d("AudioFocus", "AudioFocus lost temporarily");
                    if (mediaPlayer.isPlaying()) { // تأكد من الحالة
                        pauseSurah();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    Log.d("AudioFocus", "AudioFocus gained");

                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    Log.d("AudioFocus", "AudioFocus lost temporarily, ducking");
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.setVolume(0.5f, 0.5f);
                    }
                    break;
            }
        }
    }
    private void playNextSurah() {
        if (surahList == null || surahList.isEmpty()) {
            Log.e("MediaService", "surahList is empty!");
            sendErrorBroadcast();
            return;
        }


        if (surahPosition < surahList.size() - 1) {
            surahPosition++;
        } else {
            surahPosition = 0; // العودة إلى البداية
        }

        Surah nextSurah = surahList.get(surahPosition);
        if (nextSurah != null && nextSurah.getSurahUrl() != null) {
            playSurah(nextSurah.getSurahUrl());
            surahName = nextSurah.getSurahName();
            readerName = nextSurah.getReaderName();
            recitationName = nextSurah.getRecitationName();
            updateNotification(surahName, 0, mediaPlayer.getDuration());

            if (bottomSheetController != null) {
                bottomSheetController.updateBottomSheetContent(surahName, readerName, recitationName);
            }
        } else {
            Log.e("MediaService", "Invalid Surah data at position: " + surahPosition);
            sendErrorBroadcast();
        }
    }

    private void playPreviousSurah() {
        if (surahList == null || surahList.isEmpty()) {
            Log.e("MediaService", "surahList is empty!");
            sendErrorBroadcast();
            return;
        }

        // التحقق من صحة الفهرس وإعادة التعيين إذا لزم الأمر
        if (surahPosition < 0 || surahPosition >= surahList.size()) {
            surahPosition = 0;
        }

        // تقليل الفهرس مع التحقق من الحدود
        if (surahPosition > 0) {
            surahPosition--;
        } else {
            surahPosition = surahList.size() - 1; // الانتقال إلى النهاية
        }

        Surah previousSurah = surahList.get(surahPosition);
        if (previousSurah != null && previousSurah.getSurahUrl() != null) {
            playSurah(previousSurah.getSurahUrl());
            surahName = previousSurah.getSurahName();
            readerName = previousSurah.getReaderName();
            recitationName = previousSurah.getRecitationName();
            updateNotification(surahName, 0, mediaPlayer.getDuration());

            if (bottomSheetController != null) {
                bottomSheetController.updateBottomSheetContent(surahName, readerName, recitationName);
            }
        } else {
            Log.e("MediaService", "Invalid Surah data at position: " + surahPosition);
            sendErrorBroadcast();
        }
    }
    public void pauseSurah() {
        if (surahList != null && surahList.size() > 0 && surahPosition >= 0 && surahPosition < surahList.size()) {
            Surah surah = surahList.get(surahPosition); // استخدم الفهرس الصحيح
            saveCurrentSurahState(surahName, readerName, surah.getSurahUrl(), recitationName);
        } else {
            surahPosition = 0;
            Surah surah = surahList.get(surahPosition); // استخدم الفهرس الصحيح
            saveCurrentSurahState(surahName, readerName, surah.getSurahUrl(), recitationName);
            Log.e("MediaService", "Invalid surahPosition or surahList is empty");
        }
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            isPaused = true;
            Log.d("MediaPlayer", "Pausing playback");
            mediaPlayer.pause();
            if (bottomSheetController != null) {
                bottomSheetController.updatePlayPauseButton(mediaPlayer.isPlaying());
            }

            Log.d("MediaService", "Paused. Starting 2-hour shutdown timer...");

            // إلغاء أي مؤقت قديم وبدء العد من جديد
            scheduleServiceStop();

            // إعادة تعيين AudioFocus عند الإيقاف
            audioManager.abandonAudioFocus(null);
            Log.d("AudioFocus", "AudioFocus abandoned");
        }

        SharedPreferences prefs = getSharedPreferences("PlayerPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("isPlaying", false).apply();

        Log.d("MediaService", "surahPosition: " + surahPosition);

        Surah surah = surahList.get(surahPosition);
        saveCurrentSurahState(surahName, readerName, surah.getSurahUrl(), recitationName);

        updateNotification(surahName, mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration());
    }



    private void startSeekBarUpdater() {
        updateSeekBarTask = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) { // أرسل التحديث حتى لو كان التشغيل متوقفًا
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    int duration = mediaPlayer.getDuration();
                    updateNotification(surahName, currentPosition, duration);
                    Intent updateIntent = new Intent("UPDATE_SEEK_BAR");
                    updateIntent.putExtra("currentPosition", currentPosition);
                    updateIntent.putExtra("duration", duration);
                    LocalBroadcastManager.getInstance(MediaService.this).sendBroadcast(updateIntent);
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateSeekBarTask);
    }
    private void sendHideBottomSheetBroadcast() {
        Intent intent = new Intent("HIDE_BOTTOM_SHEET_ACTION");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d("", "kdddddkfd");
    }
    public void resumeSurah() {
        try {
            if (surahList == null || surahList.isEmpty()) {
                Log.e("MediaService", "surahList is empty!");
                sendHideBottomSheetBroadcast(); // إرسال إشارة لإخفاء البوتوم شيت
                return;
            }

            if (surahPosition < 0 || surahPosition >= surahList.size()) {
                Log.e("MediaService", "Invalid surahPosition: " + surahPosition);
                sendHideBottomSheetBroadcast(); // إرسال إشارة لإخفاء البوتوم شيت
                return;
            }

            Surah surah = surahList.get(surahPosition);
            if (surah == null || surah.getSurahUrl() == null) {
                Log.e("MediaService", "Invalid Surah data!");
                sendHideBottomSheetBroadcast(); // إرسال إشارة لإخفاء البوتوم شيت
                return;
            }

            if (surahList != null && !surahList.isEmpty() && surahPosition >= 0 && surahPosition < surahList.size()) {
                if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                    isPaused = false;
                    if (stopServiceRunnable != null) {
                        stopServiceHandler.removeCallbacks(stopServiceRunnable);
                    }
                }
                if (bottomSheetController != null) {
                    bottomSheetController.updatePlayPauseButton(mediaPlayer.isPlaying());
                }
                updateNotification(surahName, mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration()); // تحديث الإشعار عند الاستئناف
                Surah surahUrl = surahList.get(surahPosition);
                saveCurrentSurahState(surahName, readerName, surahUrl.getSurahUrl(), recitationName);
            } else {
                Log.e("MediaService", "Invalid surahList or position");
                sendErrorBroadcast(); // إرسال إشارة لإخفاء البوتوم شيت
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e("MediaService", "Index error: " + e.getMessage());
            sendErrorBroadcast(); // إرسال إشارة لإخفاء البوتوم شيت
        }
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Media Playback",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for Quran Player Notifications");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    private Notification createNotification(String title, int currentPosition, int duration) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // إجراءات التحكم: تشغيل/إيقاف، التالي، السابق
        Intent playPauseIntent = new Intent(this, MediaService.class);
        playPauseIntent.setAction("ACTION_PLAY_PAUSE");
        PendingIntent playPausePendingIntent = PendingIntent.getService(this, 1, playPauseIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, MediaService.class);
        nextIntent.setAction("ACTION_NEXT");
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 2, nextIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent previousIntent = new Intent(this, MediaService.class);
        previousIntent.setAction("ACTION_PREVIOUS");
        PendingIntent previousPendingIntent = PendingIntent.getService(this, 3, previousIntent, PendingIntent.FLAG_IMMUTABLE);

        // تنسيق الوقت
        String formattedDuration = formatTime(duration);
        String formattedCurrentPosition = formatTime(currentPosition);
        int playPauseIcon = mediaPlayer.isPlaying() ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24;
        String playPauseLabel = mediaPlayer.isPlaying() ? "إيقاف" : "تشغيل";

        // بناء الإشعار
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("سورة " + surahName) // اسم السورة الحالية
                .setContentText("القارئ: " + readerName + " - "+ recitationName) // اسم القارئ
                .setSubText(formattedCurrentPosition + "/" + formattedDuration) // الوقت الحالي/الوقت الكلي
                .setSmallIcon(R.drawable.baseline_notifications_24) // الأيقونة الصغيرة
                .setColor(ContextCompat.getColor(this, R.color.black)) // لون الإشعار
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_home_black_24dp)) // الأيقونة الكبيرة
                .setContentIntent(pendingIntent) // النقر على الإشعار
                .setPriority(NotificationCompat.PRIORITY_HIGH) // الأولوية
                .addAction(R.drawable.baseline_skip_previous_24, "السابق", previousPendingIntent) // زر السابق
                .addAction(playPauseIcon, playPauseLabel, playPausePendingIntent) // زر التشغيل/الإيقاف
                .addAction(R.drawable.baseline_skip_next_24, "التالي", nextPendingIntent) // زر التالي
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2)) // عرض الأزرار في الإشعار المضغوط
                .setOnlyAlertOnce(true) // عدم إظهار الإشعار بشكل متكرر
                .setProgress(duration, currentPosition, false); // إضافة SeekBar

        return builder.build();
    }
    private String formatTime(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds %= 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            resumeSurah();
        }

        @Override
        public void onPause() {
            pauseSurah();
        }

        @Override
        public void onSeekTo(long position) {
            if (mediaPlayer != null) {
                mediaPlayer.seekTo((int) position);
            }
        }

        @Override
        public void onSkipToNext() {
            playNextSurah(); // تشغيل السورة التالية
        }

        @Override
        public void onSkipToPrevious() {
            playPreviousSurah(); // تشغيل السورة السابقة
        }
    }
    private void sendErrorBroadcast() {
        Intent intent = new Intent("HIDE_BOTTOM_SHEET_ACTION");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && updateSeekBarTask != null) {
            handler.removeCallbacks(updateSeekBarTask);
        }
        if (mediaSession != null) {
            mediaSession.release();
        }

        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
                audioManager.abandonAudioFocus(null);
            } catch (IllegalStateException e) {
                Log.e("MediaService", "Error releasing MediaPlayer: " + e.getMessage());
            }
            mediaPlayer = null;

        }

    }
    private void updateNotification(String title, int currentPosition, int duration) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = createNotification(title, currentPosition, duration);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }
    public interface BottomSheetController {
        void updatePlayPauseButton(boolean isPlaying);
        void showBottomSheet();
        void expandBottomSheet();
        void collapseBottomSheet();
        void updateBottomSheetContent(String surahName, String readerName, String recitationName);
    }
    public void setBottomSheetController(BottomSheetController controller) {
        this.bottomSheetController = controller;
    }
    private void saveCurrentSurahState(String surahName, String readerName, String surahUrl, String recitationName) {
        if (mediaPlayer!= null) {
            SharedPreferences prefs = getSharedPreferences("PlayerPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("surahName", surahName);
            editor.putString("readerName", readerName);
            editor.putString("recitationName", recitationName);
            editor.putString("mediaPlayer", mediaPlayer.toString());
            editor.putString("surahUrl", surahUrl);
            editor.putString("currentPosition", String.valueOf(mediaPlayer.getCurrentPosition()));
            editor.putString("duration", String.valueOf(mediaPlayer.getDuration()));
            editor.putBoolean("isPlaying", mediaPlayer.isPlaying());
            editor.apply();

            // إضافة Log للتحقق من القيم عند التخزين
            Log.d("MediaService", "Saved Surah State - Name: " + surahName + ", Reader: " + readerName + ", isPlaying: " + mediaPlayer.isPlaying());

        }
    }
    public void stopServiceCompletely() {
        stopForeground(true);
        stopSelf();
        sendHideBottomSheetBroadcast();
    }

    public void setRepeatMode(boolean enabled) {
        isRepeatEnabled = enabled;
    }

    public void setShuffleMode(boolean enabled) {
        isShuffleEnabled = enabled;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }
    private void sendSurahUpdateToMainActivity() {
        Intent intent = new Intent("UPDATE_SURAH_INFO");
        intent.putExtra("surahName", surahName);
        intent.putExtra("readerName", readerName);
        intent.putExtra("recitationName", recitationName);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    public void stopMediaServiceCompletely() {

    }
    public void scheduleServiceStop() {
        if (stopServiceRunnable != null) {
            stopServiceHandler.removeCallbacks(stopServiceRunnable);
        }

        stopServiceRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPaused) { // لو لسه المستخدم مشغلش الصوت تاني
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                }
            }
        };
        stopServiceHandler.postDelayed(stopServiceRunnable, 3600000);
        Log.d("MediaService", "2 hours pass. Stopping App...");
    }

}