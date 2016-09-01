package fi.thl.termed.service.scheme;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import fi.thl.termed.dao.Dao;
import fi.thl.termed.domain.Resource;
import fi.thl.termed.domain.ResourceId;
import fi.thl.termed.domain.Scheme;
import fi.thl.termed.domain.User;
import fi.thl.termed.index.Index;
import fi.thl.termed.repository.Repository;
import fi.thl.termed.service.Service;
import fi.thl.termed.service.common.ForwardingService;
import fi.thl.termed.spesification.sql.ResourcesBySchemeId;

/**
 * Indexes scheme resources after scheme modifications.
 */
public class IndexingSchemeService extends ForwardingService<UUID, Scheme> {

  private Repository<ResourceId, Resource> resourceRepository;
  private Index<ResourceId, Resource> resourceIndex;
  private Dao<ResourceId, Resource> resourceDao;

  public IndexingSchemeService(Service<UUID, Scheme> delegate,
                               Repository<ResourceId, Resource> resourceRepository,
                               Index<ResourceId, Resource> resourceIndex,
                               Dao<ResourceId, Resource> resourceDao) {
    super(delegate);
    this.resourceRepository = resourceRepository;
    this.resourceIndex = resourceIndex;
    this.resourceDao = resourceDao;
  }

  @Override
  public void save(List<Scheme> schemes, User currentUser) {
    Set<ResourceId> oldResourceIds = Sets.newHashSet();
    for (Scheme scheme : schemes) {
      oldResourceIds.addAll(schemeResourceIds(scheme.getId()));
    }

    super.save(schemes, currentUser);

    Set<ResourceId> newResourceIds = Sets.newHashSet();
    for (Scheme scheme : schemes) {
      newResourceIds.addAll(schemeResourceIds(scheme.getId()));
    }

    deleteFromIndex(Sets.difference(oldResourceIds, newResourceIds));
    reindex(newResourceIds);
  }

  @Override
  public void save(Scheme scheme, User currentUser) {
    Set<ResourceId> oldResourceIds = schemeResourceIds(scheme.getId());
    super.save(scheme, currentUser);
    Set<ResourceId> newResourceIds = schemeResourceIds(scheme.getId());

    deleteFromIndex(Sets.difference(oldResourceIds, newResourceIds));
    reindex(newResourceIds);
  }

  @Override
  public void delete(UUID schemeId, User currentUser) {
    Set<ResourceId> oldResourceIds = schemeResourceIds(schemeId);
    super.delete(schemeId, currentUser);
    deleteFromIndex(oldResourceIds);
  }

  private Set<ResourceId> schemeResourceIds(UUID schemeId) {
    return ImmutableSet.copyOf(resourceDao.getKeys(new ResourcesBySchemeId(schemeId)));
  }

  private void deleteFromIndex(Set<ResourceId> ids) {
    for (ResourceId id : ids) {
      resourceIndex.deleteFromIndex(id);
    }
  }

  private void reindex(Set<ResourceId> ids) {
    if (!ids.isEmpty()) {
      resourceIndex.reindex(ImmutableList.copyOf(ids), new Function<ResourceId, Resource>() {
        public Resource apply(ResourceId id) {
          return resourceRepository.get(id);
        }
      });
    }
  }

}