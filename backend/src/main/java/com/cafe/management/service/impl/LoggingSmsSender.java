package com.cafe.management.service.impl;

import com.cafe.management.service.SmsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Development gateway: instead of sending an SMS the code is written to the
 * application log, so the sign-in flow can be exercised without a paid account.
 *
 * <p>Never enable this in production - anyone able to read the logs could sign in.</p>
 */
@Component
@ConditionalOnProperty(name = "app.otp.sms.provider", havingValue = "log", matchIfMissing = true)
public class LoggingSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public void send(String phoneNumber, String message) {
        log.info("[sms:log] to={} message=\"{}\"", phoneNumber, message);
    }

    @Override
    public String providerName() {
        return "log";
    }
}
