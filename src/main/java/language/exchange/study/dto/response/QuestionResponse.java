package language.exchange.study.dto.response;

import language.exchange.room.domain.Language;
import language.exchange.study.dto.result.QuestionResult;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionResponse {
    private int sequence;
    private String content;

    public static QuestionResponse of(QuestionResult result, Language language) {
        return QuestionResponse.builder()
                .sequence(result.getSequence())
                .content(language == Language.KO ? result.getContentKo() : result.getContentJa())
                .build();
    }
}