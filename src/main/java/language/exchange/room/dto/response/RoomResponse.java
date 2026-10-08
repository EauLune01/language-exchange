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

    public static RoomResponse from(RoomResult result) {
        return RoomResponse.builder()
                .loginId(result.getLoginId())
                .members(result.getMembers().stream().map(RoomMemberResponse::from).toList())
                .build();
    }
}
