package language.exchange.study.dto.result;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class QuestionResult {

    private final Long id;
    private final int sequence;
    private final String content;

    public static QuestionResult of(Long id, int sequence, String content) {
        return new QuestionResult(id, sequence, content);
    }
}
