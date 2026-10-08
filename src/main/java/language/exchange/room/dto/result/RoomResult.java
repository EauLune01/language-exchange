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
    private final Integer goal;
    private final long studiedCount;

    public static RoomResult of(String loginId, List<RoomMemberResult> members, Integer goal, long studiedCount) {
        return new RoomResult(loginId, members, goal, studiedCount);
    }
}
