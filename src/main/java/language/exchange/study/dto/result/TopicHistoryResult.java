package language.exchange.study.dto.result;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TopicHistoryResult {

    private final Long id;
    private final long round;
    private final List<LocalizedTextResult> names;
    private final LocalDate usedDate;

    public static TopicHistoryResult of(Long id, long round, List<LocalizedTextResult> names, LocalDate usedDate) {
        return new TopicHistoryResult(id, round, names, usedDate);
    }
}
