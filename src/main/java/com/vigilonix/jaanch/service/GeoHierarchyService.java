package com.vigilonix.jaanch.service;

import com.vigilonix.jaanch.enums.Post;
import com.vigilonix.jaanch.pojo.GeoHierarchyNode;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GeoHierarchyService {
    private final GeoHierarchyNode rootNode;
    private final Map<UUID, GeoHierarchyNode> nodeByUuid;
    private final Map<GeoHierarchyNode, GeoHierarchyNode> parentMap;
    private final Set<UUID> testNodes;

    @Autowired
    public GeoHierarchyService(GeoHierarchyNode rootNode) {
        this.rootNode = rootNode;
        this.nodeByUuid = new HashMap<>();
        this.parentMap = new HashMap<>();
        testNodes = new HashSet<>();

        // Initialize the index and parent-child relationships
        initializeGeoHierarchyNodeMaps();
    }

    // Initialization methods
    private void initializeGeoHierarchyNodeMaps() {
        Queue<GeoHierarchyNode> bfsQueue = new LinkedList<>();
        bfsQueue.offer(rootNode);
        while (!bfsQueue.isEmpty()) {
            GeoHierarchyNode currentNode = bfsQueue.poll();
            nodeByUuid.put(currentNode.getUuid(), currentNode);
            currentNode.getChildren().forEach(child -> {
                parentMap.put(child, currentNode);
                if(BooleanUtils.isTrue(currentNode.getIsTest()) || testNodes.contains(currentNode.getUuid())) {
                    testNodes.add(child.getUuid());
                    testNodes.add(currentNode.getUuid());
                }
                bfsQueue.offer(child);
            });
        }
    }

    // Node retrieval methods
    public GeoHierarchyNode getNodeById(UUID uuid) {
        return nodeByUuid.get(uuid);
    }

    private List<GeoHierarchyNode> getAllLevelNodes(GeoHierarchyNode startNode) {
        List<GeoHierarchyNode> reachableNodes = new ArrayList<>();
        Queue<GeoHierarchyNode> bfsQueue = new LinkedList<>();
        bfsQueue.offer(startNode);
        while (!bfsQueue.isEmpty()) {
            GeoHierarchyNode node = bfsQueue.poll();
            reachableNodes.add(node);
            node.getChildren().forEach(bfsQueue::offer);
        }
        return reachableNodes;
    }

    public GeoHierarchyNode getHighestPostNode(Map<Post, List<UUID>> postGeoNodeMap) {
        Post highestPost = findHighestPost(postGeoNodeMap);
        return nodeByUuid.get(postGeoNodeMap.get(highestPost).get(0));
    }

    // Node filtering methods
    private List<GeoHierarchyNode> getFirstLevelNodesOfAuthorityPost(Map<Post, List<UUID>> postGeoNodeMap) {
        return postGeoNodeMap.entrySet().stream()
                .filter(entry -> entry.getKey().getLevel() > 1)
                .flatMap(entry -> entry.getValue().stream())
                .map(nodeByUuid::get)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<UUID> getAllLevelNodesOfAuthorityPost(Map<Post, List<UUID>> postGeoNodeMap) {
        return getFirstLevelNodesOfAuthorityPost(postGeoNodeMap).stream()
                .flatMap(node -> getAllLevelNodes(node).stream())
                .map(GeoHierarchyNode::getUuid)
                .collect(Collectors.toList());
    }

    public List<UUID> getAllLevelNodes(Map<Post, List<UUID>> postGeoNodeMap) {
        return postGeoNodeMap.values().stream()
                .flatMap(Collection::stream)
                .map(nodeByUuid::get)
                .flatMap(node -> getAllLevelNodes(node).stream())
                .map(GeoHierarchyNode::getUuid)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<UUID> getFirstLevelNodes(Map<Post, List<UUID>> postGeoNodeMap) {
        return postGeoNodeMap.values().stream()
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    public Post findHighestPost(Map<Post, List<UUID>> postGeoNodeMap) {
        return postGeoNodeMap.keySet().stream()
                .max(Comparator.comparingInt(Post::getLevel))
                .orElseThrow(() -> new NoSuchElementException("No posts found"));
    }

    // Authorization methods
    public boolean hasAuthority(UUID geoHierarchyNodeUuid, Map<Post, List<UUID>> principalPostMap) {
        return getAllLevelNodesOfAuthorityPost(principalPostMap).contains(geoHierarchyNodeUuid);
    }

    // Utility methods
    public GeoHierarchyNode transformWithoutChildren(GeoHierarchyNode node) {
        return GeoHierarchyNode.builder()
                .uuid(node.getUuid())
                .name(node.getName())
                .geofence(node.getGeofence())
                .type(node.getType())
                .build();
    }

    public boolean isTestNode(UUID geoHierarchyNodeUuid) {
        return testNodes.contains(geoHierarchyNodeUuid);
    }

    public List<GeoHierarchyNode> getAllLevelNodes(UUID geoHierarchyNodeUuid) {
        return getAllLevelNodes(nodeByUuid.get(geoHierarchyNodeUuid));
    }
}
