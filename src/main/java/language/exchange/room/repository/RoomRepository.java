package language.exchange.room.repository;

import language.exchange.room.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    boolean existsByLoginId(String loginId);

    Optional<Room> findByLoginId(String loginId);
}
