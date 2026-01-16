package com.planotech.plano.repository;

import com.planotech.plano.enums.Role;
import com.planotech.plano.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
     User findByEmail(String email);
     boolean existsByRole(Role role);
}
