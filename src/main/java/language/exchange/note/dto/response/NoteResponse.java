package language.exchange.note.dto.response;

import language.exchange.note.dto.result.NoteResult;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NoteResponse {

    private String content;

    public static NoteResponse from(NoteResult result) {
        return NoteResponse.builder()
                .content(result.getContent())
                .build();
    }
}
