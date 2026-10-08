package language.exchange.study.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import language.exchange.global.domain.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Table(name = "topics")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Topic extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nameKo;

    @Column(nullable = false)
    private String nameJa;

    private LocalDate usedDate;

    private Topic(String nameKo, String nameJa) {
        this.nameKo = nameKo;
        this.nameJa = nameJa;
    }

    public static Topic create(String nameKo, String nameJa) {
        return new Topic(nameKo, nameJa);
    }

    // 멱등: 이미 사용 처리된 주제는 날짜를 덮어쓰지 않음
    public void markAsUsed(LocalDate date) {
        if (this.usedDate == null) {
            this.usedDate = date;
        }
    }
}