package language.exchange.study.dto.response;

import language.exchange.room.domain.Language;
import language.exchange.study.dto.result.LocalizedTextResult;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LocalizedTextResponse {

    private Language lang;
    private String text;

    public static LocalizedTextResponse from(LocalizedTextResult result) {
        return LocalizedTextResponse.builder()
                .lang(result.getLang())
                .text(result.getText())
                .build();
    }
}
