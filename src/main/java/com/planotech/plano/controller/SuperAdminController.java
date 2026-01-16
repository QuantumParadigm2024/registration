package com.planotech.plano.controller;


import com.planotech.plano.model.User;
import com.planotech.plano.service.SuperAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sa")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    @Autowired
    SuperAdminService superAdminService;

    @PostMapping("/create/superadmin")
    public ResponseEntity<?> createSuperAdmin(@RequestBody User user){
        return superAdminService.createSuperAdmin(user);
    }

    @PostMapping("/create/event/admin")
    public ResponseEntity<?> createEventAdmin(@RequestBody User user){
        return superAdminService.createEventAdmin(user);
    }


}
