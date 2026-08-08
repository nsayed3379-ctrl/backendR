package com.bdreview.platform.admin.controller;

import com.bdreview.platform.admin.support.AdminSupport;
import com.bdreview.platform.auth.UserRepository;
import com.bdreview.platform.business.BusinessRepository;
import com.bdreview.platform.verification.NidVerification;
import com.bdreview.platform.verification.NidVerificationService;
import com.bdreview.platform.verification.ResolveNidRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/admin/nid-verifications")
public class AdminNidVerificationController {

    private final NidVerificationService nidVerificationService;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;

    public AdminNidVerificationController(NidVerificationService nidVerificationService,
                                           BusinessRepository businessRepository,
                                           UserRepository userRepository) {
        this.nidVerificationService = nidVerificationService;
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Integer page, Model model) {
        var results = nidVerificationService.queue(PageRequest.of(AdminSupport.pageOrDefault(page), 20));

        Map<UUID, String> businessNames = new HashMap<>();
        Map<UUID, String> ownerPhones = new HashMap<>();
        for (NidVerification nid : results.getContent()) {
            businessRepository.findById(nid.getBusinessId()).ifPresent(b -> businessNames.put(nid.getId(), b.getName()));
            userRepository.findById(nid.getOwnerUserId()).ifPresent(u -> ownerPhones.put(nid.getId(), u.getPhoneNumber()));
        }

        model.addAttribute("results", results);
        model.addAttribute("businessNames", businessNames);
        model.addAttribute("ownerPhones", ownerPhones);
        model.addAttribute("active", "nid");
        return "admin/nid/list";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable UUID id, @RequestParam(required = false) String notes,
                           RedirectAttributes redirectAttributes) {
        return resolve(id, true, notes, redirectAttributes);
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable UUID id, @RequestParam(required = false) String notes,
                          RedirectAttributes redirectAttributes) {
        return resolve(id, false, notes, redirectAttributes);
    }

    private String resolve(UUID id, boolean approve, String notes, RedirectAttributes redirectAttributes) {
        try {
            nidVerificationService.resolve(id, new ResolveNidRequest(approve, notes));
            redirectAttributes.addFlashAttribute("successMessage", approve ? "NID verification approved." : "NID verification rejected.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/nid-verifications";
    }
}
