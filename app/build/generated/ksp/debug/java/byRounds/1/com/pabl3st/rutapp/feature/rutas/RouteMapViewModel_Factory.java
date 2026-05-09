package com.pabl3st.rutapp.feature.rutas;

import androidx.lifecycle.SavedStateHandle;
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
public final class RouteMapViewModel_Factory implements Factory<RouteMapViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<RouteRepository> routeRepoProvider;

  private final Provider<StopRepository> stopRepoProvider;

  private final Provider<LocationManager> locationMgrProvider;

  private final Provider<MapProvider> mapProvider;

  private final Provider<MapConfig> mapConfigProvider;

  public RouteMapViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<RouteRepository> routeRepoProvider, Provider<StopRepository> stopRepoProvider,
      Provider<LocationManager> locationMgrProvider, Provider<MapProvider> mapProvider,
      Provider<MapConfig> mapConfigProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.routeRepoProvider = routeRepoProvider;
    this.stopRepoProvider = stopRepoProvider;
    this.locationMgrProvider = locationMgrProvider;
    this.mapProvider = mapProvider;
    this.mapConfigProvider = mapConfigProvider;
  }

  @Override
  public RouteMapViewModel get() {
    return newInstance(savedStateHandleProvider.get(), routeRepoProvider.get(), stopRepoProvider.get(), locationMgrProvider.get(), mapProvider.get(), mapConfigProvider.get());
  }

  public static RouteMapViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<RouteRepository> routeRepoProvider, Provider<StopRepository> stopRepoProvider,
      Provider<LocationManager> locationMgrProvider, Provider<MapProvider> mapProvider,
      Provider<MapConfig> mapConfigProvider) {
    return new RouteMapViewModel_Factory(savedStateHandleProvider, routeRepoProvider, stopRepoProvider, locationMgrProvider, mapProvider, mapConfigProvider);
  }

  public static RouteMapViewModel newInstance(SavedStateHandle savedStateHandle,
      RouteRepository routeRepo, StopRepository stopRepo, LocationManager locationMgr,
      MapProvider mapProvider, MapConfig mapConfig) {
    return new RouteMapViewModel(savedStateHandle, routeRepo, stopRepo, locationMgr, mapProvider, mapConfig);
  }
}
