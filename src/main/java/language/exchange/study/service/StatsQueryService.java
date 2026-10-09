package language.exchange.study.service;

import language.exchange.global.constants.study.StudyConstants;
import language.exchange.study.dto.result.MonthlyCountResult;
import language.exchange.study.dto.result.StatsResult;
import language.exchange.study.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsQueryService {

    private final TopicRepository topicRepository;

    /** 방의 학습 통계. 따로 저장하는 값 없이 주제의 사용 날짜로 조회할 때 계산합니다. */
    public StatsResult getStats(Long roomId) {
        List<LocalDate> usedDates = topicRepository.findUsedDates(roomId);
        Map<YearMonth, Long> countByMonth = usedDates.stream()
                .collect(Collectors.groupingBy(YearMonth::from, Collectors.counting()));

        // 한 번도 안 한 달도 0으로 채워서, 오래된 달부터 이번 달까지 빈칸 없이 내려준다
        YearMonth thisMonth = YearMonth.now();
        List<MonthlyCountResult> monthly = IntStream.range(0, StudyConstants.STATS_MONTHS)
                .mapToObj(i -> thisMonth.minusMonths(StudyConstants.STATS_MONTHS - 1 - i))
                .map(month -> MonthlyCountResult.of(month.toString(), countByMonth.getOrDefault(month, 0L)))
                .toList();

        return StatsResult.of(
                topicRepository.countByRoomId(roomId),
                usedDates.size(),
                countByMonth.getOrDefault(thisMonth, 0L),
                weekStreak(usedDates),
                usedDates.stream().min(LocalDate::compareTo).orElse(null),
                monthly);
    }

    // 주는 월~일. 이번 주에 아직 안 했어도 지난주까지 이어 왔다면 끊긴 것으로 보지 않는다
    private int weekStreak(List<LocalDate> usedDates) {
        Set<LocalDate> studiedWeeks = usedDates.stream()
                .map(date -> date.with(DayOfWeek.MONDAY))
                .collect(Collectors.toSet());

        LocalDate week = LocalDate.now().with(DayOfWeek.MONDAY);
        if (!studiedWeeks.contains(week)) {
            week = week.minusWeeks(1);
        }
        int streak = 0;
        while (studiedWeeks.contains(week)) {
            streak++;
            week = week.minusWeeks(1);
        }
        return streak;
    }
}
