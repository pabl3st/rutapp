package com.pabl3st.rutapp.feature.rutas;

import androidx.lifecycle.SavedStateHandle;
import com.pabl3st.rutapp.core.map.MapProvider;
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
public final class CrearParadaViewModel_Factory implements Factory<CrearParadaViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<StopRepository> stopRepoProvider;

  private final Provider<MapProvider> mapProvider;

  public CrearParadaViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<StopRepository> stopRepoProvider, Provider<MapProvider> mapProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.stopRepoProvider = stopRepoProvider;
    this.mapProvider = mapProvider;
  }

  @Override
  public CrearParadaViewModel get() {
    return newInstance(savedStateHandleProvider.get(), stopRepoProvider.get(), mapProvider.get());
  }

  public static CrearParadaViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<StopRepository> stopRepoProvider, Provider<MapProvider> mapProvider) {
    return new CrearParadaViewModel_Factory(savedStateHandleProvider, stopRepoProvider, mapProvider);
  }

  public static CrearParadaViewModel newInstance(SavedStateHandle savedStateHandle,
      StopRepository stopRepo, MapProvider mapProvider) {
    return new CrearParadaViewModel(savedStateHandle, stopRepo, mapProvider);
  }
}
