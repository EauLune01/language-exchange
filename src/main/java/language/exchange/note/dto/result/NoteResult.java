package language.exchange.note.dto.result;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class NoteResult {

    private final String content;

    public static NoteResult from(String content) {
        return new NoteResult(content);
    }
}
