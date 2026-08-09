package com.jairomatias.eventix.ticket.wallet;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.ticket.config.TicketingProperties;
import com.jairomatias.eventix.ticket.entity.AppleWalletRegistration;
import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.entity.TicketStatus;
import com.jairomatias.eventix.ticket.repository.AppleWalletRegistrationRepository;
import com.jairomatias.eventix.ticket.security.TicketCryptographyService;

@Service
public class DefaultAppleWalletPassService
        implements AppleWalletPassService {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            DefaultAppleWalletPassService.class);
    private static final ZoneId EVENTIX_ZONE =
            ZoneId.of("America/Santo_Domingo");

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final TicketingProperties properties;
    private final TicketCryptographyService cryptographyService;
    private final AppleWalletRegistrationRepository registrationRepository;
    private final ObjectMapper objectMapper;

    public DefaultAppleWalletPassService(
            TicketingProperties properties,
            TicketCryptographyService cryptographyService,
            AppleWalletRegistrationRepository registrationRepository,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.cryptographyService = cryptographyService;
        this.registrationRepository = registrationRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isAvailable() {
        return properties.getAppleWallet().configured();
    }

    @Override
    public byte[] createPass(DigitalTicket ticket) {
        if (!isAvailable()) {
            throw new BusinessRuleException(
                    "Apple Wallet no está configurado.");
        }
        try {
            SigningMaterial signing = loadSigningMaterial();
            Map<String, byte[]> files = new LinkedHashMap<>();
            files.put("pass.json", objectMapper.writeValueAsBytes(
                    createPassJson(ticket)));
            files.put("icon.png", createBrandImage(29, 29, false));
            files.put("icon@2x.png", createBrandImage(58, 58, false));
            files.put("logo.png", createBrandImage(160, 50, true));
            files.put("logo@2x.png", createBrandImage(320, 100, true));

            byte[] manifest = objectMapper.writeValueAsBytes(
                    createManifest(files));
            files.put("manifest.json", manifest);
            files.put("signature", signManifest(manifest, signing));
            return zip(files);
        } catch (IOException | GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "No se pudo generar el pase de Apple Wallet.",
                    exception);
        }
    }

    @Override
    public void notifyUpdate(DigitalTicket ticket) {
        TicketingProperties.AppleWallet config =
                properties.getAppleWallet();
        if (!config.configured() || !config.isApnsEnabled()) {
            return;
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .connectTimeout(Duration.ofSeconds(5))
                    .sslContext(createApnsSslContext())
                    .build();
            String host = config.isApnsProduction()
                    ? "https://api.push.apple.com"
                    : "https://api.sandbox.push.apple.com";
            for (AppleWalletRegistration registration :
                    registrationRepository.findAllByTicket_Id(ticket.getId())) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(
                                host
                                + "/3/device/"
                                + registration.getPushToken()))
                        .header(
                                "apns-topic",
                                config.getPassTypeIdentifier())
                        .header("apns-priority", "10")
                        .timeout(Duration.ofSeconds(10))
                        .POST(HttpRequest.BodyPublishers.ofString("{}"))
                        .build();
                HttpResponse<String> response = client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 410) {
                    registrationRepository.delete(registration);
                } else if (response.statusCode() < 200
                        || response.statusCode() >= 300) {
                    LOGGER.warn(
                            "APNs rechazó la actualización de {} con HTTP {}.",
                            ticket.getUniqueCode(),
                            response.statusCode());
                }
            }
        } catch (GeneralSecurityException | IOException exception) {
            LOGGER.warn(
                    "No se pudo notificar Apple Wallet para {}: {}",
                    ticket.getUniqueCode(),
                    exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn(
                    "La notificación de Apple Wallet fue interrumpida para {}.",
                    ticket.getUniqueCode());
        }
    }

    private Map<String, Object> createPassJson(DigitalTicket ticket) {
        TicketingProperties.AppleWallet config =
                properties.getAppleWallet();
        Map<String, Object> pass = new LinkedHashMap<>();
        pass.put("formatVersion", 1);
        pass.put("passTypeIdentifier", config.getPassTypeIdentifier());
        pass.put("serialNumber", ticket.getUniqueCode());
        pass.put("teamIdentifier", config.getTeamIdentifier());
        pass.put("organizationName", properties.getIssuerName());
        pass.put("description", "Entrada para " + ticket.getEvent().getTitle());
        pass.put("logoText", properties.getIssuerName());
        pass.put("foregroundColor", "rgb(255, 255, 255)");
        pass.put("backgroundColor", "rgb(15, 122, 82)");
        pass.put("labelColor", "rgb(219, 255, 239)");
        pass.put("relevantDate", offsetDate(ticket.getEvent().getStartAt()));
        pass.put("expirationDate", offsetDate(ticket.getEvent().getEndAt()));
        pass.put(
                "voided",
                ticket.getStatus() == TicketStatus.CANCELLED
                        || ticket.getStatus() == TicketStatus.EXPIRED);
        pass.put("webServiceURL", stripTrailingSlash(
                config.getWebServiceUrl()));
        pass.put("authenticationToken", ticket.getAntiFraudCode());
        pass.put("barcode", barcode(ticket));
        pass.put("barcodes", List.of(barcode(ticket)));
        pass.put("eventTicket", eventTicketFields(ticket));
        return pass;
    }

    private Map<String, Object> eventTicketFields(DigitalTicket ticket) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("primaryFields", List.of(field(
                "event",
                "EVENTO",
                ticket.getEvent().getTitle())));
        fields.put("secondaryFields", List.of(
                field(
                        "date",
                        "FECHA",
                        ticket.getEvent().getStartAt().format(
                                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))),
                field(
                        "venue",
                        "LUGAR",
                        ticket.getEvent().getVenue())));
        List<Map<String, Object>> auxiliary = new ArrayList<>();
        auxiliary.add(field(
                "zone",
                "ZONA",
                ticket.getZone() == null
                        ? ticket.getTicketTypeName()
                        : ticket.getZone()));
        if (ticket.getSeat() != null) {
            auxiliary.add(field("seat", "ASIENTO", ticket.getSeat()));
        }
        fields.put("auxiliaryFields", auxiliary);
        fields.put("backFields", List.of(
                field("attendee", "ASISTENTE", ticket.getAttendeeName()),
                field(
                        "organizer",
                        "ORGANIZADOR",
                        ticket.getEvent().getOrganizer().getFullName()),
                field(
                        "address",
                        "DIRECCIÓN",
                        ticket.getEvent().getAddress()),
                field("code", "CÓDIGO", ticket.getUniqueCode()),
                field(
                        "security",
                        "SEGURIDAD",
                        "Firma Ed25519 · " + ticket.getSignatureKeyId())));
        return fields;
    }

    private Map<String, Object> barcode(DigitalTicket ticket) {
        return Map.of(
                "format", "PKBarcodeFormatQR",
                "message", cryptographyService.createQrPayload(ticket),
                "messageEncoding", "iso-8859-1",
                "altText", ticket.getUniqueCode());
    }

    private Map<String, Object> field(
            String key,
            String label,
            String value) {
        return Map.of("key", key, "label", label, "value", value);
    }

    private Map<String, String> createManifest(
            Map<String, byte[]> files) {
        Map<String, String> manifest = new LinkedHashMap<>();
        files.forEach((name, content) -> manifest.put(
                name,
                digest("SHA-1", content)));
        return manifest;
    }

    private byte[] signManifest(
            byte[] manifest,
            SigningMaterial signing) throws GeneralSecurityException {
        try {
            ContentSigner contentSigner = new JcaContentSignerBuilder(
                    "SHA256withRSA")
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(signing.privateKey());
            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
            generator.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(
                            new JcaDigestCalculatorProviderBuilder()
                                    .setProvider(
                                            BouncyCastleProvider.PROVIDER_NAME)
                                    .build())
                            .build(contentSigner, signing.certificate()));
            generator.addCertificates(new JcaCertStore(List.of(
                    signing.certificate(),
                    signing.wwdrCertificate())));
            return generator.generate(
                    new CMSProcessableByteArray(manifest),
                    false)
                    .getEncoded();
        } catch (Exception exception) {
            throw new GeneralSecurityException(
                    "No se pudo firmar el manifiesto de Apple Wallet.",
                    exception);
        }
    }

    private SigningMaterial loadSigningMaterial()
            throws GeneralSecurityException, IOException {
        TicketingProperties.AppleWallet config =
                properties.getAppleWallet();
        KeyStore keyStore = loadPkcs12();
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Key key = keyStore.getKey(
                    alias,
                    config.getCertificatePassword().toCharArray());
            if (key instanceof PrivateKey privateKey
                    && keyStore.getCertificate(alias)
                            instanceof X509Certificate certificate) {
                return new SigningMaterial(
                        privateKey,
                        certificate,
                        decodeCertificate(config.getWwdrCertificate()));
            }
        }
        throw new GeneralSecurityException(
                "El PKCS#12 no contiene una clave privada.");
    }

    private SSLContext createApnsSslContext()
            throws GeneralSecurityException, IOException {
        KeyStore keyStore = loadPkcs12();
        char[] password = properties.getAppleWallet()
                .getCertificatePassword()
                .toCharArray();
        KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagers.init(keyStore, password);
        TrustManagerFactory trustManagers = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagers.init((KeyStore) null);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(
                keyManagers.getKeyManagers(),
                trustManagers.getTrustManagers(),
                null);
        return context;
    }

    private KeyStore loadPkcs12()
            throws GeneralSecurityException, IOException {
        TicketingProperties.AppleWallet config =
                properties.getAppleWallet();
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(
                new ByteArrayInputStream(
                        decodeBase64(config.getCertificateP12())),
                config.getCertificatePassword().toCharArray());
        return keyStore;
    }

    private X509Certificate decodeCertificate(String value)
            throws GeneralSecurityException {
        String normalized = value
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
        return (X509Certificate) CertificateFactory
                .getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(
                        Base64.getDecoder().decode(normalized)));
    }

    private byte[] createBrandImage(
            int width,
            int height,
            boolean withText) throws IOException {
        BufferedImage image = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(15, 122, 82));
            graphics.fillRoundRect(0, 0, width, height, height / 4, height / 4);
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font(
                    Font.SANS_SERIF,
                    Font.BOLD,
                    withText ? Math.max(height / 3, 12) : Math.max(height / 2, 12)));
            String text = withText ? "EVENTIX" : "E";
            int textWidth = graphics.getFontMetrics().stringWidth(text);
            int baseline = (height - graphics.getFontMetrics().getHeight()) / 2
                    + graphics.getFontMetrics().getAscent();
            graphics.drawString(text, Math.max((width - textWidth) / 2, 1), baseline);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", output);
        return output.toByteArray();
    }

    private byte[] zip(Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }

    private String digest(String algorithm, byte[] value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance(algorithm).digest(value));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    algorithm + " no está disponible.",
                    exception);
        }
    }

    private byte[] decodeBase64(String value) {
        String normalized = value.trim();
        if (normalized.startsWith("base64:")) {
            normalized = normalized.substring("base64:".length());
        }
        return Base64.getDecoder().decode(normalized);
    }

    private String offsetDate(java.time.LocalDateTime value) {
        return value.atZone(EVENTIX_ZONE)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private String stripTrailingSlash(String value) {
        String normalized = value.trim();
        return normalized.endsWith("/")
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
    }

    private record SigningMaterial(
            PrivateKey privateKey,
            X509Certificate certificate,
            X509Certificate wwdrCertificate) {
    }
}
