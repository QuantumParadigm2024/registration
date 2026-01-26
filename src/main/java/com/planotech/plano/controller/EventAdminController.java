package com.planotech.plano.controller;

import com.planotech.plano.auth.UserPrincipal;
import com.planotech.plano.model.Event;
import com.planotech.plano.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ea")
public class EventAdminController {

    @Autowired
    EventService eventService;

    @PreAuthorize("hasRole('EVENT_ADMIN')")
    @GetMapping("/get/allEvents")
    public ResponseEntity<?> getAllEvents(@AuthenticationPrincipal UserPrincipal userDetails) {
        return eventService.getAllEvents(userDetails.getUser());
    }




}
