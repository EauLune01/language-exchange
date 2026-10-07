package language.exchange.study.domain;

import jakarta.persistence.*;
import language.exchange.global.domain.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Topic extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nameKo;

    @Column(nullable = false)
    private String nameJa;

    private LocalDate usedDate;

    public static Topic create(String nameKo, String nameJa) {
        Topic topic = new Topic();
        topic.nameKo = nameKo;
        topic.nameJa = nameJa;
        return topic;
    }

    public void markAsUsed(LocalDate date) {
        if (this.usedDate == null) {
            this.usedDate = date;
        }
    }
}
