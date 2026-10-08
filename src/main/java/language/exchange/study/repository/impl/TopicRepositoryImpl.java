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
    public Slice<Topic> findAllUnusedFirst(Pageable pageable) {
        List<Topic> topics = queryFactory
                .selectFrom(topic)
                .orderBy(
                        usedRank().asc(),
                        topic.nameKo.asc(),
                        topic.id.asc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1L)
                .fetch();
        return SliceUtil.toSlice(topics, pageable);
    }

    @Override
    public Slice<Topic> findAllStudied(Pageable pageable) {
        List<Topic> topics = queryFactory
                .selectFrom(topic)
                .where(isUsed())
                .orderBy(
                        topic.usedDate.asc(),
                        topic.id.asc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1L)
                .fetch();
        return SliceUtil.toSlice(topics, pageable);
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
