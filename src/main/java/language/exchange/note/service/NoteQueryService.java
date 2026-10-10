package language.exchange.note.service;

import language.exchange.global.exception.BusinessException;
import language.exchange.global.exception.ErrorCode;
import language.exchange.room.domain.Language;
import language.exchange.room.service.RoomQueryService;
import language.exchange.note.domain.Note;
import language.exchange.note.dto.result.NoteResult;
import language.exchange.note.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteQueryService {

    private final NoteRepository noteRepository;
    private final RoomQueryService roomQueryService;

    /** 아직 적은 메모가 없으면 빈 문자열을 돌려줍니다. */
    public NoteResult find(Long questionId, Long roomId, Language lang) {
        if (!roomQueryService.getLanguages(roomId).contains(lang)) {
            throw new BusinessException(ErrorCode.LANGUAGE_NOT_IN_ROOM);
        }
        return NoteResult.from(noteRepository.findByQuestionIdAndRoomIdAndLang(questionId, roomId, lang.name())
                .map(Note::getContent)
                .orElse(""));
    }
}
