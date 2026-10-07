package language.exchange.study.dto.command;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QuestionCreateCommand {
    private String contentKo;
    private String contentJa;
}