package fi.thl.termed.util.service;

import fi.thl.termed.domain.User;
import fi.thl.termed.util.query.Query;
import fi.thl.termed.util.query.Select;
import fi.thl.termed.util.query.Specification;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Adds "multiple readers/single-writer" synchronization to a service.
 */
public class RwSynchronizedService<K extends Serializable, V> implements Service<K, V> {

  private Service<K, V> delegate;

  private Lock readLock;
  private Lock writeLock;

  public RwSynchronizedService(Service<K, V> delegate) {
    this.delegate = delegate;
    ReadWriteLock l = new ReentrantReadWriteLock();
    this.readLock = l.readLock();
    this.writeLock = l.writeLock();
  }

  @Override
  public List<K> save(List<V> values, SaveMode mode, WriteOptions opts, User user) {
    return writeLocked(() -> delegate.save(values, mode, opts, user));
  }

  @Override
  public K save(V value, SaveMode mode, WriteOptions opts, User user) {
    return writeLocked(() -> delegate.save(value, mode, opts, user));
  }

  @Override
  public void delete(List<K> ids, WriteOptions opts, User user) {
    writeLocked(() -> {
      delegate.delete(ids, opts, user);
      return null;
    });
  }

  @Override
  public void delete(K id, WriteOptions opts, User user) {
    writeLocked(() -> {
      delegate.delete(id, opts, user);
      return null;
    });
  }

  @Override
  public List<K> deleteAndSave(List<K> deletes, List<V> saves, SaveMode mode, WriteOptions opts,
      User user) {
    return writeLocked(() -> delegate.deleteAndSave(deletes, saves, mode, opts, user));
  }

  @Override
  public Stream<V> getValues(User user) {
    return readLocked(() -> delegate.getValues(user));
  }

  @Override
  public Stream<V> getValues(Specification<K, V> spec, User user) {
    return readLocked(() -> delegate.getValues(spec, user));
  }

  @Override
  public Stream<V> getValues(Query<K, V> query, User user) {
    return readLocked(() -> delegate.getValues(query, user));
  }

  @Override
  public Stream<K> getKeys(User user) {
    return readLocked(() -> delegate.getKeys(user));
  }

  @Override
  public Stream<K> getKeys(Specification<K, V> spec, User user) {
    return readLocked(() -> delegate.getKeys(spec, user));
  }

  @Override
  public Stream<K> getKeys(Query<K, V> query, User user) {
    return readLocked(() -> delegate.getKeys(query, user));
  }

  @Override
  public long count(Specification<K, V> spec, User user) {
    return readLocked(() -> delegate.count(spec, user));
  }

  @Override
  public boolean exists(K key, User user) {
    return readLocked(() -> delegate.exists(key, user));
  }

  @Override
  public Optional<V> get(K id, User user, Select... selects) {
    return readLocked(() -> delegate.get(id, user, selects));
  }

  private <E> E writeLocked(Supplier<E> supplier) {
    writeLock.lock();
    try {
      return supplier.get();
    } finally {
      writeLock.unlock();
    }
  }

  private <E> E readLocked(Supplier<E> supplier) {
    readLock.lock();
    try {
      return supplier.get();
    } finally {
      readLock.unlock();
    }
  }

}
