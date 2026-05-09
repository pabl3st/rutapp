package com.pabl3st.rutapp.di;

import com.pabl3st.rutapp.data.local.RutasDatabase;
import com.pabl3st.rutapp.data.local.dao.StopDao;
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
public final class DatabaseModule_ProvideStopDaoFactory implements Factory<StopDao> {
  private final Provider<RutasDatabase> dbProvider;

  public DatabaseModule_ProvideStopDaoFactory(Provider<RutasDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public StopDao get() {
    return provideStopDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideStopDaoFactory create(Provider<RutasDatabase> dbProvider) {
    return new DatabaseModule_ProvideStopDaoFactory(dbProvider);
  }

  public static StopDao provideStopDao(RutasDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideStopDao(db));
  }
}
