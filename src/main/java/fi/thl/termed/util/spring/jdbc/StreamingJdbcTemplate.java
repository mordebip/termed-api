package fi.thl.termed.util.spring.jdbc;

import static com.google.common.collect.Streams.stream;
import static fi.thl.termed.util.DurationUtils.prettyPrintMillis;
import static fi.thl.termed.util.spring.jdbc.SpringJdbcUtils.resultSetToMappingIterator;
import static java.lang.System.currentTimeMillis;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MINUTES;

import fi.thl.termed.util.collect.StreamUtils;
import fi.thl.termed.util.concurrent.ExecutorUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;

public class StreamingJdbcTemplate {

  private static final int STREAM_TIMEOUT_IN_MINUTES = 60 * 6;

  private JdbcTemplate jdbcTemplate;

  private Logger log = LoggerFactory.getLogger(getClass());
  private ScheduledExecutorService executor;

  public StreamingJdbcTemplate(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
    this.executor = ExecutorUtils.newScheduledThreadPool(5);
  }

  public void update(String sql, Object... args) {
    jdbcTemplate.update(sql, args);
  }

  public <T> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper, Object... args)
      throws DataAccessException {
    return queryForStream(sql, args, rowMapper);
  }

  public <T> Stream<T> queryForStream(String sql, Object[] args, RowMapper<T> rowMapper)
      throws DataAccessException {

    DataSource dataSource = requireNonNull(jdbcTemplate.getDataSource());

    Connection connection = DataSourceUtils.getConnection(dataSource);

    try {
      PreparedStatement preparedStatement = connection.prepareStatement(sql);
      preparedStatement.setFetchSize(500);

      new ArgumentPreparedStatementSetter(args).setValues(preparedStatement);

      ResultSet resultSet = preparedStatement.executeQuery();

      Stream<T> results = stream(resultSetToMappingIterator(resultSet, rowMapper))
          .onClose(() -> {
            JdbcUtils.closeStatement(preparedStatement);
            JdbcUtils.closeResultSet(resultSet);
            DataSourceUtils.releaseConnection(connection, dataSource);
          });

      return withRecurringWarningIfKeptOpen(withTimeout(results, sql), sql, currentTimeMillis());
    } catch (SQLException | RuntimeException | Error e) {
      DataSourceUtils.releaseConnection(connection, dataSource);
      throw new RuntimeException(e);
    }
  }

  private <T> Stream<T> withTimeout(Stream<T> stream, String timeoutMessage) {
    return StreamUtils.toStreamWithTimeout(stream, executor, STREAM_TIMEOUT_IN_MINUTES, MINUTES,
        () -> timeoutMessage);
  }

  private <T> Stream<T> withRecurringWarningIfKeptOpen(Stream<T> stream, String sql, Long start) {
    return StreamUtils.toStreamWithScheduledRepeatingAction(stream, executor, 1, MINUTES,
        () -> log.debug("Result stream for {} kept open for {}",
            sql, prettyPrintMillis(currentTimeMillis() - start)));
  }

  public <T> Optional<T> queryForOptional(String sql, Class<T> requiredType, Object... args) {
    return Optional.ofNullable(jdbcTemplate.queryForObject(sql, requiredType, args));
  }

  public <T> Optional<T> queryForFirst(String sql, RowMapper<T> rowMapper, Object... args) {
    return jdbcTemplate.query(sql, rowMapper, args).stream().findFirst();
  }

}
