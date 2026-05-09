package com.pabl3st.rutapp.data.repository;

import com.pabl3st.rutapp.data.local.dao.BusinessProfileDao;
import com.pabl3st.rutapp.data.local.dao.KpiDefinitionDao;
import com.pabl3st.rutapp.data.session.SessionManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class BusinessProfileRepository_Factory implements Factory<BusinessProfileRepository> {
  private final Provider<BusinessProfileDao> profileDaoProvider;

  private final Provider<KpiDefinitionDao> kpiDaoProvider;

  private final Provider<SessionManager> sessionProvider;

  public BusinessProfileRepository_Factory(Provider<BusinessProfileDao> profileDaoProvider,
      Provider<KpiDefinitionDao> kpiDaoProvider, Provider<SessionManager> sessionProvider) {
    this.profileDaoProvider = profileDaoProvider;
    this.kpiDaoProvider = kpiDaoProvider;
    this.sessionProvider = sessionProvider;
  }

  @Override
  public BusinessProfileRepository get() {
    return newInstance(profileDaoProvider.get(), kpiDaoProvider.get(), sessionProvider.get());
  }

  public static BusinessProfileRepository_Factory create(
      Provider<BusinessProfileDao> profileDaoProvider, Provider<KpiDefinitionDao> kpiDaoProvider,
      Provider<SessionManager> sessionProvider) {
    return new BusinessProfileRepository_Factory(profileDaoProvider, kpiDaoProvider, sessionProvider);
  }

  public static BusinessProfileRepository newInstance(BusinessProfileDao profileDao,
      KpiDefinitionDao kpiDao, SessionManager session) {
    return new BusinessProfileRepository(profileDao, kpiDao, session);
  }
}
