package com.vigilonix.samadhan.service;

import com.vigilonix.samadhan.aop.LogPayload;
import com.vigilonix.samadhan.aop.Timed;
import com.vigilonix.samadhan.enums.Post;
import com.vigilonix.samadhan.model.OdApplication;
import com.vigilonix.samadhan.model.OdApplicationAssignment;
import com.vigilonix.samadhan.model.User;
import com.vigilonix.samadhan.pojo.*;
import com.vigilonix.samadhan.repository.OdApplicationAssignmentRepository;
import com.vigilonix.samadhan.repository.OdApplicationRepository;
import com.vigilonix.samadhan.repository.UserRepository;
import com.vigilonix.samadhan.transformer.OdApplicationAssignmentTransformer;
import com.vigilonix.samadhan.transformer.OdApplicationTransformer;
import com.vigilonix.samadhan.validator.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@Transactional
public class OdApplicationService {
    private final OdApplicationRepository odApplicationRepository;
    private final OdApplicationTransformer odApplicationTransformer;
    private final UserRepository userRepository;
    private final GeoHierarchyService geoHierarchyService;
    private final ValidationService<ODApplicationValidationPayload> odUpdateValidationService;
    private final ValidationService<ODApplicationValidationPayload> odCreateValidationService;
    private final NotificationService notificationService;
    private final OdApplicationAssignmentRepository odApplicationAssignmentRepository;
    private final OdApplicationAssignmentTransformer odApplicationAssignmentTransformer;

    @Autowired
    public OdApplicationService(
            OdApplicationRepository odApplicationRepository,
            OdApplicationTransformer odApplicationTransformer,
            UserRepository userRepository,
            GeoHierarchyService geoHierarchyService,
            @Qualifier("update") ValidationService<ODApplicationValidationPayload> odUpdateValidationService,
            @Qualifier("create") ValidationService<ODApplicationValidationPayload> odCreateValidationService, NotificationService notificationService, OdApplicationAssignmentRepository odApplicationAssignmentRepository, OdApplicationAssignmentTransformer odApplicationAssignmentTransformer) {
        this.odApplicationRepository = odApplicationRepository;
        this.odApplicationTransformer = odApplicationTransformer;
        this.userRepository = userRepository;
        this.geoHierarchyService = geoHierarchyService;
        this.odUpdateValidationService = odUpdateValidationService;
        this.odCreateValidationService = odCreateValidationService;
        this.notificationService = notificationService;
        this.odApplicationAssignmentRepository = odApplicationAssignmentRepository;
        this.odApplicationAssignmentTransformer = odApplicationAssignmentTransformer;
    }

    @Timed
    @LogPayload
    public OdApplicationPayload create(OdApplicationPayload odApplicationPayload, User principal, List<UUID> geoHierarchyNodeUuids) {
        odCreateValidationService.validate(ODApplicationValidationPayload.builder()
                .odApplicationPayload(odApplicationPayload).principalUser(principal)
                .geoHierarchyNodeUuids(geoHierarchyNodeUuids)
                .build());

        Long epoch = System.currentTimeMillis();
        GeoHierarchyNode geoHierarchyNode = resolveGeoHierarchyNode(principal.getPostGeoHierarchyNodeUuidMap(), geoHierarchyNodeUuids);
        Optional<Integer> maxBucketNo = odApplicationRepository.findMaxReceiptBucketNumberForCurrentMonth(geoHierarchyNode.getUuid());
        int bucketNo = maxBucketNo.map(integer -> integer + 1).orElse(1);

        OdApplication odApplication = OdApplication.builder()
                .uuid(UUID.randomUUID())
                .od(principal)
                .applicantName(odApplicationPayload.getApplicantName())
                .applicationFilePath(odApplicationPayload.getApplicationFilePath())
                .applicantPhoneNumber(odApplicationPayload.getApplicantPhoneNumber())
                .receiptNo(generateReceiptNumber(geoHierarchyNode, bucketNo))
                .receiptBucketNumber(bucketNo)
                .geoHierarchyNodeUuid(geoHierarchyNode.getUuid())
                .createdAt(epoch)
                .modifiedAt(epoch)
                .status(OdApplicationStatus.OPEN)
                .category(odApplicationPayload.getCategory())
                .build();
        odApplicationRepository.save(odApplication);
        notificationService.sendNotification(odApplication);
        return odApplicationTransformer.transform(ODApplicationTransformationRequest.builder().odApplication(odApplication).principalUser(principal).build());
    }

