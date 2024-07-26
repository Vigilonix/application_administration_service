package com.vigilonix.jaanch.controller;

import com.vigilonix.jaanch.request.AuthRequest;
import com.vigilonix.jaanch.request.LoginResponse;
import com.vigilonix.jaanch.request.OAuth2Response;
import com.vigilonix.jaanch.request.RefreshTokenRequest;
import com.vigilonix.jaanch.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AuthController.OAUTH)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AuthController {
    public static final String TOKEN = "/token";
    public static final String REFRESH_TOKEN = "/refresh_token";
    public static final String OAUTH = "/oauth";
    private final UserService userService;

    @PostMapping(path = TOKEN, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OAuth2Response> login(@Valid @RequestBody AuthRequest athRequest) {
        LoginResponse response = userService.login(athRequest);
        return new ResponseEntity<>(response.getOAuth2Response(), HttpStatus.valueOf(response.getStatusCode()));
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping(path = REFRESH_TOKEN, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public OAuth2Response refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        return userService.refreshToken(refreshTokenRequest);
    }
}
