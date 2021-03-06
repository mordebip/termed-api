package fi.thl.termed.service.node.internal;

import static java.util.Optional.ofNullable;

import com.google.common.base.Strings;
import fi.thl.termed.domain.Node;
import fi.thl.termed.domain.NodeId;
import fi.thl.termed.domain.RevisionId;
import fi.thl.termed.domain.RevisionType;
import fi.thl.termed.util.collect.Tuple2;
import fi.thl.termed.util.dao.AbstractJdbcPostgresDao;
import fi.thl.termed.util.dao.SystemDao;
import java.util.Optional;
import javax.sql.DataSource;

public class JdbcPostgresNodeRevisionDao extends
    AbstractJdbcPostgresDao<RevisionId<NodeId>, Tuple2<RevisionType, Node>> {

  public JdbcPostgresNodeRevisionDao(
      SystemDao<RevisionId<NodeId>, Tuple2<RevisionType, Node>> delegate, DataSource dataSource) {
    super(delegate, dataSource, "node_aud");
  }

  @Override
  protected String[] toRow(RevisionId<NodeId> k, Tuple2<RevisionType, Node> v) {
    NodeId nodeId = k.getId();
    Optional<Node> node = ofNullable(v._2);

    return new String[]{
        nodeId.getTypeGraphId().toString(),
        nodeId.getTypeId(),
        nodeId.getId().toString(),
        node.flatMap(Node::getCode).map(Strings::emptyToNull).orElse(null),
        node.flatMap(Node::getUri).map(Strings::emptyToNull).orElse(null),
        node.map(Node::getNumber).map(Object::toString).orElse(null),
        node.map(Node::getCreatedBy).orElse(null),
        node.map(Node::getCreatedDate).map(Object::toString).orElse(null),
        node.map(Node::getLastModifiedBy).orElse(null),
        node.map(Node::getLastModifiedDate).map(Object::toString).orElse(null),
        k.getRevision().toString(),
        v._1.toString()
    };
  }

}
