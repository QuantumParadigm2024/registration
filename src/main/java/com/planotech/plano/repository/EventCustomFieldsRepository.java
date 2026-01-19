package com.planotech.plano.repository;

import com.planotech.plano.model.EventCustomField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventCustomFieldsRepository extends JpaRepository<EventCustomField, Long> {

}
