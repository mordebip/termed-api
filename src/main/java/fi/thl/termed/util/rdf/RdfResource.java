package fi.thl.termed.util.rdf;

import com.google.common.base.MoreObjects;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;

import java.util.Set;

import fi.thl.termed.domain.LangValue;

/**
 * Class to represent RDF resource with literal and object properties. Object property values are
 * URIs.
 */
public class RdfResource {

  private String uri;

  private SetMultimap<String, LangValue> literals;

  private SetMultimap<String, String> objects;

  public RdfResource(String uri) {
    this.uri = uri;
    this.literals = LinkedHashMultimap.create();
    this.objects = LinkedHashMultimap.create();
  }

  public String getUri() {
    return uri;
  }

  public SetMultimap<String, LangValue> getLiterals() {
    return literals;
  }

  public Set<LangValue> getLiterals(String predicateUri) {
    return literals.get(predicateUri);
  }

  public void addLiteral(String predicateURI, String lang, String value) {
    literals.put(predicateURI, new LangValue(lang, value));
  }

  public SetMultimap<String, String> getObjects() {
    return objects;
  }

  public Set<String> getObjects(String predicateUri) {
    return objects.get(predicateUri);
  }

  public void addObject(String predicateUri, String objectUri) {
    objects.put(predicateUri, objectUri);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("uri", uri)
        .add("literals", literals)
        .add("objects", objects)
        .toString();
  }

}
