package language.exchange.study.service;

import language.exchange.global.exception.BusinessException;
import language.exchange.global.exception.ErrorCode;
import language.exchange.global.util.WeekUtils;
import language.exchange.room.domain.Language;
import language.exchange.room.dto.result.RoomLanguageResult;
import language.exchange.room.service.RoomQueryService;
import language.exchange.study.domain.Question;
import language.exchange.study.domain.Topic;
import language.exchange.study.dto.result.LocalizedTextResult;
import language.exchange.study.dto.result.QuestionListResult;
import language.exchange.study.dto.result.QuestionResult;
import language.exchange.study.dto.result.TopicHistoryResult;
import language.exchange.study.dto.result.TopicResult;
import language.exchange.study.dto.result.TopicSummaryResult;
import language.exchange.study.publisher.TopicUsedPublisher;
import language.exchange.study.repository.QuestionRepository;
import language.exchange.study.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicQueryService {

    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final TopicUsedPublisher topicUsedPublisher;
    private final RoomQueryService roomQueryService;

    public TopicResult getWeeklyTopic(Long roomId) {
        RoomLanguageResult languages = roomQueryService.getLanguages(roomId);
        LocalDate today = LocalDate.now();

        Optional<Topic> current = findThisWeekTopic(roomId, today);
        if (current.isPresent()) {
            return toTopicResult(current.get(), languages);
        }

        Topic picked = topicRepository.findRandomUnused(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AVAILABLE_TOPIC));

        topicUsedPublisher.publish(picked.getId(), today);
        return toTopicResult(picked, languages);
    }

    /** 이번 주에 이미 뽑힌 주제만 조회합니다. 뽑지도, 사용 처리도 하지 않습니다. */
    public Optional<TopicResult> getThisWeekTopic(Long roomId) {
        RoomLanguageResult languages = roomQueryService.getLanguages(roomId);
        return findThisWeekTopic(roomId, LocalDate.now())
                .map(topic -> toTopicResult(topic, languages));
    }

    public Slice<TopicSummaryResult> getTopics(Long roomId, Pageable pageable) {
        RoomLanguageResult languages = roomQueryService.getLanguages(roomId);
        return topicRepository.findAllUnusedFirst(roomId, pageable)
                .map(topic -> TopicSummaryResult.of(topic.getId(), toNames(topic, languages), topic.getUsedDate()));
    }

    public Slice<TopicHistoryResult> getTopicHistory(Long roomId, Pageable pageable) {
        RoomLanguageResult languages = roomQueryService.getLanguages(roomId);
        Slice<Topic> topics = topicRepository.findAllStudied(roomId, pageable);

        // 오래된 순이므로 이 페이지 첫 항목의 회차 = 앞 페이지에서 이미 보낸 개수 + 1
        long firstRound = pageable.getOffset() + 1;
        List<Topic> content = topics.getContent();

        List<TopicHistoryResult> results = IntStream.range(0, content.size())
                .mapToObj(i -> TopicHistoryResult.of(
                        content.get(i).getId(),
                        firstRound + i,
                        toNames(content.get(i), languages),
                        content.get(i).getUsedDate()))
                .toList();
        return new SliceImpl<>(results, pageable, topics.hasNext());
    }

    public QuestionListResult getQuestions(Long roomId, Long topicId, Language language) {
        RoomLanguageResult languages = roomQueryService.getLanguages(roomId);
        if (!languages.contains(language)) {
            throw new BusinessException(ErrorCode.LANGUAGE_NOT_IN_ROOM);
        }
        // 다른 방의 주제는 없는 주제와 똑같이 404 (존재 여부를 숨긴다)
        if (!topicRepository.existsByIdAndRoomId(topicId, roomId)) {
            throw new BusinessException(ErrorCode.TOPIC_NOT_FOUND);
        }

        boolean isLanguageA = language == languages.getLanguageA();
        List<QuestionResult> questions = questionRepository
                .findAllByTopicIdOrderBySequenceAsc(topicId).stream()
                .map(question -> toQuestionResult(question, isLanguageA))
                .toList();
        return QuestionListResult.of(topicId, language, questions);
    }

    private Optional<Topic> findThisWeekTopic(Long roomId, LocalDate today) {
        return topicRepository.findFirstByRoomIdAndUsedDateBetweenOrderByUsedDateAscIdAsc(
                roomId,
                WeekUtils.startOfWeek(today),
                WeekUtils.endOfWeek(today));
    }

    private TopicResult toTopicResult(Topic topic, RoomLanguageResult languages) {
        return TopicResult.of(topic.getId(), toNames(topic, languages));
    }

    // A/B 칸을 {lang, text}로 풀어서 내보낸다 (A, B 순서)
    private List<LocalizedTextResult> toNames(Topic topic, RoomLanguageResult languages) {
        return List.of(
                LocalizedTextResult.of(languages.getLanguageA(), topic.getNameA()),
                LocalizedTextResult.of(languages.getLanguageB(), topic.getNameB()));
    }

    private QuestionResult toQuestionResult(Question question, boolean isLanguageA) {
        return QuestionResult.of(
                question.getSequence(),
                isLanguageA ? question.getContentA() : question.getContentB());
    }
}
