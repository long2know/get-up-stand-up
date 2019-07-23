package com.long2know.standupforhealth.services;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.long2know.standupforhealth.BuildConfig;
import com.long2know.utilities.helpers.helper;
import com.long2know.utilities.models.Config;

import static androidx.constraintlayout.widget.Constraints.TAG;

public class TransitionsListener implements Runnable {
    private final String TRANSITIONS_RECEIVER_ACTION = BuildConfig.APPLICATION_ID + "TRANSITIONS_RECEIVER_ACTION";
    private PendingIntent _pendingIntent;
    private TransitionsReceiver _transitionsReceiver;

    public static Handler WorkerHandler;
    private Handler _handler;
    private boolean _isStarted = true;

    // Defines the code to run for this task.
    @Override
    public void run() {
        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        Looper.prepare();
        _handler = Config.handler;

        WorkerHandler = new Handler(Looper.myLooper()) {
            public void handleMessage(Message msg) {
                Log.d(TAG, "Received a message!");
                // For now, the only messages are start/stop
                if (msg.what == 0) {
                    // The sensors are started by default
                    if (_isStarted) {
                        stopListeners();
                        Looper.myLooper().quit();
                        _isStarted = false;
                    }
                } else {
                    if (!_isStarted) {
                        startListeners();
                        _isStarted = true;
                    }
                }
            }
        };

        startListeners();
        Looper.loop();
    }

    public void startListeners() {
        Intent intent = new Intent(TRANSITIONS_RECEIVER_ACTION);
        _pendingIntent = PendingIntent.getBroadcast(Config.context, 0, intent, 0);

        _transitionsReceiver = new TransitionsReceiver();
        Config.context.registerReceiver(_transitionsReceiver, new IntentFilter(TRANSITIONS_RECEIVER_ACTION));

        helper.register(_pendingIntent, Config.context);
    }

    public void stopListeners() {
        if (_transitionsReceiver != null) {
            Config.context.unregisterReceiver(_transitionsReceiver);
            _transitionsReceiver = null;
        }
    }

}
