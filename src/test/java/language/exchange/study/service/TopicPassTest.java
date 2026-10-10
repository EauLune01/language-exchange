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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

/**
 * 주제 패스: 패스한 주제는 안 쓴 주제로 돌아가고(나중에 다시 뽑힐 수 있음) 다른 주제가 대신 뽑히는지 확인합니다.
 * (test 프로파일: 큐에는 연결하지 않고, Consumer 가 부르는 서비스 메서드를 직접 호출합니다.)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TopicPassTest {

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

    private Long roomId;

    @BeforeEach
    void setUp() {
        roomId = createRoom();
        topicService.createTopic(roomId, topic("공원", "公園"));
    }

    @Test
    @DisplayName("패스하면 다른 주제가 뽑히고, 패스한 주제는 안 쓴 주제로 돌아간다")
    void passReturnsTopicToUnusedAndDrawsAnother() {
        topicService.createTopic(roomId, topic("도시", "都市"));
        LocalDate today = LocalDate.now();
        Long drawnId = topicQueryService.getTopic(roomId).getId();
        topicService.markAsUsed(drawnId, today); // Consumer 가 하는 일

        TopicResult next = topicService.passTopic(roomId, drawnId);

        assertThat(next.getId()).isNotEqualTo(drawnId);
        verify(topicUsedPublisher).publish(next.getId(), today);
        assertThat(usedDateOf(drawnId)).isNull();
    }

    @Test
    @DisplayName("대신 뽑을 주제가 없으면 패스할 수 없고, 주제는 사용한 상태로 남는다")
    void passWithoutAnotherTopicChangesNothing() {
        LocalDate today = LocalDate.now();
        Long drawnId = topicQueryService.getTopic(roomId).getId();
        topicService.markAsUsed(drawnId, today);

        assertThatThrownBy(() -> topicService.passTopic(roomId, drawnId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NO_AVAILABLE_TOPIC);
        assertThat(usedDateOf(drawnId)).isEqualTo(today);
    }

    @Test
    @DisplayName("다른 방의 주제는 패스할 수 없다")
    void passRejectsTopicOfAnotherRoom() {
        Long otherRoomId = createRoom();
        topicService.createTopic(otherRoomId, topic("도시", "都市"));
        Long otherTopicId = topicQueryService.getTopics(otherRoomId, FIRST_PAGE).getContent().get(0).getId();

        assertThatThrownBy(() -> topicService.passTopic(roomId, otherTopicId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TOPIC_NOT_FOUND);
    }

    private LocalDate usedDateOf(Long topicId) {
        return topicQueryService.getTopics(roomId, FIRST_PAGE).getContent().stream()
                .filter(topic -> topic.getId().equals(topicId))
                .findFirst()
                .orElseThrow()
                .getUsedDate();
    }

    /** A = KO, B = JA 인 방 */
    private Long createRoom() {
        String loginId = "t-" + UUID.randomUUID().toString().substring(0, 12);
        return roomService.createRoom(RoomCreateCommand.of(loginId, "password123",
                RoomMemberCommand.of("first", "KR", Language.JA),
                RoomMemberCommand.of("second", "JP", Language.KO), 50));
    }

    /** 질문은 "<주제명> 1", "<주제명> 2", "<주제명> 3" */
    private TopicCreateCommand topic(String nameKo, String nameJa) {
        List<QuestionCreateCommand> questions = List.of(1, 2, 3).stream()
                .map(no -> QuestionCreateCommand.from(List.of(
                        LocalizedTextCommand.of(Language.KO, nameKo + " " + no),
                        LocalizedTextCommand.of(Language.JA, nameJa + " " + no))))
                .toList();
        return TopicCreateCommand.of(
                List.of(LocalizedTextCommand.of(Language.KO, nameKo), LocalizedTextCommand.of(Language.JA, nameJa)),
                questions);
    }
}
