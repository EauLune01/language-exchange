package language.exchange.room.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import language.exchange.global.domain.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Table(name = "rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String loginId;

    @Column(nullable = false)
    private String passwordHash;

    // MySQL ENUM 컬럼이 되면 언어를 추가할 때 스키마를 바꿔야 해서 VARCHAR로 고정
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "language_a", nullable = false, length = 10)
    private Language languageA;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "language_b", nullable = false, length = 10)
    private Language languageB;

    @Column(name = "a_name", nullable = false, length = 50)
    private String nameA;

    // 국적은 ISO 3166-1 alpha-2 국가 코드 (KR, JP …)
    @Column(name = "a_nationality", nullable = false, length = 2)
    private String nationalityA;

    @Column(name = "b_name", nullable = false, length = 50)
    private String nameB;

    @Column(name = "b_nationality", nullable = false, length = 2)
    private String nationalityB;

    private Room(String loginId, String passwordHash, Language languageA, Language languageB,
                 String nameA, String nationalityA, String nameB, String nationalityB) {
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.languageA = languageA;
        this.languageB = languageB;
        this.nameA = nameA;
        this.nationalityA = nationalityA;
        this.nameB = nameB;
        this.nationalityB = nationalityB;
    }

    public static Room create(String loginId, String passwordHash, Language languageA, Language languageB,
                              String nameA, String nationalityA, String nameB, String nationalityB) {
        return new Room(loginId, passwordHash, languageA, languageB, nameA, nationalityA, nameB, nationalityB);
    }
}
