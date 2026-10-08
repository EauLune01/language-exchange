package language.exchange.study.event;

import language.exchange.study.dto.command.QuestionCreateCommand;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionCreateEvent {

    private List<LocalizedTextEvent> contents;

    public static QuestionCreateEvent from(QuestionCreateCommand command) {
        return new QuestionCreateEvent(command.getContents().stream().map(LocalizedTextEvent::from).toList());
    }

    public QuestionCreateCommand toCommand() {
        return QuestionCreateCommand.from(contents.stream().map(LocalizedTextEvent::toCommand).toList());
    }
}
