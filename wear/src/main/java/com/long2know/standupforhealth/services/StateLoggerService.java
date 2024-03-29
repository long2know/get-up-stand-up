package com.long2know.standupforhealth.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.long2know.standupforhealth.MainActivity;
import com.long2know.standupforhealth.R;
import com.long2know.utilities.data_access.SqlLogger;
import com.long2know.utilities.models.Config;
import com.long2know.utilities.models.SharedData;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static androidx.constraintlayout.widget.Constraints.TAG;

public class StateLoggerService extends Service {

    private NotificationManager _notificationManager;
    private final IBinder _binder = new LocalBinder();
    public static ILoggerServiceClient _serviceClient;

    private Thread _transitionsThread;
    private TransitionsListener _transitionsListener;

    private ScheduledExecutorService _scheduler;
    private StopWatch _stopWatch = new StopWatch();

    // Below is the service framework methods
    @Override
    public void onCreate() {
        super.onCreate();

        Config.context = this;

        // Pass through any messages
        Config.handler = new Handler(Looper.getMainLooper()) {
            public void handleMessage(Message msg) {
                if (Config.activityHandler != null) {
                    Message completeMessage = Config.activityHandler.obtainMessage(msg.what, msg.arg1, msg.arg2, msg.obj);
                    completeMessage.sendToTarget();
                }
            }
        };

        _notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        _transitionsListener = new TransitionsListener();
        _transitionsThread = new Thread(_transitionsListener);
        _transitionsThread.start();

//        startLoggerService();
//
//        // Display a notification about us starting. We put an icon in the
//        // status bar.
        showNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return _binder;
    }

    @Override
    public void onDestroy() {
        if (_scheduler != null) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                try {
                    _scheduler.awaitTermination(100, TimeUnit.MILLISECONDS);
                    _scheduler.shutdownNow();
                } catch (InterruptedException e) { }
                }
            });
        }

        Message tmsg = _transitionsListener.WorkerHandler.obtainMessage(0);
        _transitionsListener.WorkerHandler.sendMessage(tmsg);
        _transitionsThread.interrupt();

        _serviceClient = null;
        super.onDestroy();
    }

    private void showNotification() {
        // Open the app when notification is clicked
        Intent contentIntent = new Intent(this, MainActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pending = PendingIntent.getActivity(getBaseContext(), 0, contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        String channelId = "long2know_standup_for_health";
        CharSequence name = "long2know_channel";
        NotificationChannel channel = new NotificationChannel(channelId, name,NotificationManager.IMPORTANCE_DEFAULT);
        _notificationManager.createNotificationChannel(channel);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setTicker("Hearty365")
                .setContentTitle("StandupForHealth is running")
                .setContentText("Running in the background - tap notification to return to app.")
                .setContentInfo("Info")
                .setSmallIcon(R.drawable.ic_play_circle_outline_black_24dp)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(pending);

        try {
            startForeground(1, notificationBuilder.build());
//            _notificationManager.notify(1, notificationBuilder.build());
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    public static void setServiceClient(ILoggerServiceClient client) {
        _serviceClient = client;
    }

    /**
     * Class for clients to access. Because we know this service always runs in
     * the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public StateLoggerService getService() {
            return StateLoggerService.this;
        }
    }

    public void startMonitor() {
        // We can force reading at specific intervals like this
        //_scheduler = Executors.newScheduledThreadPool(1);
        //_scheduler.scheduleAtFixedRate(new SqlLogger(), 0, 1, TimeUnit.SECONDS);
        _stopWatch.startTImer();
        SharedData.getInstance().IsRecording = true;
        SharedData.getInstance().IsPaused = false;
        CharSequence text = "Starting stand-up for health monitor";
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    public void stopMonitor() {
        // We don't want to block the UI
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    _scheduler.awaitTermination(300, TimeUnit.MILLISECONDS);
                    _scheduler.shutdownNow();
                } catch (InterruptedException e) {
                }
            }
        });

        _stopWatch.pauseTimer();
        _stopWatch.resetTimer();

        CharSequence text = "Stopped activity";
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        SharedData.getInstance().IsRecording = false;
        SharedData.getInstance().IsPaused = false;
    }

    public void pauseActivity() {
        // We don't want to block the UI
        SharedData.getInstance().IsPaused = true;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    _scheduler.awaitTermination(300, TimeUnit.MILLISECONDS);
                    _scheduler.shutdownNow();
                } catch (InterruptedException e) {
                }
            }
        });

        _stopWatch.pauseTimer();
        CharSequence text = "Paused monitoring";
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    public void resumeActivity() {
        // We can force reading at specific intervals like this
        _scheduler = Executors.newScheduledThreadPool(1);
        _scheduler.scheduleAtFixedRate(new SqlLogger(), 0, 1, TimeUnit.SECONDS);
        SharedData.getInstance().IsPaused = false;
        _stopWatch.startTImer();
        CharSequence text = "Resuming monitoring";
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    public void discardActivity() {
        SharedData.getInstance().IsRecording = false;
        SharedData.getInstance().IsPaused = false;

        // We don't want to block the UI
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    _scheduler.awaitTermination(300, TimeUnit.MILLISECONDS);
                    _scheduler.shutdownNow();
                } catch (InterruptedException e) {
                }
                int id = SharedData.getInstance().ActivityId;

                SqlLogger sqlLogger = new SqlLogger();
                sqlLogger.deleteActivity(id);
            }
        });

        _stopWatch.pauseTimer();
        _stopWatch.resetTimer();

        CharSequence text = "Discarded activity";
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
