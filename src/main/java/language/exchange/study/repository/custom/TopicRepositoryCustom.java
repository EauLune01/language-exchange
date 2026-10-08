package language.exchange.study.repository.custom;

import language.exchange.study.domain.Topic;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface TopicRepositoryCustom {

    /** 그 방의 주제: 아직 안 쓴 주제 먼저, 같은 그룹 안에서는 A 언어 주제명 순 */
    Slice<Topic> findAllUnusedFirst(Long roomId, Pageable pageable);

    /** 그 방에서 사용한 주제만, 오래된 순(1회차부터) */
    Slice<Topic> findAllStudied(Long roomId, Pageable pageable);
}
