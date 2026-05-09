package com.pabl3st.rutapp.di;

import com.pabl3st.rutapp.data.local.RutasDatabase;
import com.pabl3st.rutapp.data.local.dao.DaySessionDao;
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
public final class DatabaseModule_ProvideDaySessionDaoFactory implements Factory<DaySessionDao> {
  private final Provider<RutasDatabase> dbProvider;

  public DatabaseModule_ProvideDaySessionDaoFactory(Provider<RutasDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public DaySessionDao get() {
    return provideDaySessionDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideDaySessionDaoFactory create(
      Provider<RutasDatabase> dbProvider) {
    return new DatabaseModule_ProvideDaySessionDaoFactory(dbProvider);
  }

  public static DaySessionDao provideDaySessionDao(RutasDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideDaySessionDao(db));
  }
}
