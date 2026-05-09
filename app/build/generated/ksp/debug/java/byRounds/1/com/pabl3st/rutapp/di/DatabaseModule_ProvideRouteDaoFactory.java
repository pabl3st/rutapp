package com.pabl3st.rutapp.di;

import com.pabl3st.rutapp.data.local.RutasDatabase;
import com.pabl3st.rutapp.data.local.dao.RouteDao;
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
public final class DatabaseModule_ProvideRouteDaoFactory implements Factory<RouteDao> {
  private final Provider<RutasDatabase> dbProvider;

  public DatabaseModule_ProvideRouteDaoFactory(Provider<RutasDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public RouteDao get() {
    return provideRouteDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideRouteDaoFactory create(Provider<RutasDatabase> dbProvider) {
    return new DatabaseModule_ProvideRouteDaoFactory(dbProvider);
  }

  public static RouteDao provideRouteDao(RutasDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideRouteDao(db));
  }
}
