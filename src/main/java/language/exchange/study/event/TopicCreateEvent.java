package language.exchange.study.event;

import language.exchange.study.dto.command.QuestionCreateCommand;
import language.exchange.study.dto.command.TopicCreateCommand;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TopicCreateEvent {

    private String nameKo;
    private String nameJa;
    private List<QuestionCreateEvent> questions;

    public static TopicCreateEvent from(TopicCreateCommand command) {
        List<QuestionCreateEvent> questionEvents = command.getQuestions().stream()
                .map(QuestionCreateEvent::from)
                .toList();
        return new TopicCreateEvent(command.getNameKo(), command.getNameJa(), questionEvents);
    }

    public TopicCreateCommand toCommand() {
        List<QuestionCreateCommand> questionCommands = questions.stream()
                .map(QuestionCreateEvent::toCommand)
                .toList();
        return TopicCreateCommand.of(nameKo, nameJa, questionCommands);
    }
}