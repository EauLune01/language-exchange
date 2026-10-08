package language.exchange.study.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TopicUsedEvent {

    private Long topicId;
    private LocalDate usedDate;

    public static TopicUsedEvent create(Long topicId, LocalDate usedDate) {
        return new TopicUsedEvent(topicId, usedDate);
    }
}