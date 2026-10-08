package language.exchange.study.service;

import language.exchange.global.exception.BusinessException;
import language.exchange.global.exception.ErrorCode;
import language.exchange.room.domain.Language;
import language.exchange.room.dto.command.RoomCreateCommand;
import language.exchange.room.dto.command.RoomMemberCommand;
import language.exchange.room.service.RoomService;
import language.exchange.study.dto.command.LocalizedTextCommand;
import language.exchange.study.dto.command.QuestionCreateCommand;
import language.exchange.study.dto.command.TopicCreateCommand;
import language.exchange.study.dto.result.LocalizedTextResult;
import language.exchange.study.dto.result.QuestionListResult;
import language.exchange.study.dto.result.TopicResult;
import language.exchange.study.dto.result.TopicSummaryResult;
import language.exchange.study.publisher.TopicCreatePublisher;
import language.exchange.study.publisher.TopicUsedPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 방 격리: 한 방의 주제·질문·기록·이번 주 주제가 다른 방에 보이지 않는지 실제 DB 쿼리로 확인합니다.
 * (docker compose 의 MySQL·RabbitMQ 가 떠 있어야 하고, 테스트 데이터는 끝나면 롤백됩니다.
 *  큐로는 보내지 않고 Consumer 가 부르는 서비스 메서드를 직접 호출합니다.)
 */