    private GeoHierarchyNode resolveGeoHierarchyNode(Map<Post, List<UUID>> postGeoHierarchyNodeUuidMap, List<UUID> geoHierarchyNodeUuids) {
        return CollectionUtils.isEmpty(geoHierarchyNodeUuids)
                ? geoHierarchyService.getHighestPostNode(postGeoHierarchyNodeUuidMap)
                : geoHierarchyService.getNodeById(geoHierarchyNodeUuids.get(0));
    }

    private String generateReceiptNumber(GeoHierarchyNode geoHierarchyNode, int bucketNo) {
        String jurisdictionName = geoHierarchyNode.getName()
                .replace(" ", "_");
        // Get the current date
        LocalDate currentDate = LocalDate.now();
        // Define the date formatter with the desired pattern
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy");
        // Format the current date
        String formattedDate = currentDate.format(formatter);
        return String.format("%s_%s_%s", jurisdictionName, formattedDate, bucketNo);
    }

    @LogPayload
    @Timed
    public OdApplicationPayload update(UUID uuid, OdApplicationPayload odApplicationPayload, User principal) {
        OdApplication odApplication = odApplicationRepository.findByUuid(uuid);
//        odUpdateValidationService.validate(ODApplicationValidationPayload.builder()
//                .odApplicationPayload(odApplicationPayload)
//                .odApplication(odApplication)
//                .enquiryUser(userRepository.findByUuid(odApplicationPayload.getEnquiryOfficerUuid()))
//                .principalUser(principal).build());

//        if (CollectionUtils.isNotEmpty(odApplicationPayload.getEnquiries())) {
//            List<Enquiry> enquiries = odApplicationPayload.getEnquiries().stream().map(e-> Enquiry.builder().build()).collect(Collectors.toList());
//            enquiries.addAll(odApplication.getEnquiries());
//            odApplication.setEnquiries(enquiries);
//            odApplication.setStatus(OdApplicationStatus.REVIEW);
//        }
//        if (OdApplicationStatus.REVIEW.equals(odApplication.getStatus()) && OdApplicationStatus.ENQUIRY.equals(odApplicationPayload.getStatus())) {
//            odApplication.setStatus(OdApplicationStatus.ENQUIRY);
//        }
        if (OdApplicationStatus.ENQUIRY.equals(odApplication.getStatus()) && OdApplicationStatus.CLOSED.equals(odApplicationPayload.getStatus())) {
            odApplication.setStatus(OdApplicationStatus.CLOSED);
        }

        odApplication.setModifiedAt(System.currentTimeMillis());
        odApplicationRepository.save(odApplication);
        notificationService.sendNotification(odApplication);
        return odApplicationTransformer.transform(ODApplicationTransformationRequest.builder().odApplication(odApplication).principalUser(principal).build());
    }

    @LogPayload
    @Timed
    public OdApplicationPayload get(UUID odUuid, User principal) {
        OdApplication odApplication = odApplicationRepository.findByUuid(odUuid);
        List<OdApplicationAssignment> assignments = odApplicationAssignmentRepository.findLatestAssignmentForEachAssignee(odApplication);
        return odApplicationTransformer.transform(ODApplicationTransformationRequest.builder().odApplication(odApplication).principalUser(principal).assignments(assignments).build());
    }

