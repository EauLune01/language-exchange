package language.exchange.study.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import language.exchange.global.domain.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Table(name = "topics",
        indexes = @Index(name = "idx_topics_room_used_date", columnList = "room_id, used_date"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Topic extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 다른 도메인(room)은 id로만 참조
    @Column(nullable = false)
    private Long roomId;

    // A/B = 방의 languageA / languageB 칸
    // (nameA 처럼 대문자 한 글자로 끝나는 필드는 기본 네이밍 전략이 namea 로 만들기 때문에 컬럼명을 직접 적는다)
    @Column(name = "name_a", nullable = false)
    private String nameA;

    @Column(name = "name_b", nullable = false)
    private String nameB;

    private LocalDate usedDate;

    private Topic(Long roomId, String nameA, String nameB) {
        this.roomId = roomId;
        this.nameA = nameA;
        this.nameB = nameB;
    }

    public static Topic create(Long roomId, String nameA, String nameB) {
        return new Topic(roomId, nameA, nameB);
    }

    // 멱등: 이미 사용 처리된 주제는 날짜를 덮어쓰지 않음
    public void markAsUsed(LocalDate date) {
        if (this.usedDate == null) {
            this.usedDate = date;
        }
    }
}
