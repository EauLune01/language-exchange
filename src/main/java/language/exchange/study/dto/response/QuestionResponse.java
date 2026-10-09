package language.exchange.study.dto.response;

import language.exchange.study.dto.result.QuestionResult;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionResponse {

    private Long id;
    private int sequence;
    private String content;

    public static QuestionResponse from(QuestionResult result) {
        return QuestionResponse.builder()
                .id(result.getId())
                .sequence(result.getSequence())
                .content(result.getContent())
                .build();
    }
}
