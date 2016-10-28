package fi.thl.termed.service.user.internal;

import java.util.Objects;

import fi.thl.termed.domain.Empty;
import fi.thl.termed.domain.UserSchemeRole;
import fi.thl.termed.util.specification.AbstractSqlSpecification;

public class UserSchemeRolesByUsername extends AbstractSqlSpecification<UserSchemeRole, Empty> {

  private String username;

  public UserSchemeRolesByUsername(String username) {
    this.username = username;
  }

  @Override
  public boolean test(UserSchemeRole id, Empty value) {
    return Objects.equals(id.getUsername(), username);
  }

  @Override
  public String sqlQueryTemplate() {
    return "username = ?";
  }

  @Override
  public Object[] sqlQueryParameters() {
    return new Object[]{username};
  }

}