package language.exchange.study.domain;

import jakarta.persistence.*;
import language.exchange.global.domain.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"topic_id", "sequence"}))
public class Question extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(nullable = false)
    private int sequence;

    @Column(nullable = false)
    private String contentKo;

    @Column(nullable = false)
    private String contentJa;

    public static Question create(Topic topic, int sequence, String contentKo, String contentJa) {
        Question question = new Question();
        question.topic = topic;
        question.sequence = sequence;
        question.contentKo = contentKo;
        question.contentJa = contentJa;
        return question;
    }
}
