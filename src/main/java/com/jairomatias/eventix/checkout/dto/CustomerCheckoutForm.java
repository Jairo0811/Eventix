package com.jairomatias.eventix.checkout.dto;

import com.jairomatias.eventix.payment.entity.PaymentProvider;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CustomerCheckoutForm {

    @NotNull(message = "Selecciona un tipo de entrada.")
    private Long ticketTypeId;

    @Min(value = 1, message = "Debes comprar al menos una entrada.")
    @Max(value = 10, message = "Puedes comprar hasta 10 entradas por operación.")
    private int quantity = 1;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 80)
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio.")
    @Size(max = 80)
    private String lastName;

    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "Introduce un correo válido.")
    @Size(max = 160)
    private String email;

    @NotBlank(message = "El teléfono es obligatorio.")
    @Size(max = 30)
    private String phone;

    @Size(max = 40)
    private String couponCode;

    @NotNull(message = "Selecciona el método de pago.")
    private PaymentProvider provider = PaymentProvider.CARDNET;

    @Size(max = 20000, message = "El token de la billetera digital no es válido.")
    private String walletToken;

    public Long getTicketTypeId() { return ticketTypeId; }
    public void setTicketTypeId(Long ticketTypeId) { this.ticketTypeId = ticketTypeId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public PaymentProvider getProvider() { return provider; }
    public void setProvider(PaymentProvider provider) { this.provider = provider; }
    public String getWalletToken() { return walletToken; }
    public void setWalletToken(String walletToken) { this.walletToken = walletToken; }
}
