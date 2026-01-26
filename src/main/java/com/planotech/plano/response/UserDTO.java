package com.planotech.plano.response;

import com.planotech.plano.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private List<AssignedEventDTO> assignedEvents;
}