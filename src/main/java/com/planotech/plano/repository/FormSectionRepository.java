package com.planotech.plano.repository;

import com.planotech.plano.model.FormSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormSectionRepository extends JpaRepository<FormSection, Long> {

    List<FormSection> findByFormIdOrderByDisplayOrderAsc(Long formId);

    Optional<FormSection> findByIdAndFormId(Long id, Long formId);
}

