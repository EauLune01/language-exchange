package language.exchange.study.service;

import language.exchange.global.exception.BusinessException;
import language.exchange.global.exception.ErrorCode;
import language.exchange.study.domain.Topic;
import language.exchange.study.dto.result.QuestionListResult;
import language.exchange.study.dto.result.QuestionResult;
import language.exchange.study.dto.result.TopicResult;
import language.exchange.study.dto.result.TopicSummaryResult;
import language.exchange.study.publisher.TopicUsedPublisher;
import language.exchange.study.repository.QuestionRepository;
import language.exchange.study.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicQueryService {

    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final TopicUsedPublisher topicUsedPublisher;

    public TopicResult getWeeklyTopic() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        Optional<Topic> current = topicRepository.findFirstByUsedDateBetween(weekStart, weekEnd);
        if (current.isPresent()) {
            return TopicResult.from(current.get());
        }

        Topic picked = topicRepository.findRandomUnused()
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AVAILABLE_TOPIC));

        topicUsedPublisher.publish(picked.getId(), today);
        return TopicResult.from(picked);
    }

    public QuestionListResult getQuestions(Long topicId) {
        if (!topicRepository.existsById(topicId)) {
            throw new BusinessException(ErrorCode.TOPIC_NOT_FOUND);
        }
        List<QuestionResult> questions = questionRepository
                .findAllByTopicIdOrderBySequenceAsc(topicId).stream()
                .map(QuestionResult::from)
                .toList();
        return QuestionListResult.builder()
                .topicId(topicId)
                .questions(questions)
                .build();
    }

    public Slice<TopicSummaryResult> getTopics(Pageable pageable) {
        return topicRepository.findAllBy(pageable)
                .map(TopicSummaryResult::from);
    }
}