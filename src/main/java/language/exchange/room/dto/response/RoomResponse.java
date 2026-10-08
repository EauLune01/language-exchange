package language.exchange.room.dto.response;

import language.exchange.room.dto.result.RoomResult;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RoomResponse {

    private String loginId;
    private List<RoomMemberResponse> members;
    private Integer goal;
    private long studiedCount;

    public static RoomResponse from(RoomResult result) {
        return RoomResponse.builder()
                .loginId(result.getLoginId())
                .members(result.getMembers().stream().map(RoomMemberResponse::from).toList())
                .goal(result.getGoal())
                .studiedCount(result.getStudiedCount())
                .build();
    }
}
