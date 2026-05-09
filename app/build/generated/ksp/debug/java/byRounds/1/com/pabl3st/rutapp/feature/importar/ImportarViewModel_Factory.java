package com.pabl3st.rutapp.feature.importar;

import android.content.Context;
import com.pabl3st.rutapp.data.repository.RouteRepository;
import com.pabl3st.rutapp.data.repository.StopRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class ImportarViewModel_Factory implements Factory<ImportarViewModel> {
  private final Provider<Context> ctxProvider;

  private final Provider<StopRepository> stopRepoProvider;

  private final Provider<RouteRepository> routeRepoProvider;

  public ImportarViewModel_Factory(Provider<Context> ctxProvider,
      Provider<StopRepository> stopRepoProvider, Provider<RouteRepository> routeRepoProvider) {
    this.ctxProvider = ctxProvider;
    this.stopRepoProvider = stopRepoProvider;
    this.routeRepoProvider = routeRepoProvider;
  }

  @Override
  public ImportarViewModel get() {
    return newInstance(ctxProvider.get(), stopRepoProvider.get(), routeRepoProvider.get());
  }

  public static ImportarViewModel_Factory create(Provider<Context> ctxProvider,
      Provider<StopRepository> stopRepoProvider, Provider<RouteRepository> routeRepoProvider) {
    return new ImportarViewModel_Factory(ctxProvider, stopRepoProvider, routeRepoProvider);
  }

  public static ImportarViewModel newInstance(Context ctx, StopRepository stopRepo,
      RouteRepository routeRepo) {
    return new ImportarViewModel(ctx, stopRepo, routeRepo);
  }
}
