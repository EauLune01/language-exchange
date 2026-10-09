package language.exchange.study.service;

import language.exchange.room.domain.Language;
import language.exchange.room.dto.command.RoomCreateCommand;
import language.exchange.room.dto.command.RoomMemberCommand;
import language.exchange.room.service.RoomService;
import language.exchange.study.dto.command.LocalizedTextCommand;
import language.exchange.study.dto.command.QuestionCreateCommand;
import language.exchange.study.dto.command.TopicCreateCommand;
import language.exchange.study.dto.result.MonthlyCountResult;
import language.exchange.study.dto.result.StatsResult;
import language.exchange.study.dto.result.TopicSummaryResult;
import language.exchange.study.publisher.TopicCreatePublisher;
import language.exchange.study.publisher.TopicUsedPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 방 통계: 사용 날짜로 센 값과 방 격리. (test 프로파일: 메모리 DB, 큐에는 연결하지 않습니다) */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StatsQueryServiceTest {

    @Autowired
    private RoomService roomService;
    @Autowired
    private TopicService topicService;
    @Autowired
    private TopicQueryService topicQueryService;
    @Autowired
    private StatsQueryService statsQueryService;

    @MockitoBean
    private TopicCreatePublisher topicCreatePublisher;
    @MockitoBean
    private TopicUsedPublisher topicUsedPublisher;

    @Test
    @DisplayName("주제 수, 총 회차, 이번 달 횟수, 첫 날짜, 최근 6개월 월별 횟수를 센다")
    void countsFromUsedDates() {
        Long roomId = createRoom();
        List.of("가", "나", "다", "라").forEach(name -> topicService.createTopic(roomId, topic(name)));
        List<Long> topicIds = topicQueryService.getTopics(roomId, PageRequest.of(0, 20)).getContent().stream()
                .map(TopicSummaryResult::getId).toList();

        // 이번 달 1번, 지난달 1번, 그래프 범위 밖(7개월 전) 1번, 나머지 1개는 아직 안 씀
        LocalDate thisMonth = YearMonth.now().atDay(1);
        LocalDate oldest = thisMonth.minusMonths(7);
        topicService.markAsUsed(topicIds.get(0), thisMonth);
        topicService.markAsUsed(topicIds.get(1), thisMonth.minusMonths(1));
        topicService.markAsUsed(topicIds.get(2), oldest);

        StatsResult stats = statsQueryService.getStats(roomId);

        assertThat(stats.getTotalTopics()).isEqualTo(4);
        assertThat(stats.getUsedTopics()).isEqualTo(3);
        assertThat(stats.getThisMonthCount()).isEqualTo(1);
        assertThat(stats.getFirstUsedDate()).isEqualTo(oldest);
        assertThat(stats.getMonthly()).extracting(MonthlyCountResult::getCount)
                .containsExactly(0L, 0L, 0L, 0L, 1L, 1L);
        assertThat(stats.getMonthly().get(5).getMonth()).isEqualTo(YearMonth.now().toString());
    }

    @Test
    @DisplayName("다른 방의 주제는 통계에 섞이지 않고, 아무것도 없는 방은 모두 0이다")
    void statsAreScopedToRoom() {
        Long roomId = createRoom();
        topicService.createTopic(roomId, topic("가"));
        topicService.markAsUsed(
                topicQueryService.getTopics(roomId, PageRequest.of(0, 20)).getContent().get(0).getId(), LocalDate.now());

        StatsResult empty = statsQueryService.getStats(createRoom());

        assertThat(empty.getTotalTopics()).isZero();
        assertThat(empty.getUsedTopics()).isZero();
        assertThat(empty.getThisMonthCount()).isZero();
        assertThat(empty.getWeekStreak()).isZero();
        assertThat(empty.getFirstUsedDate()).isNull();
        assertThat(empty.getMonthly()).hasSize(6).allMatch(month -> month.getCount() == 0);
    }

    @Test
    @DisplayName("연속 주: 이번 주에 아직 안 했어도 지난주까지 이어 왔으면 세고, 거른 주에서 끊긴다")
    void weekStreakCountsConsecutiveWeeks() {
        Long roomId = createRoom();
        List.of("가", "나", "다", "라").forEach(name -> topicService.createTopic(roomId, topic(name)));
        List<Long> topicIds = topicQueryService.getTopics(roomId, PageRequest.of(0, 20)).getContent().stream()
                .map(TopicSummaryResult::getId).toList();

        // 지난주, 2주 전, 3주 전은 했고 4주 전은 거르고 5주 전에 한 번
        LocalDate today = LocalDate.now();
        topicService.markAsUsed(topicIds.get(0), today.minusWeeks(1));
        topicService.markAsUsed(topicIds.get(1), today.minusWeeks(2));
        topicService.markAsUsed(topicIds.get(2), today.minusWeeks(3));
        topicService.markAsUsed(topicIds.get(3), today.minusWeeks(5));

        assertThat(statsQueryService.getStats(roomId).getWeekStreak()).isEqualTo(3);
    }

    private Long createRoom() {
        String loginId = "t-" + UUID.randomUUID().toString().substring(0, 12);
        return roomService.createRoom(RoomCreateCommand.of(loginId, "password123",
                RoomMemberCommand.of("first", "KR", Language.JA),
                RoomMemberCommand.of("second", "JP", Language.KO), 50));
    }

    private static List<LocalizedTextCommand> texts(String text) {
        return List.of(LocalizedTextCommand.of(Language.KO, text), LocalizedTextCommand.of(Language.JA, text));
    }

    private static TopicCreateCommand topic(String name) {
        return TopicCreateCommand.of(texts(name), List.of(1, 2, 3).stream()
                .map(no -> QuestionCreateCommand.from(texts(name + " " + no)))
                .toList());
    }
}
