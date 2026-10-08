package language.exchange.study.event;

import language.exchange.study.dto.command.QuestionCreateCommand;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionCreateEvent {

    private String contentKo;
    private String contentJa;

    public static QuestionCreateEvent from(QuestionCreateCommand command) {
        return new QuestionCreateEvent(command.getContentKo(), command.getContentJa());
    }

    public QuestionCreateCommand toCommand() {
        return QuestionCreateCommand.of(contentKo, contentJa);
    }
}