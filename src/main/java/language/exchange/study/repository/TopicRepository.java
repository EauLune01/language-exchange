package language.exchange.study.repository;

import language.exchange.study.domain.Topic;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    Optional<Topic> findFirstByUsedDateBetween(LocalDate start, LocalDate end);

    @Query(value = "SELECT * FROM topic WHERE used_date IS NULL ORDER BY RAND() LIMIT 1",
            nativeQuery = true)
    Optional<Topic> findRandomUnused();

    Slice<Topic> findAllBy(Pageable pageable);
}
