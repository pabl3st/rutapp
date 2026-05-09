package com.pabl3st.rutapp.feature.kpis;

import com.pabl3st.rutapp.data.local.dao.KpiValueDao;
import com.pabl3st.rutapp.data.repository.BusinessProfileRepository;
import com.pabl3st.rutapp.data.repository.RouteRepository;
import com.pabl3st.rutapp.data.repository.StopRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class KpisViewModel_Factory implements Factory<KpisViewModel> {
  private final Provider<RouteRepository> routeRepoProvider;

  private final Provider<StopRepository> stopRepoProvider;

  private final Provider<KpiValueDao> kpiValueDaoProvider;

  private final Provider<BusinessProfileRepository> profileRepoProvider;

  public KpisViewModel_Factory(Provider<RouteRepository> routeRepoProvider,
      Provider<StopRepository> stopRepoProvider, Provider<KpiValueDao> kpiValueDaoProvider,
      Provider<BusinessProfileRepository> profileRepoProvider) {
    this.routeRepoProvider = routeRepoProvider;
    this.stopRepoProvider = stopRepoProvider;
    this.kpiValueDaoProvider = kpiValueDaoProvider;
    this.profileRepoProvider = profileRepoProvider;
  }

  @Override
  public KpisViewModel get() {
    return newInstance(routeRepoProvider.get(), stopRepoProvider.get(), kpiValueDaoProvider.get(), profileRepoProvider.get());
  }

  public static KpisViewModel_Factory create(Provider<RouteRepository> routeRepoProvider,
      Provider<StopRepository> stopRepoProvider, Provider<KpiValueDao> kpiValueDaoProvider,
      Provider<BusinessProfileRepository> profileRepoProvider) {
    return new KpisViewModel_Factory(routeRepoProvider, stopRepoProvider, kpiValueDaoProvider, profileRepoProvider);
  }

  public static KpisViewModel newInstance(RouteRepository routeRepo, StopRepository stopRepo,
      KpiValueDao kpiValueDao, BusinessProfileRepository profileRepo) {
    return new KpisViewModel(routeRepo, stopRepo, kpiValueDao, profileRepo);
  }
}