    @LogPayload
    @Timed
    public List<OdApplicationPayload> getList(String odApplicationStatus, User principal, List<UUID> geoHierarchyNodeUuids) {
        Map<Post, List<UUID>> geoNodes = geoHierarchyService.resolveGeoHierarchyNodes(principal.getPostGeoHierarchyNodeUuidMap(), geoHierarchyNodeUuids);
        OdApplicationStatus status = null;
        if (StringUtils.isNotEmpty(odApplicationStatus)) {
            status = OdApplicationStatus.valueOf(odApplicationStatus);
        }
        List<UUID> authorityNodes = geoHierarchyService.getAllLevelNodesOfAuthorityPost(geoNodes);
        List<OdApplication> result = new ArrayList<>();
        if (status != null) {
            if (CollectionUtils.isEmpty(authorityNodes)) {
                result = odApplicationRepository.findByOdOrEnquiryOfficerAndStatus(principal, status);
            } else {
                result = odApplicationRepository.findByGeoHierarchyNodeUuidInAndStatus(geoHierarchyService.getAllLevelNodesOfAuthorityPost(geoNodes), status);
            }
        } else {
            if (CollectionUtils.isEmpty(authorityNodes)) {
                result = odApplicationRepository.findByOdOrEnquiryOfficer(principal);
            } else {
                result = odApplicationRepository.findByGeoHierarchyNodeUuidIn(geoHierarchyService.getAllLevelNodesOfAuthorityPost(geoNodes));
            }
        }
        return result.stream()
                .map((odApplication) -> {
                    List<OdApplicationAssignment> assignments = odApplicationAssignmentRepository.findLatestAssignmentForEachAssignee(odApplication);
                    return odApplicationTransformer.transform(ODApplicationTransformationRequest.builder().assignments(assignments).odApplication(odApplication).principalUser(principal).build());
                })
                .collect(Collectors.toList());

    }

    @LogPayload
    @Timed
    public List<OdApplicationPayload> getReceiptList(User principal, List<UUID> geoHierarchyNodeUuids) {
        Map<Post, List<UUID>> geoNodes = geoHierarchyService.resolveGeoHierarchyNodes(principal.getPostGeoHierarchyNodeUuidMap(), geoHierarchyNodeUuids);
        List<UUID> authorityNodes = geoHierarchyService.getAllLevelNodesOfAuthorityPost(geoNodes);
        if (CollectionUtils.isEmpty(authorityNodes)) {
            return odApplicationRepository.findByOd(principal)
                    .stream()
                    .map((odApplication) -> odApplicationTransformer.transform(ODApplicationTransformationRequest.builder().odApplication(odApplication).principalUser(principal).build()))
                    .collect(Collectors.toList());
        }
        return odApplicationRepository.findByGeoHierarchyNodeUuidIn(geoHierarchyService.getAllLevelNodesOfAuthorityPost(geoNodes))
                .stream()
                .map((odApplication) -> odApplicationTransformer.transform(ODApplicationTransformationRequest.builder().odApplication(odApplication).principalUser(principal).build()))
                .collect(Collectors.toList());

    }

