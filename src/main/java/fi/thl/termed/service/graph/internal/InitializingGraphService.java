package fi.thl.termed.service.graph.internal;

import static java.util.UUID.randomUUID;

import fi.thl.termed.domain.Graph;
import fi.thl.termed.domain.GraphId;
import fi.thl.termed.domain.User;
import fi.thl.termed.util.UUIDs;
import fi.thl.termed.util.service.ForwardingService;
import fi.thl.termed.util.service.SaveMode;
import fi.thl.termed.util.service.Service;
import fi.thl.termed.util.service.WriteOptions;
import java.util.UUID;
import java.util.stream.Stream;

public class InitializingGraphService extends ForwardingService<GraphId, Graph> {

  public InitializingGraphService(Service<GraphId, Graph> delegate) {
    super(delegate);
  }

  @Override
  public void save(Stream<Graph> graphs, SaveMode mode, WriteOptions opts, User user) {
    super.save(graphs.map(this::initialize), mode, opts, user);
  }

  @Override
  public GraphId save(Graph graph, SaveMode mode, WriteOptions opts, User user) {
    return super.save(initialize(graph), mode, opts, user);
  }

  @Override
  public void saveAndDelete(Stream<Graph> saves, Stream<GraphId> deletes, SaveMode mode,
      WriteOptions opts, User user) {
    super.saveAndDelete(saves.map(this::initialize), deletes, mode, opts, user);
  }

  private Graph initialize(Graph graph) {
    if (graph.getId() == null) {
      UUID id = graph.getCode()
          .map(UUIDs::nameUUIDFromString)
          .orElse(graph.getUri()
              .map(UUIDs::nameUUIDFromString)
              .orElse(randomUUID()));

      return Graph.builder().id(id)
          .copyOptionalsFrom(graph)
          .build();
    }
    return graph;
  }

}
