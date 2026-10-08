package language.exchange.study.dto.result;

import lombok.Getter;

import java.util.List;

@Getter
public class QuestionListResult {

    private final Long topicId;
    private final List<QuestionResult> questions;

    private QuestionListResult(Long topicId, List<QuestionResult> questions) {
        this.topicId = topicId;
        this.questions = questions;
    }

    public static QuestionListResult of(Long topicId, List<QuestionResult> questions) {
        return new QuestionListResult(topicId, questions);
    }
}