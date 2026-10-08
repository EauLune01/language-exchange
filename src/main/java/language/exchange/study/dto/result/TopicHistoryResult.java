package language.exchange.study.dto.result;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class TopicHistoryResult {

    private final Long id;
    private final long round;
    private final String nameKo;
    private final String nameJa;
    private final LocalDate usedDate;

    private TopicHistoryResult(Long id, long round, String nameKo, String nameJa, LocalDate usedDate) {
        this.id = id;
        this.round = round;
        this.nameKo = nameKo;
        this.nameJa = nameJa;
        this.usedDate = usedDate;
    }

    public static TopicHistoryResult of(Long id, long round, String nameKo, String nameJa, LocalDate usedDate) {
        return new TopicHistoryResult(id, round, nameKo, nameJa, usedDate);
    }
}