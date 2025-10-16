package com.example.eventapp.util;

import com.example.eventapp.model.Event;
import com.example.eventapp.model.EventActivity;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.Writer;
import java.time.format.DateTimeFormatter;

public final class CsvExporter {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private CsvExporter() {
    }

    public static void exportEvent(Event event, Writer writer) throws IOException {
        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("Секция", "Начало", "Окончание", "Жюри"))) {
            for (EventActivity activity : event.getActivities()) {
                String juryNames = activity.getJuryMembers().stream()
                        .map(j -> j.getFullName().toString())
                        .reduce((a, b) -> a + "; " + b)
                        .orElse("-");
                printer.printRecord(activity.getTitle(),
                        activity.getStartTime().format(DATE_TIME_FORMATTER),
                        activity.getEndTime().format(DATE_TIME_FORMATTER),
                        juryNames);
            }
        }
    }
}
