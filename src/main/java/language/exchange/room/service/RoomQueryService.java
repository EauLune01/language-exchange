package language.exchange.room.service;

import language.exchange.global.exception.BusinessException;
import language.exchange.global.exception.ErrorCode;
import language.exchange.room.domain.Room;
import language.exchange.room.dto.result.RoomLanguageResult;
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

    /** 다른 도메인이 방의 두 언어(A/B)를 알아야 할 때 씁니다. */
    public RoomLanguageResult getLanguages(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
        return RoomLanguageResult.of(room.getLanguageA(), room.getLanguageB());
    }

    private RoomResult toRoomResult(Room room) {
        return RoomResult.of(room.getLoginId(), List.of(
                RoomMemberResult.of(room.getNameA(), room.getNationalityA(), room.getLanguageA(), room.getLanguageB()),
                RoomMemberResult.of(room.getNameB(), room.getNationalityB(), room.getLanguageB(), room.getLanguageA())));
    }
}
