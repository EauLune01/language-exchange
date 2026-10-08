package language.exchange.study.dto.result;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TopicResult {

    private final Long id;
    private final List<LocalizedTextResult> names;

    public static TopicResult of(Long id, List<LocalizedTextResult> names) {
        return new TopicResult(id, names);
    }
}
