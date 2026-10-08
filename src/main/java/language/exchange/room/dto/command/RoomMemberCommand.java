package language.exchange.room.dto.command;

import language.exchange.room.domain.Language;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RoomMemberCommand {

    private final String name;
    private final String nationality;
    private final Language learningLanguage;

    public static RoomMemberCommand of(String name, String nationality, Language learningLanguage) {
        return new RoomMemberCommand(name, nationality, learningLanguage);
    }
}
