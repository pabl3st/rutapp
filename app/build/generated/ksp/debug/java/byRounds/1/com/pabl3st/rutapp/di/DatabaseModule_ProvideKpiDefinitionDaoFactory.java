package com.pabl3st.rutapp.di;

import com.pabl3st.rutapp.data.local.RutasDatabase;
import com.pabl3st.rutapp.data.local.dao.KpiDefinitionDao;
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
public final class DatabaseModule_ProvideKpiDefinitionDaoFactory implements Factory<KpiDefinitionDao> {
  private final Provider<RutasDatabase> dbProvider;

  public DatabaseModule_ProvideKpiDefinitionDaoFactory(Provider<RutasDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public KpiDefinitionDao get() {
    return provideKpiDefinitionDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideKpiDefinitionDaoFactory create(
      Provider<RutasDatabase> dbProvider) {
    return new DatabaseModule_ProvideKpiDefinitionDaoFactory(dbProvider);
  }

  public static KpiDefinitionDao provideKpiDefinitionDao(RutasDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideKpiDefinitionDao(db));
  }
}
