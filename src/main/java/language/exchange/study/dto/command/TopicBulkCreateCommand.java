package language.exchange.study.dto.command;

import lombok.Getter;

import java.util.List;

@Getter
public class TopicBulkCreateCommand {

    private final List<TopicCreateCommand> topics;

    private TopicBulkCreateCommand(List<TopicCreateCommand> topics) {
        this.topics = topics;
    }

    public static TopicBulkCreateCommand from(List<TopicCreateCommand> topics) {
        return new TopicBulkCreateCommand(topics);
    }
}