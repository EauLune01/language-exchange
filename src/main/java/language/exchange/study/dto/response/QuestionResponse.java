package language.exchange.study.dto.response;

import language.exchange.study.dto.result.QuestionResult;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionResponse {

    private int sequence;
    private String content;

    public static QuestionResponse from(QuestionResult result) {
        return QuestionResponse.builder()
                .sequence(result.getSequence())
                .content(result.getContent())
                .build();
    }
}
