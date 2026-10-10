package language.exchange.study.service;

import language.exchange.room.domain.Language;
import language.exchange.room.dto.command.RoomCreateCommand;
import language.exchange.room.dto.command.RoomMemberCommand;
import language.exchange.room.service.RoomService;
import language.exchange.study.event.LocalizedTextEvent;
import language.exchange.study.event.TopicCreateEvent;
import language.exchange.study.publisher.TopicCreatePublisher;
import language.exchange.study.publisher.TopicUsedPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * 기본 추천 주제(default-topics.json): 어떤 언어 조합의 방이든 그 방의 두 언어로 등록 요청되는지 확인합니다.
 * 파일의 주제가 등록 규칙(질문 3개, 두 언어)을 지키는지도 여기서 같이 걸러집니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DefaultTopicsTest {

    @Autowired
    private RoomService roomService;
    @Autowired
    private TopicService topicService;

    @MockitoBean
    private TopicCreatePublisher topicCreatePublisher;
    @MockitoBean
    private TopicUsedPublisher topicUsedPublisher;

    @ParameterizedTest
    @CsvSource({"JA, KO", "TH, AR", "EN, FR"})
    @DisplayName("기본 주제는 그 방의 두 언어로만 등록 요청된다")
    void defaultTopicsAreRequestedInRoomLanguages(Language learningA, Language learningB) {
        Long roomId = createRoom(learningA, learningB);

        topicService.requestDefaultTopics(roomId);

        ArgumentCaptor<TopicCreateEvent> events = ArgumentCaptor.forClass(TopicCreateEvent.class);
        verify(topicCreatePublisher, atLeastOnce()).publish(events.capture());
        assertThat(events.getAllValues()).allSatisfy(event -> {
            assertThat(event.getRoomId()).isEqualTo(roomId);
            assertThat(event.getNames()).extracting(LocalizedTextEvent::getLang)
                    .containsExactlyInAnyOrder(learningA, learningB);
            assertThat(event.getQuestions()).hasSize(3);
        });
    }

    private Long createRoom(Language learningA, Language learningB) {
        String loginId = "t-" + UUID.randomUUID().toString().substring(0, 12);
        return roomService.createRoom(RoomCreateCommand.of(loginId, "password123",
                RoomMemberCommand.of("first", "KR", learningA),
                RoomMemberCommand.of("second", "JP", learningB), 50));
    }
}
