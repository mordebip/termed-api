package fi.thl.termed.web;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import fi.thl.termed.domain.Class;
import fi.thl.termed.domain.ReferenceAttribute;
import fi.thl.termed.domain.Scheme;
import fi.thl.termed.domain.TextAttribute;
import fi.thl.termed.domain.User;
import fi.thl.termed.service.SchemeService;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * SchemeService published as a JSON/REST service.
 */
@RestController
@RequestMapping(value = "/api", produces = "application/json;charset=UTF-8")
public class SchemeController {

  @Autowired
  private SchemeService schemeService;

  @RequestMapping(method = GET, value = "/schemes")
  public List<Scheme> get(@AuthenticationPrincipal User user) {
    return schemeService.get(user);
  }

  @RequestMapping(method = GET, value = "/schemes/{schemeId}")
  public Scheme get(@PathVariable("schemeId") UUID schemeId,
                    @AuthenticationPrincipal User user) {
    return schemeService.get(schemeId, user);
  }

  @RequestMapping(method = GET, value = "/schemes/{schemeId}/classes/{classId}")
  public Class getClass(@PathVariable("schemeId") UUID schemeId,
                        @PathVariable("classId") String classId,
                        @AuthenticationPrincipal User user) {
    Scheme scheme = schemeService.get(schemeId, user);
    return Iterables.find(scheme.getClasses(), new ClassIdMatches(classId));
  }

  @RequestMapping(method = GET, value = "/schemes/{schemeId}/classes/{classId}/textAttributes")
  public List<TextAttribute> getTextAttributes(@PathVariable("schemeId") UUID schemeId,
                                               @PathVariable("classId") String classId,
                                               @AuthenticationPrincipal User user) {
    Scheme scheme = schemeService.get(schemeId, user);
    return Iterables.find(scheme.getClasses(), new ClassIdMatches(classId)).getTextAttributes();
  }

  @RequestMapping(method = GET, value = "/schemes/{schemeId}/classes/{classId}/referenceAttributes")
  public List<ReferenceAttribute> getReferenceAttributes(@PathVariable("schemeId") UUID schemeId,
                                                         @PathVariable("classId") String classId,
                                                         @AuthenticationPrincipal User user) {
    Scheme scheme = schemeService.get(schemeId, user);
    return Iterables.find(scheme.getClasses(), new ClassIdMatches(classId))
        .getReferenceAttributes();
  }

  @RequestMapping(method = POST, value = "/schemes",
      consumes = "application/json;charset=UTF-8")
  public Scheme save(@RequestBody Scheme scheme,
                     @AuthenticationPrincipal User user) {
    return schemeService.save(scheme, user);
  }

  @RequestMapping(method = PUT, value = "/schemes/{schemeId}",
      consumes = "application/json;charset=UTF-8")
  public Scheme save(@PathVariable("schemeId") UUID schemeId,
                     @RequestBody Scheme scheme,
                     @AuthenticationPrincipal User user) {
    scheme.setId(schemeId);
    return schemeService.save(scheme, user);
  }

  @RequestMapping(method = DELETE, value = "/schemes/{schemeId}")
  public void delete(@PathVariable("schemeId") UUID schemeId,
                     @AuthenticationPrincipal User user) {
    schemeService.delete(schemeId, user);
  }

  private class ClassIdMatches implements Predicate<Class> {

    private String id;

    public ClassIdMatches(String id) {
      this.id = id;
    }

    @Override
    public boolean apply(Class cls) {
      return Objects.equal(cls.getId(), id);
    }

  }

}
