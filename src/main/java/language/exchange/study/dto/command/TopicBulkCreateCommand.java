package language.exchange.study.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TopicBulkCreateCommand {
    private List<TopicCreateCommand> topics;
}