package language.exchange.study.dto.response;

import language.exchange.study.domain.Language;
import language.exchange.study.dto.result.QuestionListResult;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuestionListResponse {
    private Long topicId;
    private Language language;
    private List<QuestionResponse> questions;

    public static QuestionListResponse of(QuestionListResult result, Language language) {
        return QuestionListResponse.builder()
                .topicId(result.getTopicId())
                .language(language)
                .questions(result.getQuestions().stream()
                        .map(q -> QuestionResponse.of(q, language))
                        .toList())
                .build();
    }
}