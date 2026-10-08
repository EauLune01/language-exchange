package language.exchange.study.dto.command;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class QuestionCreateCommand {

    private final List<LocalizedTextCommand> contents;

    public static QuestionCreateCommand from(List<LocalizedTextCommand> contents) {
        return new QuestionCreateCommand(contents);
    }
}
