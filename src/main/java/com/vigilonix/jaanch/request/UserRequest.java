package com.vigilonix.jaanch.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vigilonix.jaanch.enums.Rank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRequest {
    private String uuid;
    @NotEmpty
    private String name;
    @NotEmpty
    private String password;
    @NotEmpty
    private String email;
    @NotEmpty
    private String username;
    private Long lastLive;
    @NotEmpty
    private Rank rank;
    @NotEmpty
    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("location_range")
    private Integer locationRangeInMeters;
}
