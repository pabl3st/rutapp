package com.pabl3st.rutapp.feature.rutas;

import com.pabl3st.rutapp.data.repository.RouteRepository;
import com.pabl3st.rutapp.data.session.SessionManager;
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
public final class RutasViewModel_Factory implements Factory<RutasViewModel> {
  private final Provider<RouteRepository> routeRepoProvider;

  private final Provider<SessionManager> sessionProvider;

  public RutasViewModel_Factory(Provider<RouteRepository> routeRepoProvider,
      Provider<SessionManager> sessionProvider) {
    this.routeRepoProvider = routeRepoProvider;
    this.sessionProvider = sessionProvider;
  }

  @Override
  public RutasViewModel get() {
    return newInstance(routeRepoProvider.get(), sessionProvider.get());
  }

  public static RutasViewModel_Factory create(Provider<RouteRepository> routeRepoProvider,
      Provider<SessionManager> sessionProvider) {
    return new RutasViewModel_Factory(routeRepoProvider, sessionProvider);
  }

  public static RutasViewModel newInstance(RouteRepository routeRepo, SessionManager session) {
    return new RutasViewModel(routeRepo, session);
  }
}
