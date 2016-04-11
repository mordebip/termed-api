package fi.thl.termed.repository.spesification;

import com.google.common.base.Objects;

import fi.thl.termed.domain.ClassId;
import fi.thl.termed.domain.PropertyValueId;
import fi.thl.termed.util.LangValue;

public class ClassPropertyValueSpecificationBySubjectId
    extends SqlSpecification<PropertyValueId<ClassId>, LangValue> {

  private ClassId classId;

  public ClassPropertyValueSpecificationBySubjectId(ClassId classId) {
    this.classId = classId;
  }

  @Override
  public boolean accept(PropertyValueId<ClassId> key, LangValue value) {
    return Objects.equal(key.getSubjectId(), classId);
  }

  @Override
  public String sqlQueryTemplate() {
    return "class_scheme_id = ? and class_id = ?";
  }

  @Override
  public Object[] sqlQueryParameters() {
    return new Object[]{classId.getSchemeId(), classId.getId()};
  }

}
