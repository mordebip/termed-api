package fi.thl.termed.service.type;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import fi.thl.termed.domain.AppRole;
import fi.thl.termed.domain.GrantedPermission;
import fi.thl.termed.domain.LangValue;
import fi.thl.termed.domain.ObjectRolePermission;
import fi.thl.termed.domain.PropertyValueId;
import fi.thl.termed.domain.ReferenceAttribute;
import fi.thl.termed.domain.ReferenceAttributeId;
import fi.thl.termed.domain.TextAttribute;
import fi.thl.termed.domain.TextAttributeId;
import fi.thl.termed.domain.Type;
import fi.thl.termed.domain.TypeId;
import fi.thl.termed.service.type.internal.TypeRepository;
import fi.thl.termed.service.type.internal.InitializingTypeService;
import fi.thl.termed.service.type.internal.JdbcTypeDao;
import fi.thl.termed.service.type.internal.JdbcTypePermissionsDao;
import fi.thl.termed.service.type.internal.JdbcTypePropertyDao;
import fi.thl.termed.service.type.internal.JdbcReferenceAttributeDao;
import fi.thl.termed.service.type.internal.JdbcReferenceAttributePermissionsDao;
import fi.thl.termed.service.type.internal.JdbcReferenceAttributePropertyDao;
import fi.thl.termed.service.type.internal.JdbcTextAttributeDao;
import fi.thl.termed.service.type.internal.JdbcTextAttributePermissionsDao;
import fi.thl.termed.service.type.internal.JdbcTextAttributePropertyDao;
import fi.thl.termed.service.type.internal.ReferenceAttributeRepository;
import fi.thl.termed.service.type.internal.TextAttributeRepository;
import fi.thl.termed.util.dao.AuthorizedDao;
import fi.thl.termed.util.dao.CachedSystemDao;
import fi.thl.termed.util.dao.Dao;
import fi.thl.termed.util.dao.SystemDao;
import fi.thl.termed.util.permission.DaoPermissionEvaluator;
import fi.thl.termed.util.permission.DisjunctionPermissionEvaluator;
import fi.thl.termed.util.permission.PermissionEvaluator;
import fi.thl.termed.util.service.AbstractRepository;
import fi.thl.termed.util.service.LoggingService;
import fi.thl.termed.util.service.Service;
import fi.thl.termed.util.service.TransactionalService;

import static fi.thl.termed.util.dao.AuthorizedDao.ReportLevel.SILENT;

@Configuration
public class TypeServiceConfiguration {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private PlatformTransactionManager transactionManager;

  // permission system DAO instances are shared internally
  private SystemDao<ObjectRolePermission<TypeId>, GrantedPermission>
      typePermissionSystemDao;
  private SystemDao<ObjectRolePermission<TextAttributeId>, GrantedPermission>
      textAttributePermissionSystemDao;
  private SystemDao<ObjectRolePermission<ReferenceAttributeId>, GrantedPermission>
      referenceAttributePermissionSystemDao;

  @Bean
  public Service<TypeId, Type> typeService() {
    Service<TypeId, Type> service = typeRepository();

    service = new TransactionalService<>(service, transactionManager);
    service = new LoggingService<>(service, getClass().getPackage().getName() + ".Service");
    service = new InitializingTypeService(service);

    return service;
  }

  @Bean
  public PermissionEvaluator<TypeId> typeEvaluator() {
    return new DisjunctionPermissionEvaluator<>(
        appAdminEvaluator(), new DaoPermissionEvaluator<>(typePermissionSystemDao()));
  }

  @Bean
  public PermissionEvaluator<TextAttributeId> textAttributeEvaluator() {
    return new DisjunctionPermissionEvaluator<>(
        appAdminEvaluator(), new DaoPermissionEvaluator<>(textAttributePermissionSystemDao()));
  }

  @Bean
  public PermissionEvaluator<ReferenceAttributeId> referenceAttributeEvaluator() {
    return new DisjunctionPermissionEvaluator<>(
        appAdminEvaluator(), new DaoPermissionEvaluator<>(referenceAttributePermissionSystemDao()));
  }

  private AbstractRepository<TypeId, Type> typeRepository() {
    return new TypeRepository(
        typeDao(),
        typePermissionDao(),
        typePropertyDao(),
        textAttributeRepository(),
        referenceAttributeRepository());
  }

  private Dao<TypeId, Type> typeDao() {
    return new AuthorizedDao<>(typeSystemDao(), typeEvaluator());
  }

  private Dao<ObjectRolePermission<TypeId>, GrantedPermission> typePermissionDao() {
    return new AuthorizedDao<>(typePermissionSystemDao(), appAdminEvaluator(), SILENT);
  }

  private Dao<PropertyValueId<TypeId>, LangValue> typePropertyDao() {
    return new AuthorizedDao<>(typePropertySystemDao(), typePropertyEvaluator());
  }

  private PermissionEvaluator<PropertyValueId<TypeId>> typePropertyEvaluator() {
    return (u, o, p) -> typeEvaluator().hasPermission(u, o.getSubjectId(), p);
  }

