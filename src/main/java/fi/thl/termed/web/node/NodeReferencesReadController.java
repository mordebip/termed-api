package fi.thl.termed.web.node;

import static fi.thl.termed.util.collect.FunctionUtils.partialApplySecond;

import com.google.common.collect.ImmutableList;
import fi.thl.termed.domain.Node;
import fi.thl.termed.domain.NodeId;
import fi.thl.termed.domain.User;
import fi.thl.termed.service.node.util.IndexedReferenceLoader;
import fi.thl.termed.service.node.util.IndexedReferrerLoader;
import fi.thl.termed.util.service.Service;
import fi.thl.termed.util.spring.annotation.GetJsonMapping;
import fi.thl.termed.util.spring.exception.NotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/graphs/{graphId}/types/{typeId}/nodes/{id}")
public class NodeReferencesReadController {

  @Autowired
  private Service<NodeId, Node> nodeService;

  @GetJsonMapping("/references")
  public List<Node> getReferences(
      @PathVariable("graphId") UUID graphId,
      @PathVariable("typeId") String typeId,
      @PathVariable("id") UUID id,
      @AuthenticationPrincipal User user) {

    Node root = nodeService.get(new NodeId(id, typeId, graphId), user)
        .orElseThrow(NotFoundException::new);

    Set<Node> nodes = new LinkedHashSet<>();
    for (String attributeId : root.getReferences().keys()) {
      nodes.addAll(new IndexedReferenceLoader(nodeService, user).apply(root, attributeId));
    }

    return new ArrayList<>(nodes);
  }

  @GetJsonMapping("/references/{attributeId}")
  public List<Node> getReferences(
      @PathVariable("graphId") UUID graphId,
      @PathVariable("typeId") String typeId,
      @PathVariable("id") UUID id,
      @PathVariable("attributeId") String attributeId,
      @AuthenticationPrincipal User user) {

    Node root = nodeService.get(new NodeId(id, typeId, graphId), user)
        .orElseThrow(NotFoundException::new);

    return new IndexedReferenceLoader(nodeService, user).apply(root, attributeId);
  }

  @GetJsonMapping("/references/{attributeId}/recursive")
  public Set<Node> getRecursiveReferences(
      @PathVariable("graphId") UUID graphId,
      @PathVariable("typeId") String typeId,
      @PathVariable("id") UUID id,
      @PathVariable("attributeId") String attributeId,
      @AuthenticationPrincipal User user) {

    Node root = nodeService.get(new NodeId(id, typeId, graphId), user)
        .orElseThrow(NotFoundException::new);

    Function<Node, ImmutableList<Node>> loadReferences =
        partialApplySecond(new IndexedReferenceLoader(nodeService, user), attributeId);

    Set<Node> results = new LinkedHashSet<>();
    for (Node neighbour : loadReferences.apply(root)) {
      collectNeighbours(results, neighbour, loadReferences);
    }
    return results;
  }

  @GetJsonMapping("/referrers")
  public List<Node> getReferrers(
      @PathVariable("graphId") UUID graphId,
      @PathVariable("typeId") String typeId,
      @PathVariable("id") UUID id,
      @AuthenticationPrincipal User user) {

    Node root = nodeService.get(new NodeId(id, typeId, graphId), user)
        .orElseThrow(NotFoundException::new);

    Set<Node> nodes = new LinkedHashSet<>();
    for (String attributeId : root.getReferrers().keys()) {
      nodes.addAll(new IndexedReferrerLoader(nodeService, user).apply(root, attributeId));
    }

    return new ArrayList<>(nodes);
  }

  @GetJsonMapping("/referrers/{attributeId}")
  public List<Node> getReferrers(
      @PathVariable("graphId") UUID graphId,
      @PathVariable("typeId") String typeId,
      @PathVariable("id") UUID id,
      @PathVariable("attributeId") String attributeId,
      @AuthenticationPrincipal User user) {

    Node root = nodeService.get(new NodeId(id, typeId, graphId), user)
        .orElseThrow(NotFoundException::new);

    return new IndexedReferrerLoader(nodeService, user).apply(root, attributeId);
  }

  @GetJsonMapping("/referrers/{attributeId}/recursive")
  public Set<Node> getRecursiveReferrers(
      @PathVariable("graphId") UUID graphId,
      @PathVariable("typeId") String typeId,
      @PathVariable("id") UUID id,
      @PathVariable("attributeId") String attributeId,
      @AuthenticationPrincipal User user) {

    Node root = nodeService.get(new NodeId(id, typeId, graphId), user)
        .orElseThrow(NotFoundException::new);

    Function<Node, ImmutableList<Node>> loadReferrers =
        partialApplySecond(new IndexedReferrerLoader(nodeService, user), attributeId);

    Set<Node> results = new LinkedHashSet<>();
    for (Node neighbour : loadReferrers.apply(root)) {
      collectNeighbours(results, neighbour, loadReferrers);
    }
    return results;
  }

  private <T> void collectNeighbours(Set<T> results, T node,
      Function<T, ImmutableList<T>> getNeighbours) {
    if (!results.contains(node)) {
      results.add(node);
      for (T neighbour : getNeighbours.apply(node)) {
        collectNeighbours(results, neighbour, getNeighbours);
      }
    }
  }

}
