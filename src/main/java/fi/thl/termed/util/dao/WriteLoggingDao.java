package fi.thl.termed.util.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import fi.thl.termed.domain.User;

public class WriteLoggingDao<K extends Serializable, V> extends ForwardingDao<K, V> {

  private Logger log;

  public WriteLoggingDao(Dao<K, V> delegate, String loggerName) {
    super(delegate);
    this.log = LoggerFactory.getLogger(loggerName);
  }

  public WriteLoggingDao(Dao<K, V> delegate, Class<?> loggerName) {
    super(delegate);
    this.log = LoggerFactory.getLogger(loggerName);
  }

  @Override
  public void insert(Map<K, V> map, User user) {
    log.info("insert {} (user: {})", map.keySet(), user.getUsername());
    super.insert(map, user);
  }

  @Override
  public void insert(K key, V value, User user) {
    log.info("insert {} (user: {})", key, user.getUsername());
    super.insert(key, value, user);
  }

  @Override
  public void update(Map<K, V> map, User user) {
    log.info("update {} (user: {})", map.keySet(), user.getUsername());
    super.update(map, user);
  }

  @Override
  public void update(K key, V value, User user) {
    log.info("update {} (user: {})", key, user.getUsername());
    super.update(key, value, user);
  }

  @Override
  public void delete(List<K> keys, User user) {
    log.info("delete {} keys (user: {})", keys, user.getUsername());
    super.delete(keys, user);
  }

  @Override
  public void delete(K key, User user) {
    log.info("delete {} (user: {})", key, user.getUsername());
    super.delete(key, user);
  }

}
