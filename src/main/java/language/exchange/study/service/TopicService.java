package language.exchange.study.service;

import language.exchange.global.constants.study.StudyConstants;
import language.exchange.global.exception.BusinessException;
import language.exchange.global.exception.ErrorCode;
import language.exchange.room.domain.Language;
import language.exchange.room.dto.result.RoomLanguageResult;
import language.exchange.room.service.RoomQueryService;
import language.exchange.study.domain.Question;
import language.exchange.study.domain.Topic;
import language.exchange.study.dto.command.LocalizedTextCommand;
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
    private final RoomQueryService roomQueryService;

    public void requestTopicCreation(Long roomId, TopicCreateCommand command) {
        validate(command, roomQueryService.getLanguages(roomId));
        topicCreatePublisher.publish(TopicCreateEvent.of(roomId, command));
    }

    public void requestTopicBulkCreation(Long roomId, TopicBulkCreateCommand command) {
        RoomLanguageResult languages = roomQueryService.getLanguages(roomId);
        List<TopicCreateCommand> topics = command.getTopics();
        topics.forEach(topic -> validate(topic, languages));
        topics.forEach(topic -> topicCreatePublisher.publish(TopicCreateEvent.of(roomId, topic)));
    }

    /** Consumer가 호출합니다. {lang, text}를 그 방의 A/B 칸으로 옮겨 저장합니다. */
    public void createTopic(Long roomId, TopicCreateCommand command) {
        RoomLanguageResult languages = roomQueryService.getLanguages(roomId);
        validate(command, languages);

        Topic topic = topicRepository.save(Topic.create(
                roomId,
                textOf(command.getNames(), languages.getLanguageA()),
                textOf(command.getNames(), languages.getLanguageB())));

        List<QuestionCreateCommand> questions = command.getQuestions();
        List<Question> questionEntities = IntStream.range(0, questions.size())
                .mapToObj(i -> Question.create(
                        topic,
                        i + 1,
                        textOf(questions.get(i).getContents(), languages.getLanguageA()),
                        textOf(questions.get(i).getContents(), languages.getLanguageB())))
                .toList();
        questionRepository.saveAll(questionEntities);
    }

    public void markAsUsed(Long topicId, LocalDate usedDate) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOPIC_NOT_FOUND));
        topic.markAsUsed(usedDate);
    }

    private void validate(TopicCreateCommand command, RoomLanguageResult languages) {
        List<QuestionCreateCommand> questions = command.getQuestions();
        if (questions == null || questions.size() != StudyConstants.QUESTION_COUNT) {
            throw new BusinessException(ErrorCode.INVALID_QUESTION_COUNT);
        }
        validateLanguages(command.getNames(), languages);
        questions.forEach(question -> validateLanguages(question.getContents(), languages));
    }

    // 방의 두 언어가 정확히 하나씩 있어야 한다
    private void validateLanguages(List<LocalizedTextCommand> texts, RoomLanguageResult languages) {
        textOf(texts, languages.getLanguageA());
        textOf(texts, languages.getLanguageB());
    }

    private String textOf(List<LocalizedTextCommand> texts, Language language) {
        if (texts == null || texts.size() != 2) {
            throw new BusinessException(ErrorCode.LANGUAGE_NOT_IN_ROOM);
        }
        List<LocalizedTextCommand> matched = texts.stream()
                .filter(text -> text.getLang() == language)
                .toList();
        if (matched.size() != 1) {
            throw new BusinessException(ErrorCode.LANGUAGE_NOT_IN_ROOM);
        }
        return matched.get(0).getText();
    }
}
