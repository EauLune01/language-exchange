package language.exchange.study.dto.result;

import language.exchange.room.domain.Language;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LocalizedTextResult {

    private final Language lang;
    private final String text;

    public static LocalizedTextResult of(Language lang, String text) {
        return new LocalizedTextResult(lang, text);
    }
}
