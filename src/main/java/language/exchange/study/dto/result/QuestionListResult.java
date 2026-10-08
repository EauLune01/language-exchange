package language.exchange.study.dto.result;

import language.exchange.room.domain.Language;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class QuestionListResult {

    private final Long topicId;
    private final Language language;
    private final List<QuestionResult> questions;

    public static QuestionListResult of(Long topicId, Language language, List<QuestionResult> questions) {
        return new QuestionListResult(topicId, language, questions);
    }
}
