package com.jairomatias.eventix.ticket.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.ticket.config.TicketingProperties;
import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.entity.TicketStatus;
import com.jairomatias.eventix.ticket.repository.AppleWalletRegistrationRepository;
import com.jairomatias.eventix.ticket.security.TicketCryptographyService;
import com.jairomatias.eventix.user.entity.User;

class AppleWalletPassSigningTest {

    @Test
    void createsVerifiableArchiveWithSeatAndSignedQr() throws Exception {
        KeyPair keys = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        X500Name subject = new X500Name("CN=Eventix test only");
        X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(
                new JcaX509v3CertificateBuilder(subject, BigInteger.ONE,
                        new Date(0), new Date(4102444800000L), subject, keys.getPublic())
                        .build(new JcaContentSignerBuilder("SHA256withRSA").build(keys.getPrivate())));
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, new char[0]);
        store.setKeyEntry("test", keys.getPrivate(), new char[0], new Certificate[] {certificate});
        ByteArrayOutputStream p12 = new ByteArrayOutputStream();
        store.store(p12, new char[0]);
        TicketingProperties properties = new TicketingProperties();
        var apple = properties.getAppleWallet();
        apple.setEnabled(true);
        apple.setPassTypeIdentifier("pass.example");
        apple.setTeamIdentifier("TEAM");
        apple.setWebServiceUrl("https://example.com/api/wallet/apple/v1");
        apple.setCertificateP12(Base64.getEncoder().encodeToString(p12.toByteArray()));
        apple.setWwdrCertificate(Base64.getEncoder().encodeToString(certificate.getEncoded()));
        DigitalTicket ticket = mock(DigitalTicket.class);
        Event event = mock(Event.class);
        User organizer = mock(User.class);
        when(ticket.getEvent()).thenReturn(event);
        when(event.getOrganizer()).thenReturn(organizer);
        when(organizer.getFullName()).thenReturn("Organizador");
        when(event.getTitle()).thenReturn("Concierto");
        when(event.getVenue()).thenReturn("Teatro");
        when(event.getAddress()).thenReturn("Santo Domingo");
        when(event.getStartAt()).thenReturn(LocalDateTime.of(2026, 8, 8, 19, 0));
        when(event.getEndAt()).thenReturn(LocalDateTime.of(2026, 8, 8, 23, 0));
        when(ticket.getUniqueCode()).thenReturn("TKT-1");
        when(ticket.getAntiFraudCode()).thenReturn("0123456789abcdef");
        when(ticket.getAttendeeName()).thenReturn("María");
        when(ticket.getZone()).thenReturn("VIP");
        when(ticket.getSeat()).thenReturn("A-12");
        when(ticket.getStatus()).thenReturn(TicketStatus.CANCELLED);
        TicketCryptographyService crypto = mock(TicketCryptographyService.class);
        when(crypto.createQrPayload(ticket)).thenReturn("EVX1.signed");
        ObjectMapper mapper = new ObjectMapper();
        var service = new DefaultAppleWalletPassService(properties, crypto,
                mock(AppleWalletRegistrationRepository.class), mapper);
        Map<String, byte[]> files = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(service.createPass(ticket)))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                files.put(entry.getName(), zip.readAllBytes());
            }
        }
        JsonNode pass = mapper.readTree(files.get("pass.json"));
        assertThat(pass.path("webServiceURL").asText()).isEqualTo("https://example.com/api/wallet/apple");
        assertThat(pass.path("voided").asBoolean()).isTrue();
        assertThat(pass.path("barcodes").get(0).path("message").asText()).isEqualTo("EVX1.signed");
        assertThat(pass.path("eventTicket").path("auxiliaryFields").get(1).path("value").asText()).isEqualTo("A-12");
        JsonNode manifest = mapper.readTree(files.get("manifest.json"));
        for (var entry : files.entrySet()) {
            if (!entry.getKey().equals("manifest.json") && !entry.getKey().equals("signature")) {
                assertThat(manifest.path(entry.getKey()).asText()).isEqualTo(HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-1").digest(entry.getValue())));
            }
        }
        CMSSignedData signed = new CMSSignedData(new CMSProcessableByteArray(files.get("manifest.json")),
                files.get("signature"));
        assertThat(signed.getSignerInfos().getSigners()).hasSize(1);
        assertThat(signed.getSignerInfos().getSigners().iterator().next()
                .verify(new JcaSimpleSignerInfoVerifierBuilder().build(certificate))).isTrue();
    }
}
