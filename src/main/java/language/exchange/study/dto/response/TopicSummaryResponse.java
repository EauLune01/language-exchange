package language.exchange.study.dto.response;

import language.exchange.study.dto.result.TopicSummaryResult;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class TopicSummaryResponse {
    private Long id;
    private String nameKo;
    private String nameJa;
    private LocalDate usedDate;

    public static TopicSummaryResponse from(TopicSummaryResult result) {
        return TopicSummaryResponse.builder()
                .id(result.getId())
                .nameKo(result.getNameKo())
                .nameJa(result.getNameJa())
                .usedDate(result.getUsedDate())
                .build();
    }
}
