package language.exchange.study.note.dto.request;

import jakarta.validation.constraints.Size;
import language.exchange.room.domain.Language;
import language.exchange.study.note.dto.command.NoteUpsertCommand;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteUpsertRequest {

    // 빈 내용은 서비스에서 NOTE_CONTENT_BLANK 로 막는다
    @Size(max = 2000, message = "메모는 2000자까지 적을 수 있습니다.")
    private String content;

    public NoteUpsertCommand toCommand(Long questionId, Long roomId, Language lang) {
        return NoteUpsertCommand.of(questionId, roomId, lang, content);
    }
}
