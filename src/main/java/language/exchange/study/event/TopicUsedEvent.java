package language.exchange.study.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TopicUsedEvent {

    private Long topicId;
    private LocalDate usedDate;

    public static TopicUsedEvent create(Long topicId, LocalDate usedDate) {
        return new TopicUsedEvent(topicId, usedDate);
    }
}