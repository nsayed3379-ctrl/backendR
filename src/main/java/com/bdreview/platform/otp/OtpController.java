package com.bdreview.platform.otp;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/request")
    public ResponseEntity<Void> requestOtp(@Valid @RequestBody OtpRequestDto request) {
        otpService.requestOtp(request.phoneNumber());
        return ResponseEntity.accepted().build();
    }
}
