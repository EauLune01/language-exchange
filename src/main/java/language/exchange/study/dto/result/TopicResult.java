package language.exchange.study.dto.result;

import lombok.Getter;

@Getter
public class TopicResult {

    private final Long id;
    private final String nameKo;
    private final String nameJa;

    private TopicResult(Long id, String nameKo, String nameJa) {
        this.id = id;
        this.nameKo = nameKo;
        this.nameJa = nameJa;
    }

    public static TopicResult of(Long id, String nameKo, String nameJa) {
        return new TopicResult(id, nameKo, nameJa);
    }
}