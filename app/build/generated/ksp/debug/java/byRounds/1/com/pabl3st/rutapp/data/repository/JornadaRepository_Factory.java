package com.pabl3st.rutapp.data.repository;

import com.pabl3st.rutapp.core.location.LocationManager;
import com.pabl3st.rutapp.data.local.dao.DaySessionDao;
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
public final class JornadaRepository_Factory implements Factory<JornadaRepository> {
  private final Provider<DaySessionDao> daoProvider;

  private final Provider<LocationManager> locationMgrProvider;

  public JornadaRepository_Factory(Provider<DaySessionDao> daoProvider,
      Provider<LocationManager> locationMgrProvider) {
    this.daoProvider = daoProvider;
    this.locationMgrProvider = locationMgrProvider;
  }

  @Override
  public JornadaRepository get() {
    return newInstance(daoProvider.get(), locationMgrProvider.get());
  }

  public static JornadaRepository_Factory create(Provider<DaySessionDao> daoProvider,
      Provider<LocationManager> locationMgrProvider) {
    return new JornadaRepository_Factory(daoProvider, locationMgrProvider);
  }

  public static JornadaRepository newInstance(DaySessionDao dao, LocationManager locationMgr) {
    return new JornadaRepository(dao, locationMgr);
  }
}
