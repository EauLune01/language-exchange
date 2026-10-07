package language.exchange.study.repository;

import language.exchange.study.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findAllByTopicIdOrderBySequenceAsc(Long topicId);
}