package com.vigilonix.jaanch.transformer;


import com.vigilonix.jaanch.model.User;
import com.vigilonix.jaanch.request.UserResponse;
import com.vigilonix.jaanch.service.GeoHierarchyService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.Transformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class UserResponseTransformer implements Transformer<User, UserResponse> {
    public static final long MILLISECONDS_IN_ONE_YEAR = 365 * 24 * 3600 * 1000L;
    private final GeoHierarchyService geoHierarchyService;

    @Override
    public UserResponse transform(User principal) {

        return UserResponse.builder()
                .username(principal.getUsername())
                .email(principal.getEmail())
                .createdOn(principal.getCreatedOn())
                .uuid(principal.getUuid())
                .name(principal.getName())
                .createdOn(principal.getCreatedOn())
                .modifiedOn(principal.getModifiedOn())
                .uuid(principal.getUuid())
                .stateChangedOn(principal.getStateChangedOn())
                .role(principal.getRole())
                .rank(principal.getRank())
                .state(principal.getState())
                .lastLive(principal.getLastLive())
                .latitude(principal.getLatitude())
                .longitude(principal.getLongitude())
                .postFieldGeoNodeUuidMap(principal.getPostFieldGeoNodeUuidMap().entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(uuid -> geoHierarchyService.transformWithoutChildren(geoHierarchyService.getNodeById(uuid)))
                                .collect(Collectors.toList())
                )))
                .highestPost(geoHierarchyService.findHighestPost(principal.getPostFieldGeoNodeUuidMap()))
                .build();
    }
}
