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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/ea")
@PreAuthorize("hasRole('EVENT_ADMIN')")
public class EventAdminController {

    @Autowired
    EventService eventService;

    @PostMapping(value ="/create/event",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createEvent(
                                         @RequestPart(value = "files", required = false) Map<String, MultipartFile[]> files,
                                         @AuthenticationPrincipal UserPrincipal userDetails){
//        System.out.println("eventRequest="+eventRequest);
        System.out.println(files.isEmpty());
//        return eventService.createEvent(eventRequest, files, userDetails.getUser());
        return null;
    }



}
