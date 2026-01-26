package com.planotech.plano.helper;

import com.planotech.plano.model.Event;
import com.planotech.plano.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@Service
public class EventMediaAsyncService {

    @Autowired
    EventRepository eventRepository;

    @Autowired
    FileUploader fileUploader;

    private static final Logger log =
            LoggerFactory.getLogger(EventMediaAsyncService.class);

    @Async
    public void uploadLogo(Long eventId, MultipartFile file) {

        try {
            List<String> urls = fileUploader.handleFileUploadAsync(Collections.singletonList(file)).join();
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));

            event.setLogoUrl(urls.get(0));
            eventRepository.save(event);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
