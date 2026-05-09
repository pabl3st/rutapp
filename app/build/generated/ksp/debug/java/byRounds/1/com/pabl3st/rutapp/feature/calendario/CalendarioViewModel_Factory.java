package com.pabl3st.rutapp.feature.calendario;

import com.pabl3st.rutapp.data.repository.RouteRepository;
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
public final class CalendarioViewModel_Factory implements Factory<CalendarioViewModel> {
  private final Provider<RouteRepository> routeRepoProvider;

  public CalendarioViewModel_Factory(Provider<RouteRepository> routeRepoProvider) {
    this.routeRepoProvider = routeRepoProvider;
  }

  @Override
  public CalendarioViewModel get() {
    return newInstance(routeRepoProvider.get());
  }

  public static CalendarioViewModel_Factory create(Provider<RouteRepository> routeRepoProvider) {
    return new CalendarioViewModel_Factory(routeRepoProvider);
  }

  public static CalendarioViewModel newInstance(RouteRepository routeRepo) {
    return new CalendarioViewModel(routeRepo);
  }
}
