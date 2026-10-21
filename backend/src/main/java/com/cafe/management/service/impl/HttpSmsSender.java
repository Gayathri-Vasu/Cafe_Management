package com.cafe.management.service.impl;

import com.cafe.management.exception.BadRequestException;
import com.cafe.management.service.SmsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic HTTP gateway: POSTs {@code {"to": ..., "message": ...}} as JSON to the
 * configured webhook URL.
 *
 * <p>Point {@code app.otp.sms.http.url} at a Twilio Function, an MSG91 / Fast2SMS
 * relay, or any small service of your own that knows how to talk to the provider.
 * Keep the gateway credentials inside that service, not in this application.</p>
 */
@Component
@ConditionalOnProperty(name = "app.otp.sms.provider", havingValue = "http")
public class HttpSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(HttpSmsSender.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    private final String url;
    private final String bearerToken;
    private final String senderId;

    public HttpSmsSender(@Value("${app.otp.sms.http.url:}") String url,
                         @Value("${app.otp.sms.http.token:}") String bearerToken,
                         @Value("${app.otp.sms.http.sender-id:}") String senderId) {
        this.url = url;
        this.bearerToken = bearerToken;
        this.senderId = senderId;
        if (url == null || url.isBlank()) {
            log.warn("OTP SMS provider is 'http' but app.otp.sms.http.url is empty - codes will fail to send.");
        }
    }

    @Override
    public void send(String phoneNumber, String message) {
        if (url == null || url.isBlank()) {
            throw new BadRequestException("The SMS gateway is not configured");
        }

        Map<String, String> body = new LinkedHashMap<>();
        body.put("to", phoneNumber);
        body.put("message", message);
        if (senderId != null && !senderId.isBlank()) {
            body.put("senderId", senderId);
        }

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonMapper.writeValueAsString(body)));
            if (bearerToken != null && !bearerToken.isBlank()) {
                builder.header("Authorization", "Bearer " + bearerToken);
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("SMS gateway responded with HTTP {}", response.statusCode());
                throw new BadRequestException("We could not send a code to that number");
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("We could not send a code to that number");
        } catch (Exception ex) {
            log.warn("SMS gateway call failed: {}", ex.getMessage());
            throw new BadRequestException("We could not send a code to that number");
        }
    }

    @Override
    public String providerName() {
        return "http";
    }
}
