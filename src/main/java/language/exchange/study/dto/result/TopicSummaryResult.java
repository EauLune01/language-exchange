package language.exchange.study.dto.result;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TopicSummaryResult {

    private final Long id;
    private final List<LocalizedTextResult> names;
    private final LocalDate usedDate;

    public static TopicSummaryResult of(Long id, List<LocalizedTextResult> names, LocalDate usedDate) {
        return new TopicSummaryResult(id, names, usedDate);
    }
}
