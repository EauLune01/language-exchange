package language.exchange.study.dto.result;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class TopicSummaryResult {

    private final Long id;
    private final String nameKo;
    private final String nameJa;
    private final LocalDate usedDate;

    private TopicSummaryResult(Long id, String nameKo, String nameJa, LocalDate usedDate) {
        this.id = id;
        this.nameKo = nameKo;
        this.nameJa = nameJa;
        this.usedDate = usedDate;
    }

    public static TopicSummaryResult of(Long id, String nameKo, String nameJa, LocalDate usedDate) {
        return new TopicSummaryResult(id, nameKo, nameJa, usedDate);
    }
}