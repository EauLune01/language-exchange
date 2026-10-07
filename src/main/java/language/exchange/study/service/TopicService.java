package language.exchange.study.service;

import language.exchange.global.constant.AppConstants;
import language.exchange.global.exception.BusinessException;
import language.exchange.global.exception.ErrorCode;
import language.exchange.study.domain.Question;
import language.exchange.study.domain.Topic;
import language.exchange.study.dto.command.QuestionCreateCommand;
import language.exchange.study.dto.command.TopicBulkCreateCommand;
import language.exchange.study.dto.command.TopicCreateCommand;
import language.exchange.study.event.TopicCreateEvent;
import language.exchange.study.publisher.TopicCreatePublisher;
import language.exchange.study.repository.QuestionRepository;
import language.exchange.study.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional
public class TopicService {

    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final TopicCreatePublisher topicCreatePublisher;

    public void requestTopicCreation(TopicCreateCommand command) {
        validateQuestionCount(command.getQuestions());
        topicCreatePublisher.publish(TopicCreateEvent.from(command));
    }

    public void requestTopicBulkCreation(TopicBulkCreateCommand command) {
        List<TopicCreateCommand> topics = command.getTopics();
        topics.forEach(topic -> validateQuestionCount(topic.getQuestions()));
        topics.forEach(topic -> topicCreatePublisher.publish(TopicCreateEvent.from(topic)));
    }

    public void createTopic(TopicCreateCommand command) {
        List<QuestionCreateCommand> questions = command.getQuestions();
        validateQuestionCount(questions);

        Topic topic = topicRepository.save(
                Topic.create(command.getNameKo(), command.getNameJa()));

        List<Question> questionEntities = IntStream.range(0, questions.size())
                .mapToObj(i -> Question.create(
                        topic, i + 1, questions.get(i).getContentKo(), questions.get(i).getContentJa()))
                .toList();
        questionRepository.saveAll(questionEntities);
    }

    public void markAsUsed(Long topicId, LocalDate usedDate) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOPIC_NOT_FOUND));
        topic.markAsUsed(usedDate);
    }

    private void validateQuestionCount(List<QuestionCreateCommand> questions) {
        if (questions == null || questions.size() != AppConstants.QUESTION_COUNT) {
            throw new BusinessException(ErrorCode.INVALID_QUESTION_COUNT);
        }
    }
}