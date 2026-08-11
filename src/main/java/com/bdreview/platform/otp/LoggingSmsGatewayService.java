package com.bdreview.platform.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Default/dev implementation — logs instead of sending. Swap in a real provider bean for prod. */
@Component
@Profile("!prod")
public class LoggingSmsGatewayService implements SmsGatewayService {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsGatewayService.class);

    @Override
    public void sendOtp(String normalizedPhoneNumber, String otpCode) {
        log.info("[DEV SMS] OTP for {}: {}", normalizedPhoneNumber, otpCode);
    }

    @Override
    public void sendMessage(String normalizedPhoneNumber, String message) {
        log.info("[DEV SMS] Message for {}: {}", normalizedPhoneNumber, message);
    }
}
