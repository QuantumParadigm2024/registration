package com.planotech.plano.repository;

import com.planotech.plano.enums.Role;
import com.planotech.plano.model.Event;
import com.planotech.plano.model.EventUser;
import com.planotech.plano.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface EventUserRepository extends JpaRepository<EventUser, Long> {
    boolean existsByUserAndEventAndRole(User user, Event event, Role role);
    @Query("""
        SELECT eu.event
        FROM EventUser eu
        WHERE eu.user.id = :userId
          AND eu.role = :role
          AND eu.active = true
    """)
    List<Event> findActiveEventsByUserAndRole(
            @Param("userId") Long userId,
            @Param("role") Role role
    );

    List<EventUser> findByUserInAndActiveTrue(List<User> users);
}
