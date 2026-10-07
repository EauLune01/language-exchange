package language.exchange.study.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TopicCreateCommand {
    private String nameKo;
    private String nameJa;
    private List<QuestionCreateCommand> questions;
}