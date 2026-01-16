package com.planotech.plano.repository;

import com.planotech.plano.model.EventCustomField;
import com.planotech.plano.model.EventFieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventCustomFieldsRepository extends JpaRepository<EventCustomField, Long> {

}
