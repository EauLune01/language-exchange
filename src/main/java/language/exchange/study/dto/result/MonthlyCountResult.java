package language.exchange.study.dto.result;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MonthlyCountResult {

    private final String month; // "2026-10"
    private final long count;

    public static MonthlyCountResult of(String month, long count) {
        return new MonthlyCountResult(month, count);
    }
}
