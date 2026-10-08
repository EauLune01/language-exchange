package language.exchange.study.dto.response;

import language.exchange.study.dto.result.TopicSummaryResult;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class TopicSummaryResponse {

    private Long id;
    private List<LocalizedTextResponse> names;
    private LocalDate usedDate;

    public static TopicSummaryResponse from(TopicSummaryResult result) {
        return TopicSummaryResponse.builder()
                .id(result.getId())
                .names(result.getNames().stream().map(LocalizedTextResponse::from).toList())
                .usedDate(result.getUsedDate())
                .build();
    }
}
