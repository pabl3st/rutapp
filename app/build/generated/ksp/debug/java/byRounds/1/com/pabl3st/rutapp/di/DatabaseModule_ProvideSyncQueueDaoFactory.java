package com.pabl3st.rutapp.di;

import com.pabl3st.rutapp.data.local.RutasDatabase;
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideSyncQueueDaoFactory implements Factory<SyncQueueDao> {
  private final Provider<RutasDatabase> dbProvider;

  public DatabaseModule_ProvideSyncQueueDaoFactory(Provider<RutasDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public SyncQueueDao get() {
    return provideSyncQueueDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideSyncQueueDaoFactory create(
      Provider<RutasDatabase> dbProvider) {
    return new DatabaseModule_ProvideSyncQueueDaoFactory(dbProvider);
  }

  public static SyncQueueDao provideSyncQueueDao(RutasDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideSyncQueueDao(db));
  }
}
