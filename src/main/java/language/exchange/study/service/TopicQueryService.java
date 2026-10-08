package language.exchange.study.service;

import language.exchange.global.exception.BusinessException;
import language.exchange.global.exception.ErrorCode;
import language.exchange.global.util.WeekUtils;
import language.exchange.study.domain.Question;
import language.exchange.study.domain.Topic;
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

    public TopicResult getWeeklyTopic() {
        LocalDate today = LocalDate.now();

        Optional<TopicResult> current = findThisWeekTopic(today);
        if (current.isPresent()) {
            return current.get();
        }

        Topic picked = topicRepository.findRandomUnused()
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AVAILABLE_TOPIC));

        topicUsedPublisher.publish(picked.getId(), today);
        return toTopicResult(picked);
    }

    /** 이번 주에 이미 뽑힌 주제만 조회합니다. 뽑지도, 사용 처리도 하지 않습니다. */
    public Optional<TopicResult> getThisWeekTopic() {
        return findThisWeekTopic(LocalDate.now());
    }

    public Slice<TopicSummaryResult> getTopics(Pageable pageable) {
        return topicRepository.findAllUnusedFirst(pageable)
                .map(this::toTopicSummaryResult);
    }

    public Slice<TopicHistoryResult> getTopicHistory(Pageable pageable) {
        Slice<Topic> topics = topicRepository.findAllStudied(pageable);

        // 오래된 순이므로 이 페이지 첫 항목의 회차 = 앞 페이지에서 이미 보낸 개수 + 1
        long firstRound = pageable.getOffset() + 1;
        List<Topic> content = topics.getContent();

        List<TopicHistoryResult> results = IntStream.range(0, content.size())
                .mapToObj(i -> toTopicHistoryResult(content.get(i), firstRound + i))
                .toList();
        return new SliceImpl<>(results, pageable, topics.hasNext());
    }

    public QuestionListResult getQuestions(Long topicId) {
        if (!topicRepository.existsById(topicId)) {
            throw new BusinessException(ErrorCode.TOPIC_NOT_FOUND);
        }
        List<QuestionResult> questions = questionRepository
                .findAllByTopicIdOrderBySequenceAsc(topicId).stream()
                .map(this::toQuestionResult)
                .toList();
        return QuestionListResult.of(topicId, questions);
    }

    private Optional<TopicResult> findThisWeekTopic(LocalDate today) {
        return topicRepository.findFirstByUsedDateBetween(
                        WeekUtils.startOfWeek(today),
                        WeekUtils.endOfWeek(today))
                .map(this::toTopicResult);
    }

    private TopicResult toTopicResult(Topic topic) {
        return TopicResult.of(topic.getId(), topic.getNameKo(), topic.getNameJa());
    }

    private TopicSummaryResult toTopicSummaryResult(Topic topic) {
        return TopicSummaryResult.of(
                topic.getId(),
                topic.getNameKo(),
                topic.getNameJa(),
                topic.getUsedDate());
    }

    private TopicHistoryResult toTopicHistoryResult(Topic topic, long round) {
        return TopicHistoryResult.of(
                topic.getId(),
                round,
                topic.getNameKo(),
                topic.getNameJa(),
                topic.getUsedDate());
    }

    private QuestionResult toQuestionResult(Question question) {
        return QuestionResult.of(
                question.getSequence(),
                question.getContentKo(),
                question.getContentJa());
    }
}