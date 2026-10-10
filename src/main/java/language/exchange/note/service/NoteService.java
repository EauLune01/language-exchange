package language.exchange.note.service;

import language.exchange.global.exception.BusinessException;
import language.exchange.global.exception.ErrorCode;
import language.exchange.room.service.RoomQueryService;
import language.exchange.study.domain.Question;
import language.exchange.note.domain.Note;
import language.exchange.note.dto.command.NoteUpsertCommand;
import language.exchange.note.repository.NoteRepository;
import language.exchange.study.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NoteService {

    private final NoteRepository noteRepository;
    private final QuestionRepository questionRepository;
    private final RoomQueryService roomQueryService;

    /** 이 방이 그 질문에 그 언어로 적은 메모가 있으면 내용을 바꾸고, 없으면 새로 만듭니다. */
    public void upsert(NoteUpsertCommand command) {
        String content = command.getContent();
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.NOTE_CONTENT_BLANK);
        }

        if (!roomQueryService.getLanguages(command.getRoomId()).contains(command.getLang())) {
            throw new BusinessException(ErrorCode.LANGUAGE_NOT_IN_ROOM);
        }

        // 다른 방의 질문은 없는 질문과 똑같이 404 (존재 여부를 숨긴다)
        Question question = questionRepository.findById(command.getQuestionId())
                .filter(found -> found.getTopic().getRoomId().equals(command.getRoomId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // 메모는 뽑아서 이야기한(사용된) 주제의 질문에만 적을 수 있다. 조회(NoteQueryService)는 막지 않는다
        if (question.getTopic().getUsedDate() == null) {
            throw new BusinessException(ErrorCode.TOPIC_NOT_USED);
        }

        // ponytail: 같은 방에서 첫 저장이 동시에 두 번 오면 unique 제약으로 한쪽이 500. 문제가 되면 예외를 잡아 update 로 재시도
        String lang = command.getLang().name();
        noteRepository.findByQuestionIdAndRoomIdAndLang(command.getQuestionId(), command.getRoomId(), lang)
                .ifPresentOrElse(
                        note -> note.updateContent(content),
                        () -> noteRepository.save(Note.create(question, command.getRoomId(), lang, content)));
    }
}
