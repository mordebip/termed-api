package fi.thl.termed.repository.transform;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import fi.thl.termed.domain.Empty;
import fi.thl.termed.domain.SchemeRole;

public class SchemeRoleModelToDto implements Function<Map<SchemeRole, Empty>, List<String>> {

  public static SchemeRoleModelToDto create() {
    return new SchemeRoleModelToDto();
  }

  @Override
  public List<String> apply(Map<SchemeRole, Empty> map) {
    List<String> roles = Lists.newArrayList();
    for (SchemeRole schemeRole : map.keySet()) {
      roles.add(schemeRole.getRole());
    }
    return roles;
  }

}