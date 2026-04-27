package com.planotech.plano.repository;

import com.planotech.plano.model.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

    Optional<EmailOtp> findTopByEmailAndFormKeyOrderByIdDesc(
            String email,
            String formKey
    );

    void deleteByEmailAndFormKey(String email, String formKey);
}
