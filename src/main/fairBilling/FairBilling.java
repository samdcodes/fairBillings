package fairBilling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class FairBilling {

    private static LocalTime firstTime = null;
    private static LocalTime lastTime = null;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("No path to log file supplied, please supply exactly one argument path");
            System.exit(1);
        }
        List<String> lines = Files.readAllLines(Paths.get(args[0]));

        Map<String, List<LogEntry>> validEntriesMap = new HashMap<>();
        cleanInputAndPopulateEntries(lines, validEntriesMap);

        ConcurrentMap<String, Bill> fairBills = new ConcurrentHashMap<>();
        generateBillsConcurrent(validEntriesMap, fairBills);

        for(String key: fairBills.keySet()) {
            System.out.printf("%S %S %S%n", key, fairBills.get(key).getSessions(), fairBills.get(key).getTotalTimeSeconds());
        }
    }

    static void generateBillsConcurrent(Map<String, List<LogEntry>> validEntriesMap, ConcurrentMap<String, Bill> bills) {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        for (String key : validEntriesMap.keySet()) {
            List<LogEntry> entries = validEntriesMap.get(key);
            executor.submit(() -> generateBill(key, entries, bills));
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    static void generateBill(String username, List<LogEntry> entries, Map<String, Bill> bills) {
        Bill activeBill = new Bill(0, 0);
        boolean endsAvailable = true;
        while(!entries.isEmpty()) {
            LogEntry currentEntry = entries.get(0);
            long sessionTime = 0;
            // Current Entry is an 'End' action, so should use the first timestamp
            if(Action.End.equals(currentEntry.action())) {
                sessionTime = Duration.between(firstTime, currentEntry.timestamp()).getSeconds();
            // Current Entry is a 'Start' action, so look for an 'End' action for the user
            } else {
                boolean endFound = false;
                if(endsAvailable) {
                    for (int index = 1; index < entries.size(); index++) {
                        LogEntry nextEntry = entries.get(index);
                        if(Action.End.equals(nextEntry.action())) {
                            endFound = true;
                            sessionTime = Duration.between(currentEntry.timestamp(), nextEntry.timestamp()).getSeconds();
                            // remove the 'End' action as it has been tied to a 'Start' action
                            entries.remove(index);
                            break;
                        }
                    }
                }
                // no associated End action for user, use latest timestamp
                if(!endFound) {
                    endsAvailable = false;
                    sessionTime = Duration.between(currentEntry.timestamp(), lastTime).getSeconds();
                }
            }
            activeBill.addActiveSessionTime(sessionTime);
            activeBill.addSession();
            entries.remove(0);
        }
        bills.put(username, activeBill);
    }

    static void cleanInputAndPopulateEntries(List<String> lines, Map<String, List<LogEntry>> validEntriesMap) {
        for(String line : lines) {
            if(validEntryFormat(line)) {
                String[] segments = line.split(" ");
                LocalTime logTime = LocalTime.parse(segments[0], DateTimeFormatter.ofPattern("HH:mm:ss"));
                LogEntry newLogEntry = new LogEntry(
                        logTime,
                        Action.valueOf(segments[2]));

                if(isNull(firstTime)) {
                    firstTime = logTime;
                }
                lastTime = logTime;

                List<LogEntry> currentEntry = validEntriesMap.get(segments[1]);
                if (nonNull(currentEntry)) {
                    currentEntry.add(newLogEntry);
                } else {
                    List<LogEntry> newLogEntryList = new ArrayList<>();
                    newLogEntryList.add(newLogEntry);
                    validEntriesMap.put(segments[1], newLogEntryList);
                }
            }
        }
    }

    static boolean validEntryFormat(String entry) {
        if(!entry.matches("^\\d{2}:\\d{2}:\\d{2} [a-zA-Z0-9]+ (Start|End)$")) {
            return false;
        }
        try {
            LocalTime.parse(entry.split(" ")[0], DateTimeFormatter.ofPattern("HH:mm:ss"));
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}