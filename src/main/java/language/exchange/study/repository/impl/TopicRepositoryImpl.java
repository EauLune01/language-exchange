package language.exchange.study.repository.impl;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import language.exchange.global.util.SliceUtil;
import language.exchange.study.domain.Topic;
import language.exchange.study.repository.custom.TopicRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

import static language.exchange.study.domain.QTopic.topic;

@RequiredArgsConstructor
public class TopicRepositoryImpl implements TopicRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<Topic> findAllUnusedFirst(Long roomId, Pageable pageable) {
        List<Topic> topics = queryFactory
                .selectFrom(topic)
                .where(roomEq(roomId))
                .orderBy(
                        usedRank().asc(),
                        topic.nameA.asc(),
                        topic.id.asc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1L)
                .fetch();
        return SliceUtil.toSlice(topics, pageable);
    }

    @Override
    public Slice<Topic> findAllStudied(Long roomId, Pageable pageable) {
        List<Topic> topics = queryFactory
                .selectFrom(topic)
                .where(roomEq(roomId), isUsed())
                .orderBy(
                        topic.usedDate.asc(),
                        topic.id.asc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1L)
                .fetch();
        return SliceUtil.toSlice(topics, pageable);
    }

    // null을 돌려주면 조건이 사라져 다른 방 데이터가 보이므로 roomId가 null이면 여기서 예외가 난다
    private BooleanExpression roomEq(Long roomId) {
        return topic.roomId.eq(roomId);
    }

    // 안 쓴 주제(usedDate가 null) = 0, 쓴 주제 = 1
    private NumberExpression<Integer> usedRank() {
        return new CaseBuilder()
                .when(topic.usedDate.isNull()).then(0)
                .otherwise(1);
    }

    private BooleanExpression isUsed() {
        return topic.usedDate.isNotNull();
    }
}
