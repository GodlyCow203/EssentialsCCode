package net.godlycow.org.essc.bootstrap;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StartupTimer {

    private static final DecimalFormat FORMAT = new DecimalFormat("#0.000", DecimalFormatSymbols.getInstance(Locale.US));

    private final List<PhaseRecord> phases = new ArrayList<>();

    public void start() {
        phases.clear();
        record("start");
    }

    public void mark(String phase) {
        if (!phases.isEmpty()) {
            record(phase);
        }
    }

    public String finish() {
        if (phases.size() < 2) {
            return "execution time: no phases recorded";
        }

        StringBuilder output = new StringBuilder("execution time: ");
        long startTime = phases.get(0).time();
        long previousTime = startTime;

        for (int i = 1; i < phases.size(); i++) {
            PhaseRecord record = phases.get(i);
            double duration = (record.time() - previousTime) / 1_000_000.0;
            output.append(record.phase()).append(": ").append(FORMAT.format(duration)).append("ms — ");
            previousTime = record.time();
        }

        double total = (previousTime - startTime) / 1_000_000.0;
        output.append("total: ").append(FORMAT.format(total)).append("ms");
        phases.clear();
        return output.toString();
    }

    private void record(String phase) {
        phases.add(new PhaseRecord(phase, System.nanoTime()));
    }

    private record PhaseRecord(String phase, long time) {}
}