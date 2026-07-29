package com.jairomatias.eventix.user.dto;

import com.jairomatias.eventix.role.entity.RoleName;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserCreateForm {

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

    @NotBlank(message = "La contraseña temporal es obligatoria.")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$",
            message = "{validation.password.policy}")
    private String password;

    @Size(max = 30, message = "El teléfono no puede superar 30 caracteres.")
    private String phone;

    @NotNull(message = "Selecciona un rol.")
    private RoleName roleName;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
}

