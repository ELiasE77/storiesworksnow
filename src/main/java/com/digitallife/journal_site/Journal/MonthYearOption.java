package com.digitallife.journal_site.Journal;

/**
 * Represents a month/year pair for filtering journal entries.
 */
public record MonthYearOption(int month, int year, String label) {
}