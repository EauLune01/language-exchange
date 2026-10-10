package language.exchange.note.service;

import language.exchange.global.exception.BusinessException;
import language.exchange.global.exception.ErrorCode;
import language.exchange.room.domain.Language;
import language.exchange.room.dto.command.RoomCreateCommand;
import language.exchange.room.dto.command.RoomMemberCommand;
import language.exchange.room.service.RoomService;
import language.exchange.study.dto.command.LocalizedTextCommand;
import language.exchange.study.dto.command.QuestionCreateCommand;
import language.exchange.study.dto.command.TopicCreateCommand;
import language.exchange.note.dto.command.NoteUpsertCommand;
import language.exchange.study.publisher.TopicCreatePublisher;
import language.exchange.study.publisher.TopicUsedPublisher;
import language.exchange.study.service.TopicQueryService;
import language.exchange.study.service.TopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 메모 저장(upsert)과 방·언어 격리. (test 프로파일: 메모리 DB, 큐에는 연결하지 않습니다) */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteServiceTest {

    @Autowired
    private RoomService roomService;
    @Autowired
    private TopicService topicService;
    @Autowired
    private TopicQueryService topicQueryService;
    @Autowired
    private NoteService noteService;
    @Autowired
    private NoteQueryService noteQueryService;

    @MockitoBean
    private TopicCreatePublisher topicCreatePublisher;
    @MockitoBean
    private TopicUsedPublisher topicUsedPublisher;

    private Long roomId;
    private Long otherRoomId;
    private Long questionId;
    private Long unusedQuestionId;

    @BeforeEach
    void setUp() {
        roomId = createRoom(Language.JA, Language.KO);
        otherRoomId = createRoom(Language.FR, Language.EN);

        topicService.createTopic(roomId, TopicCreateCommand.of(
                texts("공원", "公園"),
                List.of(question(1), question(2), question(3))));
        Long topicId = topicQueryService.getTopics(roomId, PageRequest.of(0, 20)).getContent().get(0).getId();
        questionId = topicQueryService.getQuestions(roomId, topicId, Language.KO).getQuestions().get(0).getId();
        topicService.markAsUsed(topicId, LocalDate.now()); // 메모는 사용된 주제의 질문에만 적을 수 있다 (Consumer 가 하는 일)

        topicService.createTopic(roomId, TopicCreateCommand.of(
                texts("여행", "旅行"),
                List.of(question(1), question(2), question(3))));
        unusedQuestionId = topicQueryService.getTopics(roomId, PageRequest.of(0, 20)).getContent().stream()
                .filter(topic -> topic.getUsedDate() == null)
                .map(topic -> topicQueryService.getQuestions(roomId, topic.getId(), Language.KO).getQuestions().get(0).getId())
                .findFirst().orElseThrow();
    }

    @Test
    @DisplayName("메모가 없으면 빈 문자열이고, 저장하면 만들어지고, 다시 저장하면 내용이 바뀐다")
    void upsertCreatesThenUpdates() {
        assertThat(noteQueryService.find(questionId, roomId, Language.KO).getContent()).isEmpty();

        noteService.upsert(NoteUpsertCommand.of(questionId, roomId, Language.KO, "첫 메모"));
        assertThat(noteQueryService.find(questionId, roomId, Language.KO).getContent()).isEqualTo("첫 메모");

        noteService.upsert(NoteUpsertCommand.of(questionId, roomId, Language.KO, "고친 메모"));
        assertThat(noteQueryService.find(questionId, roomId, Language.KO).getContent()).isEqualTo("고친 메모");
    }

    @Test
    @DisplayName("다른 방에서는 메모가 보이지 않고, 다른 방의 질문에는 메모를 적을 수 없다(404)")
    void notesAreScopedToRoom() {
        noteService.upsert(NoteUpsertCommand.of(questionId, roomId, Language.KO, "우리 방 메모"));

        assertThat(noteQueryService.find(questionId, otherRoomId, Language.EN).getContent()).isEmpty();
        assertThatThrownBy(() -> noteService.upsert(NoteUpsertCommand.of(questionId, otherRoomId, Language.EN, "남의 방 메모")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("같은 질문이어도 언어마다 메모가 따로이고, 방의 두 언어가 아닌 언어는 거부한다")
    void notesAreScopedToLanguage() {
        noteService.upsert(NoteUpsertCommand.of(questionId, roomId, Language.KO, "한국어 메모"));
        noteService.upsert(NoteUpsertCommand.of(questionId, roomId, Language.JA, "日本語のメモ"));

        assertThat(noteQueryService.find(questionId, roomId, Language.KO).getContent()).isEqualTo("한국어 메모");
        assertThat(noteQueryService.find(questionId, roomId, Language.JA).getContent()).isEqualTo("日本語のメモ");

        assertThatThrownBy(() -> noteService.upsert(NoteUpsertCommand.of(questionId, roomId, Language.EN, "memo")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LANGUAGE_NOT_IN_ROOM);
        assertThatThrownBy(() -> noteQueryService.find(questionId, roomId, Language.EN))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LANGUAGE_NOT_IN_ROOM);
    }

    @Test
    @DisplayName("아직 사용하지 않은 주제의 질문에는 메모를 적을 수 없지만(403), 조회는 된다")
    void rejectsNoteOnUnusedTopic() {
        assertThatThrownBy(() -> noteService.upsert(NoteUpsertCommand.of(unusedQuestionId, roomId, Language.KO, "메모")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TOPIC_NOT_USED);
        assertThat(noteQueryService.find(unusedQuestionId, roomId, Language.KO).getContent()).isEmpty();
    }

    @Test
    @DisplayName("빈 내용과 없는 질문은 거부한다")
    void rejectsBlankContentAndUnknownQuestion() {
        assertThatThrownBy(() -> noteService.upsert(NoteUpsertCommand.of(questionId, roomId, Language.KO, "  ")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOTE_CONTENT_BLANK);
        assertThatThrownBy(() -> noteService.upsert(NoteUpsertCommand.of(-1L, roomId, Language.KO, "메모")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
    }

    /** 첫 번째 사람이 learningA 를, 두 번째 사람이 learningB 를 배우는 방 (A = learningB, B = learningA) */
    private Long createRoom(Language learningA, Language learningB) {
        String loginId = "t-" + UUID.randomUUID().toString().substring(0, 12);
        return roomService.createRoom(RoomCreateCommand.of(loginId, "password123",
                RoomMemberCommand.of("first", "KR", learningA),
                RoomMemberCommand.of("second", "JP", learningB), 50));
    }

    private static List<LocalizedTextCommand> texts(String ko, String ja) {
        return List.of(LocalizedTextCommand.of(Language.KO, ko), LocalizedTextCommand.of(Language.JA, ja));
    }

    private static QuestionCreateCommand question(int no) {
        return QuestionCreateCommand.from(texts("공원 " + no, "公園 " + no));
    }
}
