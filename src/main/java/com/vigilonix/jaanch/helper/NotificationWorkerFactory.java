package com.vigilonix.jaanch.helper;

import com.vigilonix.jaanch.enums.NotificationMethod;
import com.vigilonix.jaanch.pojo.NotificationPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class NotificationWorkerFactory {
    private final Map<NotificationMethod, NavigableSet<INotificationWorker>> notificationWorkerMap;

    public boolean notify(NotificationPayload notificationPayload) {
        log.debug("payload received to notify {}", notificationPayload);
        for (INotificationWorker notificationWorker :notificationWorkerMap.getOrDefault(notificationPayload.getNotificationMethod(), new TreeSet<>())) {
            try {
                log.debug("notification worker {} for payload {}", notificationWorker, notificationPayload);
                if(notificationWorker.work(notificationPayload)) {
                    return true;
                }
            }catch (RuntimeException e) {
                log.error("failed to notify for payload {}", notificationPayload, e);
            }
        }
        return false;
    }
}
