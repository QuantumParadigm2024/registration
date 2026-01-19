package com.planotech.plano.service;

import com.planotech.plano.exception.CustomBadRequestException;
import com.planotech.plano.helper.EventMediaAsyncService;
import com.planotech.plano.helper.FileUploader;
import com.planotech.plano.model.Event;
import com.planotech.plano.model.EventCustomField;
import com.planotech.plano.model.EventMedia;
import com.planotech.plano.model.User;
import com.planotech.plano.repository.EventRepository;
import com.planotech.plano.request.EventCreateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class EventService {

    @Autowired
    EventRepository eventRepository;

    @Autowired
    FileUploader fileUploader;

    @Autowired
    EventMediaAsyncService eventMediaAsyncService;

    public ResponseEntity<?> createEvent(EventCreateRequest eventRequest, MultiValueMap<String, MultipartFile> files, User user) {

        if (eventRepository.findByName(eventRequest.getName()).isPresent()) {
            throw new CustomBadRequestException("Event already exists with the same name");
        }
        Event event = new Event();
        event.setName(eventRequest.getName());
        event.setCreatedBy(user);
        event.setDescription(eventRequest.getDescription());
        event.setLocation(eventRequest.getLocation());
        event.setEndDate(eventRequest.getEndDate());
        event.setStartDate(eventRequest.getStartDate());

        if(eventRequest.getDynamicFields()!=null && !eventRequest.getDynamicFields().isEmpty()){
            eventRequest.getDynamicFields().forEach((key, value)->{
                EventCustomField field = new EventCustomField();
                field.setEvent(event);
                field.setFieldKey(key);
                field.setFieldValues(value);
                event.getCustomFields().add(field);
            });
        }
//        eventRepository.save(event);
        if (files != null && !files.isEmpty()){
            eventMediaAsyncService.uploadMediaAsync(event, files);
        }
        return ResponseEntity.ok(Map.of(
                "message", "Event created successfully",
                "status", "success",
                "code", HttpStatus.OK.value()
        ));
    }
}