    @LogPayload
    @Timed
    public AnalyticalResponse getDashboardAnalytics(User principal, List<UUID> geoHierarchyNodeUuids) {
        Map<Post, List<UUID>> geoNodes = geoHierarchyService.resolveGeoHierarchyNodes(principal.getPostGeoHierarchyNodeUuidMap(), geoHierarchyNodeUuids);
        List<UUID> authorityNodes = geoHierarchyService.getAllLevelNodesOfAuthorityPost(geoNodes);
        List<Object[]> allPostGeoAnalyticalRecord = new ArrayList<>();
        List<Object[]> selfAllPostGeoAnalyticalRecord = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(authorityNodes)) {
            allPostGeoAnalyticalRecord = odApplicationRepository.countByStatusForGeoNodes(authorityNodes);
        }
        selfAllPostGeoAnalyticalRecord = odApplicationRepository.countByStatusForOdOfficer(principal);
        Map<OdApplicationStatus, Long> geoStatusCountMap = allPostGeoAnalyticalRecord.stream()
                .filter(record -> !Objects.isNull(record[0]))
                .collect(Collectors.toMap(
                        record -> (OdApplicationStatus) record[0],  // status, which might be null
                        record -> (Long) record[1],                 // count
                        Long::sum                                  // in case of duplicate keys, sum the values
                ));
        Map<OdApplicationStatus, Long> selfStatusCountMap = selfAllPostGeoAnalyticalRecord.stream()
                .filter(record -> !Objects.isNull(record[0]))
                .collect(Collectors.toMap(
                        record -> (OdApplicationStatus) record[0],  // status, which might be null
                        record -> (Long) record[1],                 // count
                        Long::sum                                  // in case of duplicate keys, sum the values
                ));
        for (OdApplicationStatus odApplicationStatus : Arrays.asList(OdApplicationStatus.REVIEW, OdApplicationStatus.OPEN, OdApplicationStatus.CLOSED)) {
            selfStatusCountMap.put(odApplicationStatus, geoStatusCountMap.getOrDefault(odApplicationStatus, 0L));
        }
        return AnalyticalResponse.builder()
                .statusCountMap(geoStatusCountMap)
                .self_statusCountMap(selfStatusCountMap)
                .build();
    }

    public String getAnalytics(User principal, List<UUID> geoHierarchyNodeUuids) {
        Map<Post, List<UUID>> geoNodes = geoHierarchyService.resolveGeoHierarchyNodes(principal.getPostGeoHierarchyNodeUuidMap(), geoHierarchyNodeUuids);
        return """
                                
                """;
    }

    public void createAssignment(List<OdAssignmentPayload> assignmentRequests, UUID odApplicationUuid, User principal, List<UUID> geoHierarchyNodeUuids) {
        OdApplication odApplication = odApplicationRepository.findByUuid(odApplicationUuid);
        for (OdAssignmentPayload assignmentPojo : assignmentRequests) {
            User assignee = userRepository.findByUuid(assignmentPojo.getAssigneeUuid());
            OdApplicationAssignment odApplicationAssignment = OdApplicationAssignment.builder()
                    .uuid(UUID.randomUUID())
                    .application(odApplication)
                    .enquiryOfficer(assignee)
                    .createdAt(System.currentTimeMillis())
                    .modifiedAt(System.currentTimeMillis())
                    .status(OdApplicationStatus.ENQUIRY)
                    .build();
            odApplicationAssignmentRepository.save(odApplicationAssignment);
        }
        odApplication.setStatus(OdApplicationStatus.ENQUIRY);
        odApplicationRepository.save(odApplication);
    }

    public OdAssignmentPayload updateAssignment(OdAssignmentPayload assignmentPayload, UUID assignmentUuid, User principal) {
        OdApplicationAssignment odApplicationAssignment = odApplicationAssignmentRepository.findByUuid(assignmentUuid);
        if (StringUtils.isNotEmpty(assignmentPayload.getFilePath())) {
            odApplicationAssignment.setFilePath(assignmentPayload.getFilePath());
            odApplicationAssignment.setStatus(OdApplicationStatus.REVIEW);
        }
        if (OdApplicationStatus.REVIEW.equals(odApplicationAssignment.getStatus()) && OdApplicationStatus.ENQUIRY.equals(assignmentPayload.getStatus())) {
            odApplicationAssignment.setStatus(OdApplicationStatus.ENQUIRY);
            odApplicationAssignment.setFilePath(null);
        }
        if (OdApplicationStatus.REVIEW.equals(odApplicationAssignment.getStatus()) && OdApplicationStatus.CLOSED.equals(assignmentPayload.getStatus())) {
            odApplicationAssignment.setStatus(OdApplicationStatus.CLOSED);
        }
        odApplicationAssignment.setModifiedAt(System.currentTimeMillis());
        odApplicationAssignmentRepository.save(odApplicationAssignment);
        return odApplicationAssignmentTransformer.transform(odApplicationAssignment);
    }
}