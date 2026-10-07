package language.exchange.study.event;

import language.exchange.study.dto.command.TopicCreateCommand;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
        return TopicCreateCommand.builder()
                .nameKo(nameKo)
                .nameJa(nameJa)
                .questions(questions.stream()
                        .map(QuestionCreateEvent::toCommand)
                        .toList())
                .build();
    }
}