package com.vigilonix.applicationnadministrativeservice.transformer;

import com.vigilonix.applicationnadministrativeservice.enums.GeoHierarchyType;
import com.vigilonix.applicationnadministrativeservice.enums.NotificationMethod;
import com.vigilonix.applicationnadministrativeservice.model.OdApplication;
import com.vigilonix.applicationnadministrativeservice.model.User;
import com.vigilonix.applicationnadministrativeservice.pojo.FirestoreNotificationRequest;
import com.vigilonix.applicationnadministrativeservice.pojo.NotificationPayload;
import com.vigilonix.applicationnadministrativeservice.repository.UserRepositoryCustom;
import com.vigilonix.applicationnadministrativeservice.service.GeoHierarchyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ApplicationFirestoreStateChangeSSETransformer implements Transformer<OdApplication, NotificationPayload> {
    private final GeoHierarchyService geoHierarchyService;
    private final UserRepositoryCustom userRepositoryCustom;
    @Override
    public NotificationPayload transform(OdApplication odApplication) {
        UUID ownerGeoHierarachyNode =
                GeoHierarchyType.BEAT.equals(geoHierarchyService.getNodeById(odApplication.getGeoHierarchyNodeUuid()).getType())
                        ?                geoHierarchyService.getParentMap().get(odApplication.getGeoHierarchyNodeUuid()).getUuid()
                        : odApplication.getGeoHierarchyNodeUuid();

        List<User> ownerUsers = userRepositoryCustom.findAuthorityGeoHierarchyUser(ownerGeoHierarachyNode);
        if(CollectionUtils.isEmpty(ownerUsers)) {
            throw new IllegalArgumentException("no one is owner of this geofence"+ odApplication.getGeoHierarchyNodeUuid());
        }
        User authorityUser = ownerUsers.get(0);
        Map<String,Object> dataMap = Map.of("last_od_application_refresh_epoch", System.currentTimeMillis());
        return         NotificationPayload.builder()
                .notificationMethod(NotificationMethod.SSE_EVENT)
                .request(FirestoreNotificationRequest.builder()
                        .to(authorityUser.getUuid().toString())
                        .dataMap(dataMap)
                        .build())
                .build();
    }
}