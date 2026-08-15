package com.bdreview.platform.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Default/dev implementation — logs instead of sending. Swap in a real provider bean for prod. */
@Component
@Profile("!prod")
public class LoggingEmailSenderService implements EmailSenderService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSenderService.class);

    @Override
    public void sendVerificationCode(String email, String code) {
        log.info("[DEV EMAIL] Verification code for {}: {}", email, code);
    }
}
