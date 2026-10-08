package language.exchange.room.dto.result;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RoomResult {

    private final String loginId;
    private final List<RoomMemberResult> members;

    public static RoomResult of(String loginId, List<RoomMemberResult> members) {
        return new RoomResult(loginId, members);
    }
}
