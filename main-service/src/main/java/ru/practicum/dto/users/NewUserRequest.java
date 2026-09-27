package ru.practicum.dto.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewUserRequest {
    @NotNull
    @Size(min = 6, max = 254)
    @Email
    private String email;

    @NotNull
    @NotBlank
    @Size(min = 2, max = 250)
    private String name;
}
