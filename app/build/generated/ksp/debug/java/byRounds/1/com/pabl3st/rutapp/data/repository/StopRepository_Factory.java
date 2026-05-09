package com.pabl3st.rutapp.data.repository;

import com.pabl3st.rutapp.data.local.dao.StopDao;
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao;
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
public final class StopRepository_Factory implements Factory<StopRepository> {
  private final Provider<StopDao> stopDaoProvider;

  private final Provider<SyncQueueDao> syncQueueDaoProvider;

  private final Provider<SessionManager> sessionProvider;

  private final Provider<Moshi> moshiProvider;

  public StopRepository_Factory(Provider<StopDao> stopDaoProvider,
      Provider<SyncQueueDao> syncQueueDaoProvider, Provider<SessionManager> sessionProvider,
      Provider<Moshi> moshiProvider) {
    this.stopDaoProvider = stopDaoProvider;
    this.syncQueueDaoProvider = syncQueueDaoProvider;
    this.sessionProvider = sessionProvider;
    this.moshiProvider = moshiProvider;
  }

  @Override
  public StopRepository get() {
    return newInstance(stopDaoProvider.get(), syncQueueDaoProvider.get(), sessionProvider.get(), moshiProvider.get());
  }

  public static StopRepository_Factory create(Provider<StopDao> stopDaoProvider,
      Provider<SyncQueueDao> syncQueueDaoProvider, Provider<SessionManager> sessionProvider,
      Provider<Moshi> moshiProvider) {
    return new StopRepository_Factory(stopDaoProvider, syncQueueDaoProvider, sessionProvider, moshiProvider);
  }

  public static StopRepository newInstance(StopDao stopDao, SyncQueueDao syncQueueDao,
      SessionManager session, Moshi moshi) {
    return new StopRepository(stopDao, syncQueueDao, session, moshi);
  }
}
