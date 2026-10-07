package language.exchange.study.dto.result;

import language.exchange.study.domain.Question;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionResult {
    private int sequence;
    private String contentKo;
    private String contentJa;

    public static QuestionResult from(Question question) {
        return QuestionResult.builder()
                .sequence(question.getSequence())
                .contentKo(question.getContentKo())
                .contentJa(question.getContentJa())
                .build();
    }
}