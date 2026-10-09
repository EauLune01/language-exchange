package language.exchange.study.dto.result;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class StatsResult {

    private final long totalTopics;
    private final long usedTopics;
    private final long thisMonthCount;
    private final int weekStreak; // 이번 주(아직 안 했으면 지난주)까지 한 주도 거르지 않고 이어온 주 수
    private final LocalDate firstUsedDate; // 아직 뽑은 주제가 없으면 null
    private final List<MonthlyCountResult> monthly; // 오래된 달 → 이번 달

    public static StatsResult of(long totalTopics, long usedTopics, long thisMonthCount, int weekStreak,
                                 LocalDate firstUsedDate, List<MonthlyCountResult> monthly) {
        return new StatsResult(totalTopics, usedTopics, thisMonthCount, weekStreak, firstUsedDate, monthly);
    }
}
