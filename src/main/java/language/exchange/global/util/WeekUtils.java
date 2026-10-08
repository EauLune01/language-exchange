package language.exchange.global.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public class WeekUtils {

    private WeekUtils() {}

    public static LocalDate startOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static LocalDate endOfWeek(LocalDate date) {
        return startOfWeek(date).plusDays(6);
    }
}