package language.exchange.study.dto.command;

import language.exchange.room.domain.Language;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LocalizedTextCommand {

    private final Language lang;
    private final String text;

    public static LocalizedTextCommand of(Language lang, String text) {
        return new LocalizedTextCommand(lang, text);
    }
}
