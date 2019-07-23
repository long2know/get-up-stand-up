package com.long2know.standupforhealth.services;

import com.long2know.utilities.models.SharedData;

// Simple interface to let the service send updates to an activity directly
public interface ILoggerServiceClient {
    void onLoggerUpdate(SharedData data);
}
