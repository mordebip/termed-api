package fi.thl.termed.repository.spesification;

import com.google.common.base.Objects;

import java.util.UUID;

import fi.thl.termed.domain.Resource;
import fi.thl.termed.domain.ResourceId;

public class ResourceSpecificationBySchemeAndTypeId extends SqlSpecification<ResourceId, Resource> {

  private UUID schemeId;
  private String typeId;

  public ResourceSpecificationBySchemeAndTypeId(UUID schemeId, String typeId) {
    this.schemeId = schemeId;
    this.typeId = typeId;
  }

  @Override
  public boolean accept(ResourceId resourceId, Resource resource) {
    return Objects.equal(resourceId.getSchemeId(), schemeId) &&
           Objects.equal(resourceId.getTypeId(), typeId);
  }

  @Override
  public String sqlQueryTemplate() {
    return "scheme_id = ? and type_id = ?";
  }

  @Override
  public Object[] sqlQueryParameters() {
    return new Object[]{schemeId, typeId};
  }

}
