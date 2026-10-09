package language.exchange.study.dto.response;

import language.exchange.study.dto.result.StatsResult;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class StatsResponse {

    private long totalTopics;
    private long usedTopics;
    private long thisMonthCount;
    private int weekStreak;
    private LocalDate firstUsedDate;
    private List<MonthlyCountResponse> monthly;

    public static StatsResponse from(StatsResult result) {
        return StatsResponse.builder()
                .totalTopics(result.getTotalTopics())
                .usedTopics(result.getUsedTopics())
                .thisMonthCount(result.getThisMonthCount())
                .weekStreak(result.getWeekStreak())
                .firstUsedDate(result.getFirstUsedDate())
                .monthly(result.getMonthly().stream().map(MonthlyCountResponse::from).toList())
                .build();
    }
}
