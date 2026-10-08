package language.exchange.study.dto.response;

import language.exchange.study.dto.result.TopicHistoryResult;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class TopicHistoryResponse {

    private Long id;
    private long round;
    private List<LocalizedTextResponse> names;
    private LocalDate usedDate;

    public static TopicHistoryResponse from(TopicHistoryResult result) {
        return TopicHistoryResponse.builder()
                .id(result.getId())
                .round(result.getRound())
                .names(result.getNames().stream().map(LocalizedTextResponse::from).toList())
                .usedDate(result.getUsedDate())
                .build();
    }
}
