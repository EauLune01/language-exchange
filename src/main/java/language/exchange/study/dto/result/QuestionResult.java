package language.exchange.study.dto.result;

import lombok.Getter;

@Getter
public class QuestionResult {

    private final int sequence;
    private final String contentKo;
    private final String contentJa;

    private QuestionResult(int sequence, String contentKo, String contentJa) {
        this.sequence = sequence;
        this.contentKo = contentKo;
        this.contentJa = contentJa;
    }

    public static QuestionResult of(int sequence, String contentKo, String contentJa) {
        return new QuestionResult(sequence, contentKo, contentJa);
    }
}