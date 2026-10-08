package language.exchange.study.dto.command;

import lombok.Getter;

import java.util.List;

@Getter
public class TopicCreateCommand {

    private final String nameKo;
    private final String nameJa;
    private final List<QuestionCreateCommand> questions;

    private TopicCreateCommand(String nameKo, String nameJa, List<QuestionCreateCommand> questions) {
        this.nameKo = nameKo;
        this.nameJa = nameJa;
        this.questions = questions;
    }

    public static TopicCreateCommand of(String nameKo, String nameJa, List<QuestionCreateCommand> questions) {
        return new TopicCreateCommand(nameKo, nameJa, questions);
    }
}