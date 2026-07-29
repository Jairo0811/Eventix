package com.jairomatias.eventix.user.dto;

import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.user.entity.UserStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserUpdateForm {

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 80, message = "El nombre no puede superar 80 caracteres.")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio.")
    @Size(max = 80, message = "El apellido no puede superar 80 caracteres.")
    private String lastName;

    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "Introduce un correo válido.")
    @Size(max = 160, message = "El correo no puede superar 160 caracteres.")
    private String email;

    @NotBlank(message = "El nombre de usuario es obligatorio.")
    @Pattern(
            regexp = "^[a-zA-Z0-9._-]{4,60}$",
            message = "Usa de 4 a 60 letras, números, puntos, guiones o guiones bajos.")
    private String username;

    @Size(max = 30, message = "El teléfono no puede superar 30 caracteres.")
    private String phone;

    @NotNull(message = "Selecciona un rol.")
    private RoleName roleName;

    @NotNull(message = "Selecciona un estado.")
    private UserStatus status;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public RoleName getRoleName() {
        return roleName;
    }

    public void setRoleName(RoleName roleName) {
        this.roleName = roleName;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}

