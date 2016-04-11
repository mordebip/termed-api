package fi.thl.termed.repository;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.UUID;

import fi.thl.termed.Application;
import fi.thl.termed.domain.Scheme;
import fi.thl.termed.util.LangValue;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@IntegrationTest
public class SchemeRepositoryTest {

  @Autowired
  private SchemeRepository schemeRepository;

  @Test
  public void shouldSaveSchemeWithProperties() {
    Scheme scheme = new Scheme(UUID.randomUUID());
    scheme.setProperties(ImmutableMultimap.of(
        "prefLabel", new LangValue("en", "Scheme label 0"),
        "prefLabel", new LangValue("en", "Scheme label 1"),
        "prefLabel", new LangValue("en", "Scheme label 2")));

    schemeRepository.save(scheme.getId(), scheme);

    Scheme savedScheme = schemeRepository.get(scheme.getId());

    assertEquals(scheme.getId(), savedScheme.getId());

    List<LangValue> langValues = Lists.newArrayList(savedScheme.getProperties().get("prefLabel"));
    assertEquals(new LangValue("en", "Scheme label 0"), langValues.get(0));
    assertEquals(new LangValue("en", "Scheme label 1"), langValues.get(1));
    assertEquals(new LangValue("en", "Scheme label 2"), langValues.get(2));
  }

  @Test
  public void shouldUpdateSchemeWithProperties() {
    Scheme scheme = new Scheme(UUID.randomUUID());
    scheme.setProperties(ImmutableMultimap.of(
        "prefLabel", new LangValue("en", "Scheme label 0"),
        "prefLabel", new LangValue("en", "Scheme label 1"),
        "prefLabel", new LangValue("en", "Scheme label 2")));

    schemeRepository.save(scheme.getId(), scheme);

    scheme.setProperties(ImmutableMultimap.of(
        "prefLabel", new LangValue("en", "Scheme label 0 updated"),
        "prefLabel", new LangValue("en", "Scheme label 2")));

    schemeRepository.save(scheme.getId(), scheme);

    Scheme updated = schemeRepository.get(scheme.getId());

    List<LangValue> langValues = Lists.newArrayList(updated.getProperties().get("prefLabel"));
    assertEquals(2, langValues.size());
    assertEquals(new LangValue("en", "Scheme label 0 updated"), langValues.get(0));
    assertEquals(new LangValue("en", "Scheme label 2"), langValues.get(1));
  }

}
