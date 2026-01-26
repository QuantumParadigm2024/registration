package com.planotech.plano.repository;

import com.planotech.plano.enums.AccountStatus;
import com.planotech.plano.enums.Role;
import com.planotech.plano.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
     Optional<User> findByEmail(String email);
     boolean existsByRole(Role role);
     List<User> findByRoleAndCompanyIsNullAndStatus(Role role, AccountStatus status);

}
