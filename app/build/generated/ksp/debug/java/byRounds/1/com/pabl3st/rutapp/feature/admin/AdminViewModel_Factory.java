package com.pabl3st.rutapp.feature.admin;

import com.pabl3st.rutapp.data.repository.AdminRepository;
import com.pabl3st.rutapp.data.repository.RouteRepository;
import com.pabl3st.rutapp.data.repository.StopRepository;
import com.pabl3st.rutapp.data.repository.SyncRepository;
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
public final class AdminViewModel_Factory implements Factory<AdminViewModel> {
  private final Provider<SessionManager> sessionProvider;

  private final Provider<SyncRepository> syncRepoProvider;

  private final Provider<RouteRepository> routeRepoProvider;

  private final Provider<StopRepository> stopRepoProvider;

  private final Provider<AdminRepository> adminRepoProvider;

  public AdminViewModel_Factory(Provider<SessionManager> sessionProvider,
      Provider<SyncRepository> syncRepoProvider, Provider<RouteRepository> routeRepoProvider,
      Provider<StopRepository> stopRepoProvider, Provider<AdminRepository> adminRepoProvider) {
    this.sessionProvider = sessionProvider;
    this.syncRepoProvider = syncRepoProvider;
    this.routeRepoProvider = routeRepoProvider;
    this.stopRepoProvider = stopRepoProvider;
    this.adminRepoProvider = adminRepoProvider;
  }

  @Override
  public AdminViewModel get() {
    return newInstance(sessionProvider.get(), syncRepoProvider.get(), routeRepoProvider.get(), stopRepoProvider.get(), adminRepoProvider.get());
  }

  public static AdminViewModel_Factory create(Provider<SessionManager> sessionProvider,
      Provider<SyncRepository> syncRepoProvider, Provider<RouteRepository> routeRepoProvider,
      Provider<StopRepository> stopRepoProvider, Provider<AdminRepository> adminRepoProvider) {
    return new AdminViewModel_Factory(sessionProvider, syncRepoProvider, routeRepoProvider, stopRepoProvider, adminRepoProvider);
  }

  public static AdminViewModel newInstance(SessionManager session, SyncRepository syncRepo,
      RouteRepository routeRepo, StopRepository stopRepo, AdminRepository adminRepo) {
    return new AdminViewModel(session, syncRepo, routeRepo, stopRepo, adminRepo);
  }
}
