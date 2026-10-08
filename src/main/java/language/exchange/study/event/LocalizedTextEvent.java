package language.exchange.study.event;

import language.exchange.room.domain.Language;
import language.exchange.study.dto.command.LocalizedTextCommand;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LocalizedTextEvent {

    private Language lang;
    private String text;

    public static LocalizedTextEvent from(LocalizedTextCommand command) {
        return new LocalizedTextEvent(command.getLang(), command.getText());
    }

    public LocalizedTextCommand toCommand() {
        return LocalizedTextCommand.of(lang, text);
    }
}
