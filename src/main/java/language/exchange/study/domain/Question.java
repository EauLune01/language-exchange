package language.exchange.study.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import language.exchange.global.domain.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "questions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"topic_id", "sequence"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(nullable = false)
    private int sequence;

    @Column(name = "content_a", nullable = false)
    private String contentA;

    @Column(name = "content_b", nullable = false)
    private String contentB;

    private Question(Topic topic, int sequence, String contentA, String contentB) {
        this.topic = topic;
        this.sequence = sequence;
        this.contentA = contentA;
        this.contentB = contentB;
    }

    public static Question create(Topic topic, int sequence, String contentA, String contentB) {
        return new Question(topic, sequence, contentA, contentB);
    }
}