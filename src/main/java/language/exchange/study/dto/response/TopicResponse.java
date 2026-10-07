package language.exchange.study.dto.response;

import language.exchange.study.dto.result.TopicResult;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TopicResponse {
    private Long id;
    private String nameKo;
    private String nameJa;

    public static TopicResponse from(TopicResult result) {
        return TopicResponse.builder()
                .id(result.getId())
                .nameKo(result.getNameKo())
                .nameJa(result.getNameJa())
                .build();
    }
}