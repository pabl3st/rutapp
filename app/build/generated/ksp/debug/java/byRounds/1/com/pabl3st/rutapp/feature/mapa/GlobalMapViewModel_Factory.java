package com.pabl3st.rutapp.feature.mapa;

import com.pabl3st.rutapp.core.location.LocationManager;
import com.pabl3st.rutapp.core.map.MapConfig;
import com.pabl3st.rutapp.core.map.MapProvider;
import com.pabl3st.rutapp.data.repository.RouteRepository;
import com.pabl3st.rutapp.data.repository.StopRepository;
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
public final class GlobalMapViewModel_Factory implements Factory<GlobalMapViewModel> {
  private final Provider<RouteRepository> routeRepoProvider;

  private final Provider<StopRepository> stopRepoProvider;

  private final Provider<LocationManager> locationMgrProvider;

  private final Provider<MapProvider> mapProvider;

  private final Provider<MapConfig> mapConfigProvider;

  public GlobalMapViewModel_Factory(Provider<RouteRepository> routeRepoProvider,
      Provider<StopRepository> stopRepoProvider, Provider<LocationManager> locationMgrProvider,
      Provider<MapProvider> mapProvider, Provider<MapConfig> mapConfigProvider) {
    this.routeRepoProvider = routeRepoProvider;
    this.stopRepoProvider = stopRepoProvider;
    this.locationMgrProvider = locationMgrProvider;
    this.mapProvider = mapProvider;
    this.mapConfigProvider = mapConfigProvider;
  }

  @Override
  public GlobalMapViewModel get() {
    return newInstance(routeRepoProvider.get(), stopRepoProvider.get(), locationMgrProvider.get(), mapProvider.get(), mapConfigProvider.get());
  }

  public static GlobalMapViewModel_Factory create(Provider<RouteRepository> routeRepoProvider,
      Provider<StopRepository> stopRepoProvider, Provider<LocationManager> locationMgrProvider,
      Provider<MapProvider> mapProvider, Provider<MapConfig> mapConfigProvider) {
    return new GlobalMapViewModel_Factory(routeRepoProvider, stopRepoProvider, locationMgrProvider, mapProvider, mapConfigProvider);
  }

  public static GlobalMapViewModel newInstance(RouteRepository routeRepo, StopRepository stopRepo,
      LocationManager locationMgr, MapProvider mapProvider, MapConfig mapConfig) {
    return new GlobalMapViewModel(routeRepo, stopRepo, locationMgr, mapProvider, mapConfig);
  }
}
