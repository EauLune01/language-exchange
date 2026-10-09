package language.exchange.study.note.domain;

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
import language.exchange.study.domain.Question;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "notes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"question_id", "room_id", "lang"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Note extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    // 다른 도메인(room)은 id로만 참조
    @Column(nullable = false)
    private Long roomId;

    // 메모를 적은 질문 언어 (Language 이름: KO, JA …). 같은 질문이어도 언어마다 메모가 따로 있다
    @Column(nullable = false, length = 5)
    private String lang;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private Note(Question question, Long roomId, String lang, String content) {
        this.question = question;
        this.roomId = roomId;
        this.lang = lang;
        this.content = content;
    }

    public static Note create(Question question, Long roomId, String lang, String content) {
        return new Note(question, roomId, lang, content);
    }

    public void updateContent(String content) {
        this.content = content;
    }
}