  private SystemDao<TypeId, Type> typeSystemDao() {
    return new CachedSystemDao<>(new JdbcTypeDao(dataSource));
  }

  private SystemDao<ObjectRolePermission<TypeId>, GrantedPermission> typePermissionSystemDao() {
    if (typePermissionSystemDao == null) {
      typePermissionSystemDao = new CachedSystemDao<>(new JdbcTypePermissionsDao(dataSource));
    }
    return typePermissionSystemDao;
  }

  private SystemDao<PropertyValueId<TypeId>, LangValue> typePropertySystemDao() {
    return new CachedSystemDao<>(new JdbcTypePropertyDao(dataSource));
  }

  // text attributes

  private AbstractRepository<TextAttributeId, TextAttribute> textAttributeRepository() {
    return new TextAttributeRepository(
        textAttributeDao(),
        textAttributePermissionDao(),
        textAttributePropertyDao());
  }

  private Dao<TextAttributeId, TextAttribute> textAttributeDao() {
    return new AuthorizedDao<>(textAttributeSystemDao(), textAttributeEvaluator(), SILENT);
  }

  private Dao<ObjectRolePermission<TextAttributeId>, GrantedPermission> textAttributePermissionDao() {
    return new AuthorizedDao<>(textAttributePermissionSystemDao(), appAdminEvaluator(), SILENT);
  }

  private Dao<PropertyValueId<TextAttributeId>, LangValue> textAttributePropertyDao() {
    return new AuthorizedDao<>(textAttributePropertySystemDao(), textAttributePropertyEvaluator());
  }


  private PermissionEvaluator<PropertyValueId<TextAttributeId>> textAttributePropertyEvaluator() {
    return (u, o, p) -> textAttributeEvaluator().hasPermission(u, o.getSubjectId(), p);
  }

  private SystemDao<TextAttributeId, TextAttribute> textAttributeSystemDao() {
    return new CachedSystemDao<>(new JdbcTextAttributeDao(dataSource));
  }

  private SystemDao<ObjectRolePermission<TextAttributeId>, GrantedPermission> textAttributePermissionSystemDao() {
    if (textAttributePermissionSystemDao == null) {
      textAttributePermissionSystemDao =
          new CachedSystemDao<>(new JdbcTextAttributePermissionsDao(dataSource));
    }
    return textAttributePermissionSystemDao;
  }

  private SystemDao<PropertyValueId<TextAttributeId>, LangValue> textAttributePropertySystemDao() {
    return new CachedSystemDao<>(new JdbcTextAttributePropertyDao(dataSource));
  }

  // reference attributes

  private AbstractRepository<ReferenceAttributeId, ReferenceAttribute> referenceAttributeRepository() {
    return new ReferenceAttributeRepository(
        referenceAttributeDao(),
        referenceAttributePermissionDao(),
        referenceAttributePropertyDao());
  }

  private Dao<ReferenceAttributeId, ReferenceAttribute> referenceAttributeDao() {
    return new AuthorizedDao<>(referenceAttributeSystemDao(),
                               referenceAttributeEvaluator(), SILENT);
  }

  private Dao<ObjectRolePermission<ReferenceAttributeId>, GrantedPermission> referenceAttributePermissionDao() {
    return new AuthorizedDao<>(referenceAttributePermissionSystemDao(),
                               appAdminEvaluator(), SILENT);
  }

  private Dao<PropertyValueId<ReferenceAttributeId>, LangValue> referenceAttributePropertyDao() {
    return new AuthorizedDao<>(referenceAttributePropertySystemDao(),
                               referenceAttributePropertyEvaluator());
  }

  private PermissionEvaluator<PropertyValueId<ReferenceAttributeId>> referenceAttributePropertyEvaluator() {
    return (u, o, p) -> referenceAttributeEvaluator().hasPermission(u, o.getSubjectId(), p);
  }

  private SystemDao<ReferenceAttributeId, ReferenceAttribute> referenceAttributeSystemDao() {
    return new CachedSystemDao<>(new JdbcReferenceAttributeDao(dataSource));
  }

  private SystemDao<ObjectRolePermission<ReferenceAttributeId>, GrantedPermission> referenceAttributePermissionSystemDao() {
    if (referenceAttributePermissionSystemDao == null) {
      referenceAttributePermissionSystemDao =
          new CachedSystemDao<>(new JdbcReferenceAttributePermissionsDao(dataSource));
    }
    return referenceAttributePermissionSystemDao;
  }

  private SystemDao<PropertyValueId<ReferenceAttributeId>, LangValue> referenceAttributePropertySystemDao() {
    return new CachedSystemDao<>(new JdbcReferenceAttributePropertyDao(dataSource));
  }

  /**
   * Creates type specific permission evaluator that accepts users that are admins or superusers
   */
  private <T> PermissionEvaluator<T> appAdminEvaluator() {
    return (user, object, permission) -> user.getAppRole() == AppRole.ADMIN ||
                                         user.getAppRole() == AppRole.SUPERUSER;
  }

}