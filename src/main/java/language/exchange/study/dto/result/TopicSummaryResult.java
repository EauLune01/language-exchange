package language.exchange.study.dto.result;

import language.exchange.study.domain.Topic;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class TopicSummaryResult {
    private Long id;
    private String nameKo;
    private String nameJa;
    private LocalDate usedDate;

    public static TopicSummaryResult from(Topic topic) {
        return TopicSummaryResult.builder()
                .id(topic.getId())
                .nameKo(topic.getNameKo())
                .nameJa(topic.getNameJa())
                .usedDate(topic.getUsedDate())
                .build();
    }
}