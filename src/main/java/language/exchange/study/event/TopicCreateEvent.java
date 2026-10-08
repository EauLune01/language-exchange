package language.exchange.study.event;

import language.exchange.study.dto.command.TopicCreateCommand;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TopicCreateEvent {

    private Long roomId;
    private List<LocalizedTextEvent> names;
    private List<QuestionCreateEvent> questions;

    public static TopicCreateEvent of(Long roomId, TopicCreateCommand command) {
        return new TopicCreateEvent(
                roomId,
                command.getNames().stream().map(LocalizedTextEvent::from).toList(),
                command.getQuestions().stream().map(QuestionCreateEvent::from).toList());
    }

    public TopicCreateCommand toCommand() {
        return TopicCreateCommand.of(
                names.stream().map(LocalizedTextEvent::toCommand).toList(),
                questions.stream().map(QuestionCreateEvent::toCommand).toList());
    }
}
