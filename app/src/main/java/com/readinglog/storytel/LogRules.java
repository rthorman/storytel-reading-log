package com.readinglog.storytel;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Validation shared by the editor and database writes. Empty dates mean not yet started/finished. */
public final class LogRules {
    private LogRules() {}
    public static void validate(String title, String author, String started, String finished) {
        if (title.trim().isEmpty()) throw new IllegalArgumentException("Enter a title.");
        if (author.trim().isEmpty()) throw new IllegalArgumentException("Enter an author.");
        LocalDate start = date(started);
        LocalDate finish = date(finished);
        if (finish != null && start == null) throw new IllegalArgumentException("Add a start date before finishing a book.");
        if (finish != null && finish.isBefore(start)) throw new IllegalArgumentException("Finish date must be on or after the start date.");
        if ((start != null && start.isAfter(LocalDate.now())) || (finish != null && finish.isAfter(LocalDate.now())))
            throw new IllegalArgumentException("Reading dates cannot be in the future.");
    }
    private static LocalDate date(String value) {
        if (value.isEmpty()) return null;
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException e) { throw new IllegalArgumentException("Use dates in YYYY-MM-DD format."); }
    }
    public static String status(String started, String finished) {
        return !finished.isEmpty() ? "Finished" : !started.isEmpty() ? "In progress" : "To read";
    }
}
