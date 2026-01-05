package com.example.explicitapp3;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.os.Process;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";
    private final Context context;
    private ScheduledExecutorService scheduler;
    private FileWriter csvWriter;
    private final int pid;
    private volatile boolean isLogging = false;
    private int secondsCounter = 0;
    // For CPU calculation
    private long lastMeasurementTime = 0;
    private long lastThreadCpuTime = 0;

    public PerformanceMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.pid = Process.myPid();
    }

    public void startMonitoring(File outputFile) {
        try {
            secondsCounter = 0;  // Reset counter
            csvWriter = new FileWriter(outputFile, true);

            if (outputFile.length() == 0) {
                csvWriter.write("Seconds,Timestamp,Memory_MB,CPU_Percent,Native_Heap_MB,Java_Heap_MB,Thread_Count\n");
            }

            scheduler.scheduleWithFixedDelay(this::logMetrics, 0, 5, TimeUnit.SECONDS);

            Log.d(TAG, "Performance monitoring started: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Failed to start monitoring: " + e.getMessage());
        }
    }

    private void logMetrics() {
        if (isLogging) {
            Log.w(TAG, "Skipping metrics - previous task still running");
            return;
        }

        isLogging = true;
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());

            // Memory metrics
            Debug.MemoryInfo debugMemInfo = new Debug.MemoryInfo();
            Debug.getMemoryInfo(debugMemInfo);

            double totalMemoryMB = debugMemInfo.getTotalPss() / 1024.0;
            double nativeHeapMB = debugMemInfo.nativePss / 1024.0;
            double javaHeapMB = debugMemInfo.dalvikPss / 1024.0;

            // CPU metrics (using thread time instead of /proc/stat)
            double cpuPercent = getThreadCpuUsage();

            // Thread count
            int threadCount = Thread.activeCount();

            // Write to CSV
            String line = String.format(Locale.US, "%d,%s,%.2f,%.2f,%.2f,%.2f,%d\n",
                    secondsCounter, timestamp, totalMemoryMB, cpuPercent, nativeHeapMB, javaHeapMB, threadCount);

            csvWriter.write(line);
            csvWriter.flush();

            Log.d(TAG, String.format("Memory: %.2f MB | CPU: %.2f%% | Threads: %d",
                    totalMemoryMB, cpuPercent, threadCount));
        } catch (IOException e) {
            Log.e(TAG, "Failed to log metrics: " + e.getMessage());
        } finally {
            isLogging = false;
        }
    }

    private double getThreadCpuUsage() {
        try {
            long currentTime = System.currentTimeMillis();

            // Get total CPU time for all app threads (in nanoseconds)
            long currentThreadCpuTime = Debug.threadCpuTimeNanos();

            if (lastMeasurementTime == 0) {
                lastMeasurementTime = currentTime;
                lastThreadCpuTime = currentThreadCpuTime;
                return 0.0;
            }

            long wallTimeDiff = currentTime - lastMeasurementTime; // milliseconds
            long cpuTimeDiff = currentThreadCpuTime - lastThreadCpuTime; // nanoseconds

            lastMeasurementTime = currentTime;
            lastThreadCpuTime = currentThreadCpuTime;

            if (wallTimeDiff == 0) return 0.0;

            // Convert cpuTimeDiff from nanoseconds to milliseconds
            double cpuTimeMs = cpuTimeDiff / 1_000_000.0;

            // Calculate percentage (normalized by thread count to get per-core usage)
            double cpuPercent = (cpuTimeMs / wallTimeDiff) * 100.0;

            // Cap at reasonable maximum (8 cores = 800% theoretical max)
            return Math.min(cpuPercent, 800.0);

        } catch (Exception e) {
            Log.e(TAG, "Error calculating CPU: " + e.getMessage());
            return 0.0;
        }
    }

    public void stopMonitoring() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }

            if (csvWriter != null) {
                csvWriter.close();
            }

            Log.d(TAG, "Performance monitoring stopped");
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Error stopping monitor: " + e.getMessage());
        }
    }
}