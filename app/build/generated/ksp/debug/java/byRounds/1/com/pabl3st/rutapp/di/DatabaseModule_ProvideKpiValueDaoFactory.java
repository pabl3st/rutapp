package com.pabl3st.rutapp.di;

import com.pabl3st.rutapp.data.local.RutasDatabase;
import com.pabl3st.rutapp.data.local.dao.KpiValueDao;
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
public final class DatabaseModule_ProvideKpiValueDaoFactory implements Factory<KpiValueDao> {
  private final Provider<RutasDatabase> dbProvider;

  public DatabaseModule_ProvideKpiValueDaoFactory(Provider<RutasDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public KpiValueDao get() {
    return provideKpiValueDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideKpiValueDaoFactory create(
      Provider<RutasDatabase> dbProvider) {
    return new DatabaseModule_ProvideKpiValueDaoFactory(dbProvider);
  }

  public static KpiValueDao provideKpiValueDao(RutasDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideKpiValueDao(db));
  }
}
