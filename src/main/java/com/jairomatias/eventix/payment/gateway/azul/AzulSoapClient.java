package com.jairomatias.eventix.payment.gateway.azul;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.gateway.PaymentCommand;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;

@Component
public class AzulSoapClient {

    private static final String NAMESPACE =
            "http://Merit/AzulIS/TransactionServices/";

    private final AzulWalletProperties properties;
    private final HttpClient httpClient;

    public AzulSoapClient(AzulWalletProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public AzulPaymentResponse process(PaymentCommand command) {
        String body = processPaymentEnvelope(command);
        Document document = send(
                "ProcessPayment",
                body);
        return new AzulPaymentResponse(
                text(document, "IsoCode"),
                text(document, "ResponseCode"),
                text(document, "ResponseMessage"),
                text(document, "ErrorDescription"),
                text(document, "AzulOrderId"));
    }

    public String createApplePaySession() {
        if (!StringUtils.hasText(properties.getInitiativeContext())) {
            throw new BusinessRuleException(
                    "Apple Pay requiere configurar el dominio comercial de Eventix.");
        }
        String request = """
                <ApplePayPaymentSession xmlns=\"%s\">
                  <ApplePayPaymentSessionRequest>
                    <Channel>%s</Channel>
                    <Store>%s</Store>
                    <StoreDisplayName>%s</StoreDisplayName>
                    <InitiativeContext>%s</InitiativeContext>
                  </ApplePayPaymentSessionRequest>
                </ApplePayPaymentSession>
                """.formatted(
                NAMESPACE,
                xml(properties.getChannel()),
                xml(properties.getStore()),
                xml(properties.getMerchantDisplayName()),
                xml(properties.getInitiativeContext()));
        Document document = send("ApplePayPaymentSession", request);
        String responseCode = text(document, "ResponseCode");
        String session = text(document, "ApplePayPaymentSessionDataJSON");
        if (!StringUtils.hasText(session)) {
            String error = text(document, "ErrorDescription");
            throw new BusinessRuleException(
                    StringUtils.hasText(error)
                            ? "Azul no pudo crear la sesión de Apple Pay: " + error
                            : "Azul no pudo crear la sesión de Apple Pay ("
                                    + responseCode + ").");
        }
        return session;
    }

    private Document send(String operation, String body) {
        ensureConfigured();
        String envelope = """
                <?xml version=\"1.0\" encoding=\"utf-8\"?>
                <soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">
                  <soap:Header>
                    <SOAPAuthHeader xmlns=\"%s\">
                      <Auth1>%s</Auth1>
                      <Auth2>%s</Auth2>
                    </SOAPAuthHeader>
                  </soap:Header>
                  <soap:Body>%s</soap:Body>
                </soap:Envelope>
                """.formatted(
                NAMESPACE,
                xml(properties.getAuth1()),
                xml(properties.getAuth2()),
                body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.soapEndpoint()))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", NAMESPACE + operation)
                .POST(HttpRequest.BodyPublishers.ofString(envelope))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessRuleException(
                        "Azul no está disponible en este momento.");
            }
            return parse(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessRuleException(
                    "La operación con Azul fue interrumpida.");
        } catch (BusinessRuleException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessRuleException(
                    "No fue posible comunicarse de forma segura con Azul.");
        }
    }

    private String processPaymentEnvelope(PaymentCommand command) {
        String originalReference = command.originalExternalReference();
        String wallet = walletElement(command);
        return """
                <ProcessPayment xmlns=\"%s\">
                  <ProcessPaymentRequest>
                    <Channel>%s</Channel>
                    <Store>%s</Store>
                    <PosInputMode>E-Commerce</PosInputMode>
                    <TrxType>%s</TrxType>
                    <Amount>%s</Amount>
                    <Itbis>0</Itbis>
                    <AzulOrderId>%s</AzulOrderId>
                    <OrderNumber>%s</OrderNumber>
                    <CustomOrderId>%s</CustomOrderId>
                    %s
                  </ProcessPaymentRequest>
                </ProcessPayment>
                """.formatted(
                NAMESPACE,
                xml(properties.getChannel()),
                xml(properties.getStore()),
                command.transactionType().name().equals("REFUND")
                        ? "Refund"
                        : "Sale",
                minorUnits(command.amount()),
                xml(originalReference),
                xml(command.saleReference()),
                xml(command.saleReference()),
                wallet);
    }

    private String walletElement(PaymentCommand command) {
        if (command.transactionType().name().equals("REFUND")) {
            return "";
        }
        if (!StringUtils.hasText(command.walletToken())) {
            throw new BusinessRuleException(
                    "La billetera digital no devolvió un token de pago válido.");
        }
        String token = xml(command.walletToken());
        if (command.provider() == PaymentProvider.GOOGLE_PAY) {
            return "<GooglePay><PaymentToken>" + token
                    + "</PaymentToken></GooglePay>";
        }
        if (command.provider() == PaymentProvider.APPLE_PAY) {
            return "<ApplePay><PaymentToken>" + token
                    + "</PaymentToken></ApplePay>";
        }
        throw new BusinessRuleException(
                "El proveedor no corresponde a una billetera digital.");
    }

    private static String minorUnits(java.math.BigDecimal amount) {
        return amount.movePointRight(2)
                .setScale(0, java.math.RoundingMode.UNNECESSARY)
                .toPlainString();
    }

    private void ensureConfigured() {
        if (!properties.isReady()) {
            throw new BusinessRuleException(
                    "Apple Pay y Google Pay todavía no tienen credenciales de Azul configuradas.");
        }
    }

    private static Document parse(String value) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true);
        factory.setFeature(
                "http://xml.org/sax/features/external-general-entities",
                false);
        factory.setFeature(
                "http://xml.org/sax/features/external-parameter-entities",
                false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(
                new InputSource(new StringReader(value)));
    }

    private static String text(Document document, String tagName) {
        var nodes = document.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent();
    }

    private static String xml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    public record AzulPaymentResponse(
            String isoCode,
            String responseCode,
            String responseMessage,
            String errorDescription,
            String azulOrderId) {
    }
}
