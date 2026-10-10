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
import language.exchange.study.dto.request.TopicCreateRequest;
import language.exchange.study.dto.result.LocalizedTextResult;
import language.exchange.study.dto.result.TopicResult;
import language.exchange.study.event.TopicCreateEvent;
import language.exchange.study.publisher.TopicCreatePublisher;
import language.exchange.study.publisher.TopicUsedPublisher;
import language.exchange.study.repository.QuestionRepository;
import language.exchange.study.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional
public class TopicService {

    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final UnusedTopicPicker unusedTopicPicker;
    private final TopicCreatePublisher topicCreatePublisher;
    private final TopicUsedPublisher topicUsedPublisher;
    private final RoomQueryService roomQueryService;
    private final JsonMapper jsonMapper;

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

    /**
     * 기본 추천 주제(default-topics.json) 중 방의 두 언어가 모두 있는 주제를 등록 요청합니다.
     * 해당하는 주제가 없는 언어 조합이면 아무것도 등록하지 않습니다.
     */
    public void requestDefaultTopics(Long roomId) {
        RoomLanguageResult languages = roomQueryService.getLanguages(roomId);
        // 방을 만들 때 한 번만 부르므로 파일은 그때마다 읽는다
        List<TopicCreateCommand> topics = Arrays.stream(jsonMapper.readValue(
                        TopicService.class.getResourceAsStream(StudyConstants.DEFAULT_TOPICS_PATH), TopicCreateRequest[].class))
                .map(TopicCreateRequest::toCommand)
                .map(topic -> keepRoomLanguages(topic, languages))
                .filter(this::hasBothLanguages)
                .toList();
        topics.forEach(topic -> validate(topic, languages));
        topics.forEach(topic -> topicCreatePublisher.publish(TopicCreateEvent.of(roomId, topic)));
    }

    /** Consumer가 호출합니다. {lang, text}를 그 방의 A/B 칸으로 옮겨 저장하고, 만든 주제의 id를 돌려줍니다. */
    public Long createTopic(Long roomId, TopicCreateCommand command) {
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
        return topic.getId();
    }

    public void markAsUsed(Long topicId, LocalDate usedDate) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOPIC_NOT_FOUND));
        topic.markAsUsed(usedDate);
    }

    /**
     * 뽑은 주제를 패스하고 다른 주제를 뽑습니다. 패스한 주제는 안 쓴 주제로 돌아가 나중에 다시 뽑힐 수 있습니다.
     * 대신 뽑을 주제가 없으면 아무것도 바꾸지 않습니다.
     */
    public TopicResult passTopic(Long roomId, Long topicId) {
        RoomLanguageResult languages = roomQueryService.getLanguages(roomId);
        // 다른 방의 주제는 없는 주제와 똑같이 404 (존재 여부를 숨긴다)
        Topic passed = topicRepository.findByIdAndRoomId(topicId, roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOPIC_NOT_FOUND));
        Topic picked = unusedTopicPicker.pick(roomId, topicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AVAILABLE_TOPIC));

        // ponytail: 뽑자마자 패스해서 사용 처리(큐)보다 먼저 실행되면 패스한 주제가 뒤늦게 사용 처리된다.
        //           사람이 누르는 속도로는 일어나기 어렵다. 문제가 되면 패스도 같은 큐로 보내 순서를 맞춘다
        passed.pass();
        topicUsedPublisher.publish(picked.getId(), LocalDate.now());
        return TopicResult.of(picked.getId(), List.of(
                LocalizedTextResult.of(languages.getLanguageA(), picked.getNameA()),
                LocalizedTextResult.of(languages.getLanguageB(), picked.getNameB())));
    }

    private TopicCreateCommand keepRoomLanguages(TopicCreateCommand topic, RoomLanguageResult languages) {
        return TopicCreateCommand.of(
                keepRoomLanguages(topic.getNames(), languages),
                topic.getQuestions().stream()
                        .map(question -> QuestionCreateCommand.from(keepRoomLanguages(question.getContents(), languages)))
                        .toList());
    }

    private List<LocalizedTextCommand> keepRoomLanguages(List<LocalizedTextCommand> texts, RoomLanguageResult languages) {
        return texts.stream().filter(text -> languages.contains(text.getLang())).toList();
    }

    private boolean hasBothLanguages(TopicCreateCommand topic) {
        return topic.getNames().size() == 2
                && topic.getQuestions().stream().allMatch(question -> question.getContents().size() == 2);
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
