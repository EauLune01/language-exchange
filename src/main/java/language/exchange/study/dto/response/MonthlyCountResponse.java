package language.exchange.study.dto.response;

import language.exchange.study.dto.result.MonthlyCountResult;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonthlyCountResponse {

    private String month;
    private long count;

    public static MonthlyCountResponse from(MonthlyCountResult result) {
        return MonthlyCountResponse.builder()
                .month(result.getMonth())
                .count(result.getCount())
                .build();
    }
}
