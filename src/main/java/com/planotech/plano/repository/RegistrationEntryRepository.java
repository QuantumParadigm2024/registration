package com.planotech.plano.repository;

import com.planotech.plano.model.RegistrationEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistrationEntryRepository extends JpaRepository<RegistrationEntry, Long> {
    boolean existsByEventIdAndEmail(Long eventId, String email);
}
