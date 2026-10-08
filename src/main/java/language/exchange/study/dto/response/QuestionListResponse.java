package language.exchange.study.dto.response;

import language.exchange.room.domain.Language;
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

    public static QuestionListResponse from(QuestionListResult result) {
        return QuestionListResponse.builder()
                .topicId(result.getTopicId())
                .language(result.getLanguage())
                .questions(result.getQuestions().stream().map(QuestionResponse::from).toList())
                .build();
    }
}
