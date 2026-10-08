package language.exchange.study.dto.command;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TopicCreateCommand {

    private final List<LocalizedTextCommand> names;
    private final List<QuestionCreateCommand> questions;

    public static TopicCreateCommand of(List<LocalizedTextCommand> names, List<QuestionCreateCommand> questions) {
        return new TopicCreateCommand(names, questions);
    }
}
