package com.devlil0.whey_promotion_bot.controller;

import com.devlil0.whey_promotion_bot.service.MlTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
@RequestMapping("/api/ml/oauth")
public class MlOAuthController {

    private final MlTokenService tokenService;

    public MlOAuthController(MlTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping("/start")
    public RedirectView start() {
        return new RedirectView(tokenService.buildAuthorizationUrl());
    }

    @GetMapping("/callback")
    public Map<String, String> callback(@RequestParam(required = false) String code,
                                        @RequestParam(required = false) String error) {
        if (error != null || code == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ML OAuth erro: " + error);
        }
        tokenService.handleAuthorizationCode(code);
        return Map.of("status", "ok", "message", "ML autorizado com sucesso. O bot já pode buscar produtos.");
    }
}
