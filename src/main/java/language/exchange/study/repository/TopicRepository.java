package language.exchange.study.repository;

import language.exchange.study.domain.Topic;
import language.exchange.study.repository.custom.TopicRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long>, TopicRepositoryCustom {

    Optional<Topic> findFirstByUsedDateBetween(LocalDate start, LocalDate end);

    @Query(value = "SELECT * FROM topics WHERE used_date IS NULL ORDER BY RAND() LIMIT 1",
            nativeQuery = true)
    Optional<Topic> findRandomUnused();
}
