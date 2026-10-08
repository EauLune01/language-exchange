package language.exchange.study.repository.custom;

import language.exchange.study.domain.Topic;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface TopicRepositoryCustom {

    /** 아직 안 쓴 주제 먼저, 같은 그룹 안에서는 한국어 주제명 가나다순 */
    Slice<Topic> findAllUnusedFirst(Pageable pageable);

    /** 사용한 주제만, 오래된 순(1회차부터) */
    Slice<Topic> findAllStudied(Pageable pageable);
}