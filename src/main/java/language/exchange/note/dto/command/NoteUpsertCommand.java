package language.exchange.note.dto.command;

import language.exchange.room.domain.Language;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class NoteUpsertCommand {

    private final Long questionId;
    private final Long roomId;
    private final Language lang;
    private final String content;

    public static NoteUpsertCommand of(Long questionId, Long roomId, Language lang, String content) {
        return new NoteUpsertCommand(questionId, roomId, lang, content);
    }
}
