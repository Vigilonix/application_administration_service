package com.vigilonix.jaanch.transformer;

import com.vigilonix.jaanch.enums.NotificationMethod;
import com.vigilonix.jaanch.enums.NotificationTemplate;
import com.vigilonix.jaanch.model.OdApplication;
import com.vigilonix.jaanch.pojo.*;
import com.vigilonix.jaanch.service.GeoHierarchyService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ApplicantApplicationCreationWhatasappTemplateTransformer implements Transformer<OdApplication, List<NotificationPayload>> {
    private final GeoHierarchyService geoHierarchyService;

    @Override
    public List<NotificationPayload> transform(OdApplication odApplication) {
        Map<String, String> params = Map.of("name", odApplication.getApplicantName(),
                "receiptNo", odApplication.getReceiptNo(),
                "odName", odApplication.getOd().getName(),
                "geoName", geoHierarchyService.getNodeById(odApplication.getFieldGeoNodeUuid()).getName(),
                "date", dateFormatterddMMYYY(odApplication.getCreatedAt()));
        StringSubstitutor sub = new StringSubstitutor(params);
        String body = sub.replace(NotificationTemplate.OD_APPLICATION_CREATED_ENGLISH.getTemplate());


        WhatsappMessageRequest sendRequest = WhatsappMessageRequest.builder()
                .to("919916488861")
//                .type("template")
                .data(WhatsappTemplate.builder()
                        .name("update_message")
                        .language(Language.builder()
                                .code("en")
                                .build())
                        .components(Arrays.asList(
                                WhatsappComponent.builder()
                                        .type("body")
                                        .parameters(Arrays.asList(
                                                WhatsappParameter.builder()
                                                        .type("text")
                                                        .text(odApplication.getApplicantName())
                                                        .build(),
                                                WhatsappParameter.builder()
                                                        .type("text")
                                                        .text(odApplication.getReceiptNo())
                                                        .build(),
                                                WhatsappParameter.builder()
                                                        .type("text")
                                                        .text(odApplication.getOd().getName())
                                                        .build(),
                                                WhatsappParameter.builder()
                                                        .type("text")
                                                        .text(geoHierarchyService.getNodeById(odApplication.getFieldGeoNodeUuid()).getName())
                                                        .build(),
                                                WhatsappParameter.builder()
                                                        .type("text")
                                                        .text(dateFormatterddMMYYY(odApplication.getCreatedAt()))
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build())
                .build();


        return Arrays.asList(NotificationPayload.builder()
                .request(sendRequest)
                .notificationMethod(NotificationMethod.WHATSAPP_TEMPLATE).build());

    }

    private String dateFormatterddMMYYY(Long epoch) {
        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), istZone);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        return dateTime.format(formatter);
    }
}
