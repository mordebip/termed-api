package fi.thl.termed.service.type.internal;

import com.google.common.base.Strings;
import fi.thl.termed.domain.GraphId;
import fi.thl.termed.domain.TextAttribute;
import fi.thl.termed.domain.TextAttributeId;
import fi.thl.termed.domain.TypeId;
import fi.thl.termed.util.dao.AbstractJdbcDao;
import fi.thl.termed.util.query.SqlSpecification;
import java.util.Optional;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.springframework.jdbc.core.RowMapper;

public class JdbcTextAttributeDao extends AbstractJdbcDao<TextAttributeId, TextAttribute> {

  public JdbcTextAttributeDao(DataSource dataSource) {
    super(dataSource);
  }

  @Override
  public void insert(TextAttributeId textAttributeId, TextAttribute textAttribute) {
    TypeId domainId = textAttributeId.getDomainId();

    jdbcTemplate.update(
        "insert into text_attribute (domain_graph_id, domain_id, id, uri, regex, index) values (?, ?, ?, ?, ?, ?)",
        domainId.getGraphId(),
        domainId.getId(),
        textAttributeId.getId(),
        textAttribute.getUri().map(Strings::emptyToNull).orElse(null),
        textAttribute.getRegex(),
        textAttribute.getIndex().orElse(null));
  }

  @Override
  public void update(TextAttributeId textAttributeId, TextAttribute textAttribute) {
    TypeId domainId = textAttributeId.getDomainId();

    jdbcTemplate.update(
        "update text_attribute set uri = ?, regex = ?, index = ? where domain_graph_id = ? and domain_id = ? and id = ?",
        textAttribute.getUri().map(Strings::emptyToNull).orElse(null),
        textAttribute.getRegex(),
        textAttribute.getIndex().orElse(null),
        domainId.getGraphId(),
        domainId.getId(),
        textAttributeId.getId());
  }

  @Override
  public void delete(TextAttributeId textAttributeId) {
    TypeId domainId = textAttributeId.getDomainId();

    jdbcTemplate.update(
        "delete from text_attribute where domain_graph_id = ? and domain_id = ? and id = ?",
        domainId.getGraphId(),
        domainId.getId(),
        textAttributeId.getId());
  }

  @Override
  protected <E> Stream<E> get(SqlSpecification<TextAttributeId, TextAttribute> specification,
      RowMapper<E> mapper) {
    return jdbcTemplate.queryForStream(
        String.format("select * from text_attribute where %s order by index",
            specification.sqlQueryTemplate()),
        specification.sqlQueryParameters(), mapper);
  }

  @Override
  public boolean exists(TextAttributeId textAttributeId) {
    TypeId domainId = textAttributeId.getDomainId();

    return jdbcTemplate.queryForOptional(
        "select count(*) from text_attribute where domain_graph_id = ? and domain_id = ? and id = ?",
        Long.class,
        domainId.getGraphId(),
        domainId.getId(),
        textAttributeId.getId()).orElseThrow(IllegalStateException::new) > 0;
  }

  @Override
  protected <E> Optional<E> get(TextAttributeId textAttributeId, RowMapper<E> mapper) {
    TypeId domainId = textAttributeId.getDomainId();

    return jdbcTemplate.queryForFirst(
        "select * from text_attribute where domain_graph_id = ? and domain_id = ? and id = ?",
        mapper,
        domainId.getGraphId(),
        domainId.getId(),
        textAttributeId.getId());
  }

  @Override
  protected RowMapper<TextAttributeId> buildKeyMapper() {
    return (rs, rowNum) -> {
      TypeId domainId = TypeId.of(rs.getString("domain_id"),
          GraphId.fromUuidString(rs.getString("domain_graph_id")));
      return new TextAttributeId(domainId, rs.getString("id"));
    };
  }

  @Override
  protected RowMapper<TextAttribute> buildValueMapper() {
    return (rs, rowNum) -> {
      TypeId domain = TypeId.of(
          rs.getString("domain_id"),
          GraphId.fromUuidString(rs.getString("domain_graph_id")));

      return TextAttribute.builder()
          .id(rs.getString("id"), domain)
          .regex(rs.getString("regex"))
          .uri(rs.getString("uri"))
          .index(rs.getInt("index"))
          .build();
    };
  }

}
