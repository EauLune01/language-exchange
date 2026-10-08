package language.exchange.study.dto.command;

import lombok.Getter;

@Getter
public class QuestionCreateCommand {

    private final String contentKo;
    private final String contentJa;

    private QuestionCreateCommand(String contentKo, String contentJa) {
        this.contentKo = contentKo;
        this.contentJa = contentJa;
    }

    public static QuestionCreateCommand of(String contentKo, String contentJa) {
        return new QuestionCreateCommand(contentKo, contentJa);
    }
}