package language.exchange.study.dto.response;

import language.exchange.study.dto.result.TopicResult;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TopicResponse {

    private Long id;
    private List<LocalizedTextResponse> names;

    public static TopicResponse from(TopicResult result) {
        return TopicResponse.builder()
                .id(result.getId())
                .names(result.getNames().stream().map(LocalizedTextResponse::from).toList())
                .build();
    }
}
