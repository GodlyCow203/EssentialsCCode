package net.godlycow.org.essc.migration;

import java.util.concurrent.atomic.AtomicInteger;

public class MigrationProgress {
    private final AtomicInteger totalUsers = new AtomicInteger(0);
    private final AtomicInteger processedUsers = new AtomicInteger(0);
    private final AtomicInteger totalWarps = new AtomicInteger(0);
    private final AtomicInteger processedWarps = new AtomicInteger(0);
    private final AtomicInteger totalBans = new AtomicInteger(0);
    private final AtomicInteger processedBans = new AtomicInteger(0);

    private volatile String currentStage = "Initializing";
    private volatile long startTime = System.currentTimeMillis();
    public void setTotalUsers(int count) {
        totalUsers.set(count);
    }
    public void setTotalWarps(int count) {
        totalWarps.set(count);
    }
    public void setTotalBans(int count) {
        totalBans.set(count);
    }

    public void setStage(String stage) {
        this.currentStage = stage;
    }
    public void incrementUsers() {
        processedUsers.incrementAndGet();
    }
    public void incrementWarps() {
        processedWarps.incrementAndGet();
    }
    public void incrementBans() {
        processedBans.incrementAndGet();
    }

    public String getCurrentStage() {
        return currentStage;
    }
    public int getProcessedUsers() {
        return processedUsers.get();
    }
    public int getTotalUsers() {
        return totalUsers.get();
    }
    public int getProcessedWarps() {
        return processedWarps.get();
    }
    public int getTotalWarps() {
        return totalWarps.get();
    }

    public int getPercentComplete() {
        int total = totalUsers.get() + totalWarps.get() + totalBans.get();
        if (total == 0) return 0;
        int processed = processedUsers.get() + processedWarps.get() + processedBans.get();
        return (processed * 100) / total;
    }

    public long getElapsedTimeMs() {
        return System.currentTimeMillis() - startTime;
    }

    public String getEstimatedTimeRemaining() {
        int percent = getPercentComplete();
        if (percent == 0) return "Calculating...";

        long elapsed = getElapsedTimeMs();
        long total = (elapsed * 100) / percent;
        long remaining = total - elapsed;

        if (remaining < 60000) return remaining / 1000 + "s";
        return remaining / 60000 + "m " + (remaining % 60000) / 1000 + "s";
    }
}