package language.exchange.room.dto.command;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RoomCreateCommand {

    private final String loginId;
    private final String password;
    private final RoomMemberCommand memberA;
    private final RoomMemberCommand memberB;

    public static RoomCreateCommand of(String loginId, String password,
                                       RoomMemberCommand memberA, RoomMemberCommand memberB) {
        return new RoomCreateCommand(loginId, password, memberA, memberB);
    }
}
