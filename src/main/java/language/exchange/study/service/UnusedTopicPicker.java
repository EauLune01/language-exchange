package language.exchange.study.service;

import language.exchange.study.domain.Topic;
import language.exchange.study.repository.TopicPoolRepository;
import language.exchange.study.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** 아직 안 쓴 주제 하나를 무작위로 고릅니다. 뽑기와 패스가 같이 씁니다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnusedTopicPicker {

    private final TopicRepository topicRepository;
    private final TopicPoolRepository topicPoolRepository;

    /**
     * passedId 가 있으면 패스입니다: 그 주제 말고 다른 주제를 고르고, 골랐다면 패스한 주제는 다시 뽑힐 수 있게 됩니다.
     * Redis 를 쓸 수 없으면 DB 에서 고릅니다. (이때는 사용 날짜가 기록되기 전에 다시 뽑으면 같은 주제가 나올 수 있다)
     */
    public Optional<Topic> pick(Long roomId, Long passedId) {
        try {
            return pickFromPool(roomId, passedId);
        } catch (DataAccessException redisError) {
            log.warn("[UnusedTopicPicker] pool unavailable, picking from DB - roomId: {}, cause: {}", roomId, redisError.getMessage());
            return passedId == null
                    ? topicRepository.findRandomUnused(roomId)
                    : topicRepository.findRandomUnusedExcept(roomId, passedId);
        }
    }

    private Optional<Topic> pickFromPool(Long roomId, Long passedId) {
        while (true) {
            long topicId = topicPoolRepository.pop(roomId, passedId);
            if (topicId == TopicPoolRepository.NOT_LOADED) {
                topicPoolRepository.load(roomId, topicRepository.findUnusedIds(roomId));
                continue;
            }
            if (topicId == TopicPoolRepository.EMPTY) {
                // 풀은 비었는데 DB 에 안 쓴 주제가 있으면 풀에서 빠진 주제가 있을 수 있어서, 다음에 뽑을 때 DB 에서 다시 읽게 한다
                // (방금 뽑혀서 아직 사용 날짜가 기록되지 않은 주제는 다시 읽어도 풀에 들어가지 않는다)
                if (topicRepository.existsByRoomIdAndUsedDateIsNull(roomId)) {
                    topicPoolRepository.invalidate(roomId);
                }
                return Optional.empty();
            }
            // 풀에 남아 있던 id 가 이미 쓴 주제이거나 없는 주제면 버리고 다시 고른다
            Optional<Topic> topic = topicRepository.findByIdAndRoomId(topicId, roomId)
                    .filter(found -> found.getUsedDate() == null);
            if (topic.isPresent()) {
                return topic;
            }
        }
    }
}
