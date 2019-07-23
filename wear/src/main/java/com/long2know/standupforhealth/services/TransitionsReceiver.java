package com.long2know.standupforhealth.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.long2know.standupforhealth.BuildConfig;
import com.long2know.utilities.helpers.helper;
import com.long2know.utilities.models.Config;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransitionsReceiver extends BroadcastReceiver {
    private final String TRANSITIONS_RECEIVER_ACTION = BuildConfig.APPLICATION_ID + "TRANSITIONS_RECEIVER_ACTION";
    private Handler _handler;

    public TransitionsReceiver() {
        _handler = Config.handler;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!TextUtils.equals(TRANSITIONS_RECEIVER_ACTION, intent.getAction())) {
//            mLogFragment.getLogView()
//                    .println("Received an unsupported action in TransitionsReceiver: action="
//                            + intent.getAction());
            return;
        }
        if (ActivityTransitionResult.hasResult(intent)) {
            ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
            for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                String activity = helper.toActivityString(event.getActivityType());
                String transitionType = helper.toTransitionType(event.getTransitionType());
                String message = "Transition: "
                                + activity + " (" + transitionType + ")" + "   "
                                + new SimpleDateFormat("HH:mm:ss", Locale.US)
                                .format(new Date());

                Message completeMessage = _handler.obtainMessage(1, message);;
                completeMessage.sendToTarget();
//                mLogFragment.getLogView()
//                        .println("Transition: "
//                                + activity + " (" + transitionType + ")" + "   "
//                                + new SimpleDateFormat("HH:mm:ss", Locale.US)
//                                .format(new Date()));
            }
        }
    }
}