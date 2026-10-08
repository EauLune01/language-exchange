package language.exchange.room.dto.result;

import language.exchange.room.domain.Language;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RoomMemberResult {

    private final String name;
    private final String nationality;
    private final Language language;
    private final Language learningLanguage;

    public static RoomMemberResult of(String name, String nationality, Language language, Language learningLanguage) {
        return new RoomMemberResult(name, nationality, language, learningLanguage);
    }
}
