package com.planotech.plano.response;

import com.planotech.plano.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {
    Long id;
    String email;
    Role role;
}
