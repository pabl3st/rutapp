package com.pabl3st.rutapp.feature.home;

import com.pabl3st.rutapp.core.location.LocationManager;
import com.pabl3st.rutapp.data.repository.JornadaRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class JornadaViewModel_Factory implements Factory<JornadaViewModel> {
  private final Provider<JornadaRepository> jornadaRepoProvider;

  private final Provider<LocationManager> locationMgrProvider;

  public JornadaViewModel_Factory(Provider<JornadaRepository> jornadaRepoProvider,
      Provider<LocationManager> locationMgrProvider) {
    this.jornadaRepoProvider = jornadaRepoProvider;
    this.locationMgrProvider = locationMgrProvider;
  }

  @Override
  public JornadaViewModel get() {
    return newInstance(jornadaRepoProvider.get(), locationMgrProvider.get());
  }

  public static JornadaViewModel_Factory create(Provider<JornadaRepository> jornadaRepoProvider,
      Provider<LocationManager> locationMgrProvider) {
    return new JornadaViewModel_Factory(jornadaRepoProvider, locationMgrProvider);
  }

  public static JornadaViewModel newInstance(JornadaRepository jornadaRepo,
      LocationManager locationMgr) {
    return new JornadaViewModel(jornadaRepo, locationMgr);
  }
}
