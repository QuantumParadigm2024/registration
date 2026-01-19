package com.planotech.plano.controller;

import com.planotech.plano.auth.MyUserDetailService;
import com.planotech.plano.auth.UserPrincipal;
import com.planotech.plano.request.EventCreateRequest;
import com.planotech.plano.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@RestController
@RequestMapping("/ea")
@PreAuthorize("hasRole('EVENT_ADMIN')")
public class EventAdminController {

    @Autowired
    EventService eventService;

    @PostMapping(
            value = "/create/event",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> createEvent(
            @RequestPart("event") String eventRequest,
            @RequestParam MultiValueMap<String, MultipartFile> files,
            @AuthenticationPrincipal UserPrincipal userDetails) {
        ObjectMapper mapper = new ObjectMapper();
        EventCreateRequest event =
                mapper.readValue(eventRequest, EventCreateRequest.class);
        System.out.println("controller\n" + event);
        System.out.println( files.isEmpty()+"\n"+files.size());
        files.forEach((mediaType, file) -> {
            System.out.println(mediaType + " -> " + file.size());
        });
        return eventService.createEvent(event, files, userDetails.getUser());
//        return null;
    }


}
