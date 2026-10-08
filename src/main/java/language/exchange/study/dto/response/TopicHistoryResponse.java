package language.exchange.study.dto.response;

import language.exchange.study.dto.result.TopicHistoryResult;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class TopicHistoryResponse {

    private Long id;
    private long round;
    private String nameKo;
    private String nameJa;
    private LocalDate usedDate;

    public static TopicHistoryResponse from(TopicHistoryResult result) {
        return TopicHistoryResponse.builder()
                .id(result.getId())
                .round(result.getRound())
                .nameKo(result.getNameKo())
                .nameJa(result.getNameJa())
                .usedDate(result.getUsedDate())
                .build();
    }
}