@SpringBootTest
@Transactional
class TopicRoomIsolationTest {

    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20);

    @Autowired
    private RoomService roomService;
    @Autowired
    private TopicService topicService;
    @Autowired
    private TopicQueryService topicQueryService;

    @MockitoBean
    private TopicCreatePublisher topicCreatePublisher;
    @MockitoBean
    private TopicUsedPublisher topicUsedPublisher;

    private Long koJaRoomId; // A = KO, B = JA
    private Long enFrRoomId; // A = EN, B = FR
    private Long koJaTopicId;
    private Long enFrTopicId;

    @BeforeEach
    void setUp() {
        koJaRoomId = createRoom(Language.JA, Language.KO);
        enFrRoomId = createRoom(Language.FR, Language.EN);

        topicService.createTopic(koJaRoomId, topic(Language.KO, "공원", Language.JA, "公園"));
        topicService.createTopic(enFrRoomId, topic(Language.EN, "Park", Language.FR, "Parc"));

        koJaTopicId = topicQueryService.getTopics(koJaRoomId, FIRST_PAGE).getContent().get(0).getId();
        enFrTopicId = topicQueryService.getTopics(enFrRoomId, FIRST_PAGE).getContent().get(0).getId();
    }

    @Test
    @DisplayName("주제 목록에는 그 방의 주제만, 그 방의 두 언어로 나온다")
    void topicsAreScopedToRoom() {
        List<TopicSummaryResult> koJaTopics = topicQueryService.getTopics(koJaRoomId, FIRST_PAGE).getContent();
        List<TopicSummaryResult> enFrTopics = topicQueryService.getTopics(enFrRoomId, FIRST_PAGE).getContent();

        assertThat(koJaTopics).hasSize(1);
        assertThat(koJaTopics.get(0).getNames()).extracting(LocalizedTextResult::getLang, LocalizedTextResult::getText)
                .containsExactly(tuple(Language.KO, "공원"), tuple(Language.JA, "公園"));
        assertThat(enFrTopics).hasSize(1);
        assertThat(enFrTopics.get(0).getNames()).extracting(LocalizedTextResult::getLang, LocalizedTextResult::getText)
                .containsExactly(tuple(Language.EN, "Park"), tuple(Language.FR, "Parc"));
    }

    @Test
    @DisplayName("다른 방의 topicId로 질문을 조회하면 없는 주제처럼 404다")
    void questionsOfAnotherRoomAreNotFound() {
        QuestionListResult own = topicQueryService.getQuestions(koJaRoomId, koJaTopicId, Language.JA);
        assertThat(own.getQuestions()).hasSize(3);
        assertThat(own.getQuestions().get(0).getContent()).isEqualTo("公園 1");

        assertThatThrownBy(() -> topicQueryService.getQuestions(koJaRoomId, enFrTopicId, Language.KO))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TOPIC_NOT_FOUND);
    }

    @Test
    @DisplayName("방의 두 언어가 아닌 언어로는 질문을 조회할 수 없다")
    void questionsRejectLanguageOutsideRoom() {
        assertThatThrownBy(() -> topicQueryService.getQuestions(koJaRoomId, koJaTopicId, Language.EN))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LANGUAGE_NOT_IN_ROOM);
    }

    @Test
    @DisplayName("방의 두 언어가 아닌 언어로는 주제를 등록할 수 없고 큐에도 넣지 않는다")
    void topicCreationRejectsLanguageOutsideRoom() {
        assertThatThrownBy(() -> topicService.requestTopicCreation(
                koJaRoomId, topic(Language.EN, "Park", Language.FR, "Parc")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LANGUAGE_NOT_IN_ROOM);
        assertThatThrownBy(() -> topicService.requestTopicCreation(
                koJaRoomId, topic(Language.KO, "공원", Language.KO, "공원")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LANGUAGE_NOT_IN_ROOM);
        verify(topicCreatePublisher, never()).publish(any());
    }

    @Test
    @DisplayName("이번 주 주제와 학습 기록은 방끼리 섞이지 않는다")
    void weeklyTopicAndHistoryAreScopedToRoom() {
        LocalDate today = LocalDate.now();

        TopicResult picked = topicQueryService.getWeeklyTopic(koJaRoomId);
        assertThat(picked.getId()).isEqualTo(koJaTopicId);
        verify(topicUsedPublisher).publish(koJaTopicId, today);
        topicService.markAsUsed(koJaTopicId, today); // Consumer 가 하는 일

        assertThat(topicQueryService.getThisWeekTopic(koJaRoomId)).map(TopicResult::getId).contains(koJaTopicId);
        assertThat(topicQueryService.getThisWeekTopic(enFrRoomId)).isEmpty();

        assertThat(topicQueryService.getTopicHistory(koJaRoomId, FIRST_PAGE).getContent()).hasSize(1);
        assertThat(topicQueryService.getTopicHistory(enFrRoomId, FIRST_PAGE).getContent()).isEmpty();

        // 다른 방이 뽑으면 자기 방의 주제가 나온다
        assertThat(topicQueryService.getWeeklyTopic(enFrRoomId).getId()).isEqualTo(enFrTopicId);
    }

    @Test
    @DisplayName("주제가 없는 방은 다른 방에 안 쓴 주제가 있어도 뽑을 수 없다")
    void emptyRoomCannotDrawAnotherRoomsTopic() {
        Long emptyRoomId = createRoom(Language.ES, Language.IT);

        assertThatThrownBy(() -> topicQueryService.getWeeklyTopic(emptyRoomId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NO_AVAILABLE_TOPIC);
        assertThat(topicQueryService.getTopics(emptyRoomId, FIRST_PAGE).getContent()).isEmpty();
    }

    @Test
    @DisplayName("사용 처리는 멱등하다: 같은 주제를 두 번 처리해도 처음 날짜가 남는다")
    void markAsUsedIsIdempotent() {
        LocalDate first = LocalDate.now();

        topicService.markAsUsed(koJaTopicId, first);
        topicService.markAsUsed(koJaTopicId, first.plusDays(1));

        assertThat(topicQueryService.getTopics(koJaRoomId, FIRST_PAGE).getContent().get(0).getUsedDate())
                .isEqualTo(first);
    }

    private static org.assertj.core.groups.Tuple tuple(Object... values) {
        return org.assertj.core.groups.Tuple.tuple(values);
    }

    /** 첫 번째 사람이 learningA 를, 두 번째 사람이 learningB 를 배우는 방 (A = learningB, B = learningA) */
    private Long createRoom(Language learningA, Language learningB) {
        String loginId = "t-" + UUID.randomUUID().toString().substring(0, 12);
        return roomService.createRoom(RoomCreateCommand.of(loginId, "password123",
                RoomMemberCommand.of("first", "KR", learningA),
                RoomMemberCommand.of("second", "JP", learningB)));
    }

    /** 질문은 "<주제명> 1", "<주제명> 2", "<주제명> 3" */
    private TopicCreateCommand topic(Language langA, String nameA, Language langB, String nameB) {
        List<QuestionCreateCommand> questions = List.of(1, 2, 3).stream()
                .map(no -> QuestionCreateCommand.from(List.of(
                        LocalizedTextCommand.of(langA, nameA + " " + no),
                        LocalizedTextCommand.of(langB, nameB + " " + no))))
                .toList();
        return TopicCreateCommand.of(
                List.of(LocalizedTextCommand.of(langA, nameA), LocalizedTextCommand.of(langB, nameB)),
                questions);
    }
}
