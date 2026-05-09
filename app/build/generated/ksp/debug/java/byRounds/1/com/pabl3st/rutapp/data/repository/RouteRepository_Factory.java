package com.pabl3st.rutapp.data.repository;

import com.pabl3st.rutapp.data.local.dao.RouteDao;
import com.pabl3st.rutapp.data.local.dao.StopDao;
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao;
import com.pabl3st.rutapp.data.network.RutasApiService;
import com.pabl3st.rutapp.data.session.SessionManager;
import com.squareup.moshi.Moshi;
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
public final class RouteRepository_Factory implements Factory<RouteRepository> {
  private final Provider<RouteDao> routeDaoProvider;

  private final Provider<StopDao> stopDaoProvider;

  private final Provider<SyncQueueDao> syncQueueDaoProvider;

  private final Provider<RutasApiService> apiProvider;

  private final Provider<SessionManager> sessionProvider;

  private final Provider<Moshi> moshiProvider;

  public RouteRepository_Factory(Provider<RouteDao> routeDaoProvider,
      Provider<StopDao> stopDaoProvider, Provider<SyncQueueDao> syncQueueDaoProvider,
      Provider<RutasApiService> apiProvider, Provider<SessionManager> sessionProvider,
      Provider<Moshi> moshiProvider) {
    this.routeDaoProvider = routeDaoProvider;
    this.stopDaoProvider = stopDaoProvider;
    this.syncQueueDaoProvider = syncQueueDaoProvider;
    this.apiProvider = apiProvider;
    this.sessionProvider = sessionProvider;
    this.moshiProvider = moshiProvider;
  }

  @Override
  public RouteRepository get() {
    return newInstance(routeDaoProvider.get(), stopDaoProvider.get(), syncQueueDaoProvider.get(), apiProvider.get(), sessionProvider.get(), moshiProvider.get());
  }

  public static RouteRepository_Factory create(Provider<RouteDao> routeDaoProvider,
      Provider<StopDao> stopDaoProvider, Provider<SyncQueueDao> syncQueueDaoProvider,
      Provider<RutasApiService> apiProvider, Provider<SessionManager> sessionProvider,
      Provider<Moshi> moshiProvider) {
    return new RouteRepository_Factory(routeDaoProvider, stopDaoProvider, syncQueueDaoProvider, apiProvider, sessionProvider, moshiProvider);
  }

  public static RouteRepository newInstance(RouteDao routeDao, StopDao stopDao,
      SyncQueueDao syncQueueDao, RutasApiService api, SessionManager session, Moshi moshi) {
    return new RouteRepository(routeDao, stopDao, syncQueueDao, api, session, moshi);
  }
}
