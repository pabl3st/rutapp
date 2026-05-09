package com.pabl3st.rutapp.feature.home;

import androidx.work.WorkManager;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<RouteRepository> routeRepoProvider;

  private final Provider<StopRepository> stopRepoProvider;

  private final Provider<SyncRepository> syncRepoProvider;

  private final Provider<SessionManager> sessionProvider;

  private final Provider<WorkManager> workManagerProvider;

  public HomeViewModel_Factory(Provider<RouteRepository> routeRepoProvider,
      Provider<StopRepository> stopRepoProvider, Provider<SyncRepository> syncRepoProvider,
      Provider<SessionManager> sessionProvider, Provider<WorkManager> workManagerProvider) {
    this.routeRepoProvider = routeRepoProvider;
    this.stopRepoProvider = stopRepoProvider;
    this.syncRepoProvider = syncRepoProvider;
    this.sessionProvider = sessionProvider;
    this.workManagerProvider = workManagerProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(routeRepoProvider.get(), stopRepoProvider.get(), syncRepoProvider.get(), sessionProvider.get(), workManagerProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<RouteRepository> routeRepoProvider,
      Provider<StopRepository> stopRepoProvider, Provider<SyncRepository> syncRepoProvider,
      Provider<SessionManager> sessionProvider, Provider<WorkManager> workManagerProvider) {
    return new HomeViewModel_Factory(routeRepoProvider, stopRepoProvider, syncRepoProvider, sessionProvider, workManagerProvider);
  }

  public static HomeViewModel newInstance(RouteRepository routeRepo, StopRepository stopRepo,
      SyncRepository syncRepo, SessionManager session, WorkManager workManager) {
    return new HomeViewModel(routeRepo, stopRepo, syncRepo, session, workManager);
  }
}
