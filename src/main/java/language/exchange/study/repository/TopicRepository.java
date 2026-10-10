package language.exchange.study.repository;

import language.exchange.study.domain.Topic;
import language.exchange.study.repository.custom.TopicRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// 방이 소유하는 데이터: 방 범위 조회에는 반드시 roomId 조건을 넣는다
public interface TopicRepository extends JpaRepository<Topic, Long>, TopicRepositoryCustom {

    boolean existsByIdAndRoomId(Long id, Long roomId);

    Optional<Topic> findByIdAndRoomId(Long id, Long roomId);

    long countByRoomIdAndUsedDateIsNotNull(Long roomId);

    long countByRoomId(Long roomId);

    boolean existsByRoomIdAndUsedDateIsNull(Long roomId);

    /** 그 방의 아직 안 쓴 주제 id 들 (Redis 풀을 채울 때 쓴다) */
    @Query("SELECT t.id FROM Topic t WHERE t.roomId = :roomId AND t.usedDate IS NULL")
    List<Long> findUnusedIds(@Param("roomId") Long roomId);

    /** 그 방에서 사용한 주제들의 사용 날짜 (통계용). 방 하나의 주제 수는 많지 않아서 날짜만 모두 읽어 서비스에서 센다 */
    @Query("SELECT t.usedDate FROM Topic t WHERE t.roomId = :roomId AND t.usedDate IS NOT NULL")
    List<LocalDate> findUsedDates(@Param("roomId") Long roomId);

    // 아래 두 쿼리는 Redis 풀을 쓸 수 없을 때만 쓴다 (UnusedTopicPicker)
    @Query(value = "SELECT * FROM topics WHERE room_id = :roomId AND used_date IS NULL ORDER BY RAND() LIMIT 1",
            nativeQuery = true)
    Optional<Topic> findRandomUnused(@Param("roomId") Long roomId);

    @Query(value = "SELECT * FROM topics WHERE room_id = :roomId AND used_date IS NULL AND id <> :excludedId ORDER BY RAND() LIMIT 1",
            nativeQuery = true)
    Optional<Topic> findRandomUnusedExcept(@Param("roomId") Long roomId, @Param("excludedId") Long excludedId);
}
