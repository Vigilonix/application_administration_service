package com.vigilonix.jaanch.controller;

import com.dt.beyond.aop.LogPayload;
import com.dt.beyond.config.AuthHelper;
import com.dt.beyond.pojo.*;
import com.dt.beyond.request.*;
import com.dt.beyond.service.UserService;
import com.vigilonix.jaanch.aop.LogPayload;
import com.vigilonix.jaanch.helper.AuthHelper;
import com.vigilonix.jaanch.request.OAuth2Response;
import com.vigilonix.jaanch.request.UserRequest;
import com.vigilonix.jaanch.request.UserResponse;
import com.vigilonix.jaanch.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(UserController.V_1_USER)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserController {
    public static final String V_1_USER = "/v1/user";
    private final UserService userService;
    private final AuthHelper authHelper;

    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public OAuth2Response signup(@RequestBody UserRequest userRequest) {
        return userService.signUpViaUserPass(userRequest, authHelper.getPrincipal());
    }

    @LogPayload
    @PutMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public UserResponse updateUser(@Valid @RequestBody UserRequest userRequest) {
        return userService.updateUser(authHelper.getPrincipal(), userRequest);
    }

    @PostMapping(path = "/logout", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void logout() {
        userService.logout(authHelper.getPrincipal());
    }

    @DeleteMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public FbGdprResponse deleteAccount() {
        return userService.deleteAccount(authHelper.getPrincipal());
    }

    @PostMapping(path = "/state", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public FbGdprResponse changeState(@Valid @RequestBody UserStateChaneRequest userStateChaneRequest) {
        return userService.changeState(userStateChaneRequest, authHelper.getPrincipal());
    }
    @GetMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public UserResponse getDetails() {
        return userService.getDetails(authHelper.getPrincipal());
    }

}
