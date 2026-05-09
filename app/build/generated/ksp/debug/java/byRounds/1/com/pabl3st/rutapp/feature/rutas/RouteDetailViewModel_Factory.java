package com.pabl3st.rutapp.feature.rutas;

import androidx.lifecycle.SavedStateHandle;
import com.pabl3st.rutapp.core.location.LocationManager;
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
public final class RouteDetailViewModel_Factory implements Factory<RouteDetailViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<RouteRepository> routeRepoProvider;

  private final Provider<StopRepository> stopRepoProvider;

  private final Provider<LocationManager> locationMgrProvider;

  public RouteDetailViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<RouteRepository> routeRepoProvider, Provider<StopRepository> stopRepoProvider,
      Provider<LocationManager> locationMgrProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.routeRepoProvider = routeRepoProvider;
    this.stopRepoProvider = stopRepoProvider;
    this.locationMgrProvider = locationMgrProvider;
  }

  @Override
  public RouteDetailViewModel get() {
    return newInstance(savedStateHandleProvider.get(), routeRepoProvider.get(), stopRepoProvider.get(), locationMgrProvider.get());
  }

  public static RouteDetailViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<RouteRepository> routeRepoProvider, Provider<StopRepository> stopRepoProvider,
      Provider<LocationManager> locationMgrProvider) {
    return new RouteDetailViewModel_Factory(savedStateHandleProvider, routeRepoProvider, stopRepoProvider, locationMgrProvider);
  }

  public static RouteDetailViewModel newInstance(SavedStateHandle savedStateHandle,
      RouteRepository routeRepo, StopRepository stopRepo, LocationManager locationMgr) {
    return new RouteDetailViewModel(savedStateHandle, routeRepo, stopRepo, locationMgr);
  }
}
