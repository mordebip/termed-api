package fi.thl.termed.spesification.sql;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.util.UUID;

import fi.thl.termed.domain.ObjectRolePermission;
import fi.thl.termed.spesification.SqlSpecification;
import fi.thl.termed.spesification.AbstractSpecification;

public class SchemePermissionsBySchemeId
    extends AbstractSpecification<ObjectRolePermission<UUID>, Void>
    implements SqlSpecification<ObjectRolePermission<UUID>, Void> {

  private UUID schemeId;

  public SchemePermissionsBySchemeId(UUID schemeId) {
    this.schemeId = schemeId;
  }

  @Override
  public boolean accept(ObjectRolePermission<UUID> objectRolePermission, Void value) {
    return Objects.equal(objectRolePermission.getObjectId(), schemeId);
  }

  @Override
  public String sqlQueryTemplate() {
    return "scheme_id = ?";
  }

  @Override
  public Object[] sqlQueryParameters() {
    return new Object[]{schemeId};
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SchemePermissionsBySchemeId that = (SchemePermissionsBySchemeId) o;
    return Objects.equal(schemeId, that.schemeId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(schemeId);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("schemeId", schemeId)
        .toString();
  }

}
