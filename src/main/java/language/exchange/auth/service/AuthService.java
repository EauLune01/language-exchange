package language.exchange.auth.service;

import language.exchange.auth.dto.command.LoginCommand;
import language.exchange.global.exception.BusinessException;
import language.exchange.global.exception.ErrorCode;
import language.exchange.room.domain.Room;
import language.exchange.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final RoomRepository roomRepository;
    private final PasswordEncoder passwordEncoder;

    /** 방 아이디·비밀번호를 확인하고 roomId를 돌려줍니다. 아이디 없음과 비밀번호 틀림은 구분하지 않습니다. */
    public Long login(LoginCommand command) {
        return roomRepository.findByLoginId(command.getLoginId())
                .filter(room -> passwordEncoder.matches(command.getPassword(), room.getPasswordHash()))
                .map(Room::getId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
    }
}
