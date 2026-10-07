package language.exchange.study.dto.result;

import language.exchange.study.domain.Topic;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TopicResult {
    private Long id;
    private String nameKo;
    private String nameJa;

    public static TopicResult from(Topic topic) {
        return TopicResult.builder()
                .id(topic.getId())
                .nameKo(topic.getNameKo())
                .nameJa(topic.getNameJa())
                .build();
    }
}