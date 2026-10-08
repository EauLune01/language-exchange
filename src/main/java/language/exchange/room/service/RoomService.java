package language.exchange.room.service;

import language.exchange.global.exception.BusinessException;
import language.exchange.global.exception.ErrorCode;
import language.exchange.room.domain.Language;
import language.exchange.room.domain.Room;
import language.exchange.room.dto.command.RoomCreateCommand;
import language.exchange.room.dto.command.RoomMemberCommand;
import language.exchange.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomService {

    // ISO 3166-1 alpha-2 국가 코드 (KR, JP …)
    private static final Set<String> COUNTRY_CODES = Set.of(Locale.getISOCountries());

    private final RoomRepository roomRepository;
    private final PasswordEncoder passwordEncoder;

    public Long createRoom(RoomCreateCommand command) {
        RoomMemberCommand memberA = command.getMemberA();
        RoomMemberCommand memberB = command.getMemberB();

        if (!COUNTRY_CODES.contains(memberA.getNationality()) || !COUNTRY_CODES.contains(memberB.getNationality())) {
            throw new BusinessException(ErrorCode.INVALID_NATIONALITY);
        }
        // 서로 상대의 언어를 배우는 교환: A가 쓰는 언어 = B가 배우고 싶은 언어, B가 쓰는 언어 = A가 배우고 싶은 언어
        Language languageA = memberB.getLearningLanguage();
        Language languageB = memberA.getLearningLanguage();
        if (languageA == languageB) {
            throw new BusinessException(ErrorCode.SAME_LANGUAGE);
        }
        // 동시에 같은 아이디로 만들면 rooms.login_id 유니크 제약이 마지막으로 막는다 (그 경우 500)
        if (roomRepository.existsByLoginId(command.getLoginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_ROOM_ID);
        }

        Room room = roomRepository.save(Room.create(
                command.getLoginId(),
                passwordEncoder.encode(command.getPassword()),
                languageA,
                languageB,
                memberA.getName(),
                memberA.getNationality(),
                memberB.getName(),
                memberB.getNationality()));
        return room.getId();
    }
}
