package fi.thl.termed.domain;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Multimap;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import fi.thl.termed.util.collect.ListUtils;
import fi.thl.termed.util.collect.MultimapUtils;

public class Class {

  private String id;

  private String uri;

  private Integer index;

  private Scheme scheme;

  private Multimap<String, Permission> permissions;

  private Multimap<String, LangValue> properties;

  private List<TextAttribute> textAttributes;

  private List<ReferenceAttribute> referenceAttributes;

  public Class(Scheme scheme, String id) {
    this.scheme = scheme;
    this.id = id;
  }

  public Class(Scheme scheme, String id, String uri) {
    this.scheme = scheme;
    this.id = id;
    this.uri = uri;
  }

  public Class(ClassId classId) {
    this.scheme = new Scheme(classId.getSchemeId());
    this.id = classId.getId();
  }

  public Class(Class cls) {
    this.id = cls.id;
    this.uri = cls.uri;
    this.index = cls.index;
    this.scheme = cls.scheme;
    this.permissions = cls.permissions;
    this.properties = cls.properties;
    this.textAttributes = cls.textAttributes;
    this.referenceAttributes = cls.referenceAttributes;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public Integer getIndex() {
    return index;
  }

  public void setIndex(Integer index) {
    this.index = index;
  }

  public Scheme getScheme() {
    return scheme;
  }

  public void setScheme(Scheme scheme) {
    this.scheme = scheme;
  }

  public UUID getSchemeId() {
    return scheme != null ? scheme.getId() : null;
  }

  public Multimap<String, Permission> getPermissions() {
    return MultimapUtils.nullToEmpty(permissions);
  }

  public void setPermissions(Multimap<String, Permission> permissions) {
    this.permissions = permissions;
  }

  public Multimap<String, LangValue> getProperties() {
    return MultimapUtils.nullToEmpty(properties);
  }

  public void setProperties(Multimap<String, LangValue> properties) {
    this.properties = properties;
  }

  public List<TextAttribute> getTextAttributes() {
    return ListUtils.nullToEmpty(textAttributes);
  }

  public void setTextAttributes(List<TextAttribute> textAttributes) {
    this.textAttributes = textAttributes;
  }

  public List<ReferenceAttribute> getReferenceAttributes() {
    return ListUtils.nullToEmpty(referenceAttributes);
  }

  public void setReferenceAttributes(List<ReferenceAttribute> referenceAttributes) {
    this.referenceAttributes = referenceAttributes;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .add("uri", uri)
        .add("index", index)
        .add("schemeId", getSchemeId())
        .add("permissions", permissions)
        .add("properties", properties)
        .add("textAttributes", textAttributes)
        .add("referenceAttributes", referenceAttributes)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Class cls = (Class) o;
    return Objects.equals(id, cls.id) &&
           Objects.equals(uri, cls.uri) &&
           Objects.equals(index, cls.index) &&
           Objects.equals(getSchemeId(), cls.getSchemeId()) &&
           Objects.equals(permissions, cls.permissions) &&
           Objects.equals(properties, cls.properties) &&
           Objects.equals(textAttributes, cls.textAttributes) &&
           Objects.equals(referenceAttributes, cls.referenceAttributes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, uri, index, getSchemeId(), permissions, properties, textAttributes,
                        referenceAttributes);
  }

}