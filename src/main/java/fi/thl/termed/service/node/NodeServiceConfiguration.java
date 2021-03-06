package fi.thl.termed.service.node;

import static fi.thl.termed.domain.Permission.INSERT;
import static fi.thl.termed.domain.Permission.READ;
import static fi.thl.termed.util.Converter.newConverter;
import static fi.thl.termed.util.spring.jdbc.SpringJdbcUtils.getDatabaseProductName;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import fi.thl.termed.domain.AppRole;
import fi.thl.termed.domain.Empty;
import fi.thl.termed.domain.Graph;
import fi.thl.termed.domain.GraphId;
import fi.thl.termed.domain.IndexingQueueItemId;
import fi.thl.termed.domain.Node;
import fi.thl.termed.domain.NodeAttributeValueId;
import fi.thl.termed.domain.NodeId;
import fi.thl.termed.domain.ReferenceAttributeId;
import fi.thl.termed.domain.Revision;
import fi.thl.termed.domain.RevisionId;
import fi.thl.termed.domain.RevisionType;
import fi.thl.termed.domain.StrictLangValue;
import fi.thl.termed.domain.TextAttributeId;
import fi.thl.termed.domain.Type;
import fi.thl.termed.domain.TypeId;
import fi.thl.termed.domain.event.ApplicationShutdownEvent;
import fi.thl.termed.service.node.internal.AttributeValueInitializingNodeService;
import fi.thl.termed.service.node.internal.DocumentToNode;
import fi.thl.termed.service.node.internal.IdInitializingNodeService;
import fi.thl.termed.service.node.internal.IndexedNodeService;
import fi.thl.termed.service.node.internal.JdbcNodeDao;
import fi.thl.termed.service.node.internal.JdbcNodeIndexingQueueDao;
import fi.thl.termed.service.node.internal.JdbcNodeIndexingQueueItemDao;
import fi.thl.termed.service.node.internal.JdbcNodeNamespaceSequenceDao;
import fi.thl.termed.service.node.internal.JdbcNodeReferenceAttributeValueDao;
import fi.thl.termed.service.node.internal.JdbcNodeReferenceAttributeValueRevisionDao;
import fi.thl.termed.service.node.internal.JdbcNodeRevisionDao;
import fi.thl.termed.service.node.internal.JdbcNodeSequenceDao;
import fi.thl.termed.service.node.internal.JdbcNodeTextAttributeValueDao;
import fi.thl.termed.service.node.internal.JdbcNodeTextAttributeValueRevisionDao;
import fi.thl.termed.service.node.internal.JdbcPostgresNodeDao;
import fi.thl.termed.service.node.internal.JdbcPostgresNodeReferenceAttributeValueDao;
import fi.thl.termed.service.node.internal.JdbcPostgresNodeReferenceAttributeValueRevisionDao;
import fi.thl.termed.service.node.internal.JdbcPostgresNodeRevisionDao;
import fi.thl.termed.service.node.internal.JdbcPostgresNodeTextAttributeValueDao;
import fi.thl.termed.service.node.internal.JdbcPostgresNodeTextAttributeValueRevisionDao;
import fi.thl.termed.service.node.internal.NodeMetadataInitializingService;
import fi.thl.termed.service.node.internal.NodeRepository;
import fi.thl.termed.service.node.internal.NodeRevisionRepository;
import fi.thl.termed.service.node.internal.NodeToDocument;
import fi.thl.termed.service.node.internal.NodeWriteEventPostingService;
import fi.thl.termed.service.node.internal.ReadAuthorizedNodeService;
import fi.thl.termed.service.node.internal.RevisionInitializingNodeService;
import fi.thl.termed.util.collect.Tuple2;
import fi.thl.termed.util.dao.AuthorizedDao;
import fi.thl.termed.util.dao.JdbcSystemSequenceDao;
import fi.thl.termed.util.dao.SystemDao;
import fi.thl.termed.util.dao.SystemSequenceDao;
import fi.thl.termed.util.dao.TransactionalSystemDao;
import fi.thl.termed.util.index.Index;
import fi.thl.termed.util.index.lucene.LuceneIndex;
import fi.thl.termed.util.permission.DisjunctionPermissionEvaluator;
import fi.thl.termed.util.permission.PermissionEvaluator;
import fi.thl.termed.util.service.CachedNamedSequenceService;
import fi.thl.termed.util.service.DaoNamedSequenceService;
import fi.thl.termed.util.service.ForwardingNamedSequenceService;
import fi.thl.termed.util.service.NamedSequenceService;
import fi.thl.termed.util.service.ProfilingService;
import fi.thl.termed.util.service.SequenceService;
import fi.thl.termed.util.service.Service;
import fi.thl.termed.util.service.SynchronizedNamedSequenceService;
import fi.thl.termed.util.service.TransactionalNamedSequenceService;
import fi.thl.termed.util.service.TransactionalService;
import fi.thl.termed.util.service.WriteLoggingService;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class NodeServiceConfiguration {

  @Autowired
  private DataSource dataSource;
  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private PermissionEvaluator<GraphId> graphEvaluator;
  @Autowired
  private PermissionEvaluator<TypeId> typeEvaluator;
  @Autowired
  private PermissionEvaluator<TextAttributeId> textAttributeEvaluator;
  @Autowired
  private PermissionEvaluator<ReferenceAttributeId> referenceAttributeEvaluator;

  @Autowired
  private Service<TypeId, Type> typeService;
  @Autowired
  private Service<GraphId, Graph> graphService;
  @Autowired
  private SequenceService revisionSeqService;
  @Autowired
  private Service<Long, Revision> revisionService;
  @Value("${fi.thl.termed.defaultNamespace:}")
  private String ns;

  @Value("${fi.thl.termed.index:}")
  private String indexPath;

  @Autowired
  private EventBus eventBus;

  private String packageName = getClass().getPackage().getName();

  @Bean
  public Service<NodeId, Node> nodeService() {
    Service<NodeId, Node> service = nodeRepository();
    service = new TransactionalService<>(service, transactionManager);

    service = new IndexedNodeService(service,
        nodeIndex(),
        nodeIndexingQueueSequenceDao(),
        nodeIndexingQueueDao(),
        nodeIndexingQueueItemDao());
    eventBus.register(service);

    service = new ReadAuthorizedNodeService(service,
        typeEvaluator, textAttributeEvaluator, referenceAttributeEvaluator);

    service = new WriteLoggingService<>(service, packageName + ".WriteLoggingService");
    service = new NodeWriteEventPostingService(service, nodeRevisionService(), eventBus);
    service = new RevisionInitializingNodeService(service, revisionSeqService, revisionService);

    service = new NodeMetadataInitializingService(service, nodeSequenceService(),
        nodeNamespaceSequenceService(), typeService::get, graphService::get, ns);
    service = new AttributeValueInitializingNodeService(service, typeService::get);
    service = new ProfilingService<>(service, packageName + ".ProfilingService", 500);

    service = new IdInitializingNodeService(service);

    return service;
  }

  @Bean
  public Service<RevisionId<NodeId>, Tuple2<RevisionType, Node>> nodeRevisionService() {
    Service<RevisionId<NodeId>, Tuple2<RevisionType, Node>> service = nodeRevisionRepository();
    service = new TransactionalService<>(service, transactionManager);
    return service;
  }

  private Index<NodeId, Node> nodeIndex() {
    return new LuceneIndex<>(
        indexPath,
        newConverter(NodeId::toString, NodeId::fromString),
        newConverter(new NodeToDocument(), new DocumentToNode()));
  }

  private Service<NodeId, Node> nodeRepository() {
    return new NodeRepository(
        new AuthorizedDao<>(nodeSystemDao(), nodeEvaluator()),
        new AuthorizedDao<>(textAttributeValueSystemDao(), textAttributeValueEvaluator()),
        new AuthorizedDao<>(referenceAttributeValueSystemDao(),
            referenceAttributeValueEvaluator()),
        new AuthorizedDao<>(nodeRevSysDao(), nodeRevEvaluator()),
        new AuthorizedDao<>(textAttributeValueRevSysDao(), textAttributeValueRevEvaluator()),
        new AuthorizedDao<>(referenceAttributeValueRevSysDao(), refAttributeValueRevEvaluator()),
        getDatabaseProductName(dataSource).equalsIgnoreCase("postgresql") ? 5000 : -1);
  }

  private NamedSequenceService<TypeId> nodeSequenceService() {
    NamedSequenceService<TypeId> sequenceService =
        new DaoNamedSequenceService<>(
            new AuthorizedDao<>(nodeSequenceSystemDao(), nodeSequenceEvaluator()));

    sequenceService = new CachedNamedSequenceService<>(sequenceService);
    sequenceService = new ForwardingNamedSequenceService<TypeId>(sequenceService) {
      @Subscribe
      public void discardCachesOn(ApplicationShutdownEvent e) {
        close();
      }
    };
    eventBus.register(sequenceService);
    sequenceService = new TransactionalNamedSequenceService<>(sequenceService, transactionManager);
    sequenceService = new SynchronizedNamedSequenceService<>(sequenceService);

    return sequenceService;
  }

  private NamedSequenceService<Tuple2<GraphId, String>> nodeNamespaceSequenceService() {
    NamedSequenceService<Tuple2<GraphId, String>> sequenceService =
        new DaoNamedSequenceService<>(
            new AuthorizedDao<>(
                nodeNamespaceSequenceSystemDao(),
                nodeNamespaceSequenceEvaluator()));

    sequenceService = new CachedNamedSequenceService<>(sequenceService);
    sequenceService = new ForwardingNamedSequenceService<Tuple2<GraphId, String>>(sequenceService) {
      @Subscribe
      public void discardCachesOn(ApplicationShutdownEvent e) {
        close();
      }
    };
    eventBus.register(sequenceService);
    sequenceService = new TransactionalNamedSequenceService<>(sequenceService, transactionManager);
    sequenceService = new SynchronizedNamedSequenceService<>(sequenceService);

    return sequenceService;
  }

  private Service<RevisionId<NodeId>, Tuple2<RevisionType, Node>> nodeRevisionRepository() {
    return new NodeRevisionRepository(
        new AuthorizedDao<>(nodeRevSysDao(), nodeRevEvaluator()),
        new AuthorizedDao<>(textAttributeValueRevSysDao(), textAttributeValueRevEvaluator()),
        new AuthorizedDao<>(referenceAttributeValueRevSysDao(), refAttributeValueRevEvaluator()),
        revisionService, revisionSeqService);
  }

  private PermissionEvaluator<TypeId> nodeSequenceEvaluator() {
    return new DisjunctionPermissionEvaluator<>(appAdminEvaluator(), typeEvaluator);
  }

  private PermissionEvaluator<Tuple2<GraphId, String>> nodeNamespaceSequenceEvaluator() {
    return new DisjunctionPermissionEvaluator<>(appAdminEvaluator(),
        ((u, o, p) -> graphEvaluator.hasPermission(u, o._1, p)));
  }

  private PermissionEvaluator<NodeId> nodeEvaluator() {
    return new DisjunctionPermissionEvaluator<>(appAdminEvaluator(),
        (u, o, p) -> typeEvaluator.hasPermission(u, o.getType(), p));
  }

  private PermissionEvaluator<NodeAttributeValueId> textAttributeValueEvaluator() {
    return new DisjunctionPermissionEvaluator<>(appAdminEvaluator(),
        (u, o, p) -> textAttributeEvaluator.hasPermission(
            u, new TextAttributeId(o.getNodeId().getType(), o.getAttributeId()), p));
  }

  private PermissionEvaluator<NodeAttributeValueId> referenceAttributeValueEvaluator() {
    return new DisjunctionPermissionEvaluator<>(appAdminEvaluator(),
        (u, o, p) -> referenceAttributeEvaluator.hasPermission(
            u, new ReferenceAttributeId(o.getNodeId().getType(), o.getAttributeId()), p));
  }

  private PermissionEvaluator<RevisionId<NodeId>> nodeRevEvaluator() {
    return new DisjunctionPermissionEvaluator<>(appAdminEvaluator(), (u, o, p) ->
        p == INSERT || (p == READ && typeEvaluator.hasPermission(u, o.getId().getType(), p)));
  }

  private PermissionEvaluator<RevisionId<NodeAttributeValueId>> textAttributeValueRevEvaluator() {
    return new DisjunctionPermissionEvaluator<>(appAdminEvaluator(), (u, o, p) -> {
      NodeAttributeValueId id = o.getId();
      TextAttributeId attrId = new TextAttributeId(id.getNodeId().getType(), id.getAttributeId());
      return p == INSERT || (p == READ && textAttributeEvaluator.hasPermission(u, attrId, p));
    });
  }

  private PermissionEvaluator<RevisionId<NodeAttributeValueId>> refAttributeValueRevEvaluator() {
    return new DisjunctionPermissionEvaluator<>(appAdminEvaluator(), (u, o, p) -> {
      NodeAttributeValueId id = o.getId();
      ReferenceAttributeId attrId = new ReferenceAttributeId(id.getNodeId().getType(),
          id.getAttributeId());
      return p == INSERT || (p == READ && referenceAttributeEvaluator.hasPermission(u, attrId, p));
    });
  }

  private SystemDao<TypeId, Long> nodeSequenceSystemDao() {
    return new JdbcNodeSequenceDao(dataSource);
  }

  private SystemDao<Tuple2<GraphId, String>, Long> nodeNamespaceSequenceSystemDao() {
    return new JdbcNodeNamespaceSequenceDao(dataSource);
  }

  private SystemDao<NodeId, Node> nodeSystemDao() {
    SystemDao<NodeId, Node> nodeDao = new JdbcNodeDao(dataSource);
    return new JdbcPostgresNodeDao(nodeDao, dataSource);
  }

  private SystemDao<NodeAttributeValueId, StrictLangValue> textAttributeValueSystemDao() {
    SystemDao<NodeAttributeValueId, StrictLangValue> textAttrValueDao =
        new JdbcNodeTextAttributeValueDao(dataSource);
    return new JdbcPostgresNodeTextAttributeValueDao(textAttrValueDao, dataSource);
  }

  private SystemDao<NodeAttributeValueId, NodeId> referenceAttributeValueSystemDao() {
    SystemDao<NodeAttributeValueId, NodeId> refAttrValueDao =
        new JdbcNodeReferenceAttributeValueDao(dataSource);
    return new JdbcPostgresNodeReferenceAttributeValueDao(refAttrValueDao, dataSource);
  }

  private SystemDao<RevisionId<NodeId>, Tuple2<RevisionType, Node>> nodeRevSysDao() {
    SystemDao<RevisionId<NodeId>, Tuple2<RevisionType, Node>> nodeRevDao =
        new JdbcNodeRevisionDao(dataSource);
    return new JdbcPostgresNodeRevisionDao(nodeRevDao, dataSource);
  }

  private SystemDao<RevisionId<NodeAttributeValueId>, Tuple2<RevisionType, StrictLangValue>> textAttributeValueRevSysDao() {
    SystemDao<RevisionId<NodeAttributeValueId>, Tuple2<RevisionType, StrictLangValue>> dao =
        new JdbcNodeTextAttributeValueRevisionDao(dataSource);
    return new JdbcPostgresNodeTextAttributeValueRevisionDao(dao, dataSource);
  }

  private SystemDao<RevisionId<NodeAttributeValueId>, Tuple2<RevisionType, NodeId>> referenceAttributeValueRevSysDao() {
    SystemDao<RevisionId<NodeAttributeValueId>, Tuple2<RevisionType, NodeId>> dao =
        new JdbcNodeReferenceAttributeValueRevisionDao(dataSource);
    return new JdbcPostgresNodeReferenceAttributeValueRevisionDao(dao, dataSource);
  }

  private SystemSequenceDao nodeIndexingQueueSequenceDao() {
    return new JdbcSystemSequenceDao(dataSource, "node_indexing_queue_seq");
  }

  private SystemDao<Long, Empty> nodeIndexingQueueDao() {
    return new TransactionalSystemDao<>(
        new JdbcNodeIndexingQueueDao(dataSource), transactionManager);
  }

  private SystemDao<IndexingQueueItemId<NodeId>, Empty> nodeIndexingQueueItemDao() {
    return new TransactionalSystemDao<>(
        new JdbcNodeIndexingQueueItemDao(dataSource), transactionManager);
  }

  /**
   * Creates type specific permission evaluator that accepts users that are admins or superusers
   */
  private <T> PermissionEvaluator<T> appAdminEvaluator() {
    return (user, object, permission) -> user.getAppRole() == AppRole.ADMIN ||
        user.getAppRole() == AppRole.SUPERUSER;
  }

}
