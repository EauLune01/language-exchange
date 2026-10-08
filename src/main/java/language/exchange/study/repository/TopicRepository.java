package language.exchange.study.repository;

import language.exchange.study.domain.Topic;
import language.exchange.study.repository.custom.TopicRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

// 방이 소유하는 데이터: 방 범위 조회에는 반드시 roomId 조건을 넣는다
public interface TopicRepository extends JpaRepository<Topic, Long>, TopicRepositoryCustom {

    boolean existsByIdAndRoomId(Long id, Long roomId);

    Optional<Topic> findFirstByRoomIdAndUsedDateBetweenOrderByUsedDateAscIdAsc(Long roomId, LocalDate start, LocalDate end);

    @Query(value = "SELECT * FROM topics WHERE room_id = :roomId AND used_date IS NULL ORDER BY RAND() LIMIT 1",
            nativeQuery = true)
    Optional<Topic> findRandomUnused(@Param("roomId") Long roomId);
}
