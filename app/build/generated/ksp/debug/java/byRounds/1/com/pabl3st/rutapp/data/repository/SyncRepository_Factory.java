package com.pabl3st.rutapp.data.repository;

import com.pabl3st.rutapp.data.local.dao.BusinessProfileDao;
import com.pabl3st.rutapp.data.local.dao.DaySessionDao;
import com.pabl3st.rutapp.data.local.dao.KpiValueDao;
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
public final class SyncRepository_Factory implements Factory<SyncRepository> {
  private final Provider<SyncQueueDao> syncQueueDaoProvider;

  private final Provider<RouteDao> routeDaoProvider;

  private final Provider<StopDao> stopDaoProvider;

  private final Provider<DaySessionDao> daySessionDaoProvider;

  private final Provider<KpiValueDao> kpiValueDaoProvider;

  private final Provider<BusinessProfileDao> businessProfileDaoProvider;

  private final Provider<RutasApiService> apiProvider;

  private final Provider<SessionManager> sessionProvider;

  private final Provider<Moshi> moshiProvider;

  public SyncRepository_Factory(Provider<SyncQueueDao> syncQueueDaoProvider,
      Provider<RouteDao> routeDaoProvider, Provider<StopDao> stopDaoProvider,
      Provider<DaySessionDao> daySessionDaoProvider, Provider<KpiValueDao> kpiValueDaoProvider,
      Provider<BusinessProfileDao> businessProfileDaoProvider,
      Provider<RutasApiService> apiProvider, Provider<SessionManager> sessionProvider,
      Provider<Moshi> moshiProvider) {
    this.syncQueueDaoProvider = syncQueueDaoProvider;
    this.routeDaoProvider = routeDaoProvider;
    this.stopDaoProvider = stopDaoProvider;
    this.daySessionDaoProvider = daySessionDaoProvider;
    this.kpiValueDaoProvider = kpiValueDaoProvider;
    this.businessProfileDaoProvider = businessProfileDaoProvider;
    this.apiProvider = apiProvider;
    this.sessionProvider = sessionProvider;
    this.moshiProvider = moshiProvider;
  }

  @Override
  public SyncRepository get() {
    return newInstance(syncQueueDaoProvider.get(), routeDaoProvider.get(), stopDaoProvider.get(), daySessionDaoProvider.get(), kpiValueDaoProvider.get(), businessProfileDaoProvider.get(), apiProvider.get(), sessionProvider.get(), moshiProvider.get());
  }

  public static SyncRepository_Factory create(Provider<SyncQueueDao> syncQueueDaoProvider,
      Provider<RouteDao> routeDaoProvider, Provider<StopDao> stopDaoProvider,
      Provider<DaySessionDao> daySessionDaoProvider, Provider<KpiValueDao> kpiValueDaoProvider,
      Provider<BusinessProfileDao> businessProfileDaoProvider,
      Provider<RutasApiService> apiProvider, Provider<SessionManager> sessionProvider,
      Provider<Moshi> moshiProvider) {
    return new SyncRepository_Factory(syncQueueDaoProvider, routeDaoProvider, stopDaoProvider, daySessionDaoProvider, kpiValueDaoProvider, businessProfileDaoProvider, apiProvider, sessionProvider, moshiProvider);
  }

  public static SyncRepository newInstance(SyncQueueDao syncQueueDao, RouteDao routeDao,
      StopDao stopDao, DaySessionDao daySessionDao, KpiValueDao kpiValueDao,
      BusinessProfileDao businessProfileDao, RutasApiService api, SessionManager session,
      Moshi moshi) {
    return new SyncRepository(syncQueueDao, routeDao, stopDao, daySessionDao, kpiValueDao, businessProfileDao, api, session, moshi);
  }
}
