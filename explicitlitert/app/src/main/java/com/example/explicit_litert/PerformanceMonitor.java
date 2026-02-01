package com.example.explicit_litert;
import android.content.Context;
import android.os.Debug;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PerformanceMonitor {
    private static final String TAG = "perfmongpteam";
    private ScheduledExecutorService scheduler;
    private volatile boolean isLogging = false;
    private int secondsCounter = 0;

    public PerformanceMonitor(Context context) {
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void startMonitoring() {
        secondsCounter = 0;
        scheduler.scheduleWithFixedDelay(this::logMetrics, 0, 1, TimeUnit.SECONDS);
        Log.i(TAG, "Performance monitoring started (Logcat only)");
    }

    private void logMetrics() {
        if (isLogging) return;
        isLogging = true;

        try {
            Debug.MemoryInfo memInfo = new Debug.MemoryInfo();
            Debug.getMemoryInfo(memInfo);

            double memoryMB = memInfo.getTotalPss() / 1024.0;

            Log.i(
                    TAG,
                    String.format(
                            Locale.US,
                            "t=%ds | Memory=%.2f MB",
                            secondsCounter++, memoryMB
                    )
            );
        } finally {
            isLogging = false;
        }
    }

    public void stopMonitoring() {
        scheduler.shutdownNow();
        Log.i(TAG, "Performance monitoring stopped");
    }
}

