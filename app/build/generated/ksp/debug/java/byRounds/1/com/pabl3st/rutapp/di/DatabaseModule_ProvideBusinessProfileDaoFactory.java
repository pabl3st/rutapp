package com.pabl3st.rutapp.di;

import com.pabl3st.rutapp.data.local.RutasDatabase;
import com.pabl3st.rutapp.data.local.dao.BusinessProfileDao;
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
public final class DatabaseModule_ProvideBusinessProfileDaoFactory implements Factory<BusinessProfileDao> {
  private final Provider<RutasDatabase> dbProvider;

  public DatabaseModule_ProvideBusinessProfileDaoFactory(Provider<RutasDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public BusinessProfileDao get() {
    return provideBusinessProfileDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideBusinessProfileDaoFactory create(
      Provider<RutasDatabase> dbProvider) {
    return new DatabaseModule_ProvideBusinessProfileDaoFactory(dbProvider);
  }

  public static BusinessProfileDao provideBusinessProfileDao(RutasDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideBusinessProfileDao(db));
  }
}
