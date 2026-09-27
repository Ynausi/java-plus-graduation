package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.exception.NotFoundException;
import ru.practicum.model.User;

import java.util.List;

public interface UsersRepository extends JpaRepository<User, Long> {

    List<User> findByIdIn(List<Long> ids);

    @Query("SELECT u FROM User u ORDER BY u.id LIMIT :limit OFFSET :offset")
    List<User> findAllWithOffsetLimit(@Param("offset") int from, @Param("limit") int size);

    default User findByIdOrThrow(Long userId) {
        return findById(userId).orElseThrow(() ->
                new NotFoundException(String.format("User with id=%s was not found", userId)));
    }
}
