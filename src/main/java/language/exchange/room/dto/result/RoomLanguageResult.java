package language.exchange.room.dto.result;

import language.exchange.room.domain.Language;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 방의 두 언어 칸. A/B는 내부 표현이라 API 바깥으로는 {lang, text}로 풀어서 내보낸다. */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RoomLanguageResult {

    private final Language languageA;
    private final Language languageB;

    public static RoomLanguageResult of(Language languageA, Language languageB) {
        return new RoomLanguageResult(languageA, languageB);
    }

    public boolean contains(Language language) {
        return language == languageA || language == languageB;
    }
}
