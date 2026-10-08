package language.exchange.room.service;

import language.exchange.global.exception.BusinessException;
import language.exchange.global.exception.ErrorCode;
import language.exchange.room.domain.Room;
import language.exchange.room.dto.result.RoomMemberResult;
import language.exchange.room.dto.result.RoomResult;
import language.exchange.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomQueryService {

    private final RoomRepository roomRepository;

    public RoomResult getRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
        return toRoomResult(room);
    }

    private RoomResult toRoomResult(Room room) {
        return RoomResult.of(room.getLoginId(), List.of(
                RoomMemberResult.of(room.getNameA(), room.getNationalityA(), room.getLanguageA()),
                RoomMemberResult.of(room.getNameB(), room.getNationalityB(), room.getLanguageB())));
    }
}
