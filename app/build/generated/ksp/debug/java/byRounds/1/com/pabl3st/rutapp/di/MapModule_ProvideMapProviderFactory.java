package com.pabl3st.rutapp.di;

import android.content.Context;
import com.pabl3st.rutapp.core.map.MapConfig;
import com.pabl3st.rutapp.core.map.MapProvider;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class MapModule_ProvideMapProviderFactory implements Factory<MapProvider> {
  private final Provider<Context> ctxProvider;

  private final Provider<MapConfig> configProvider;

  public MapModule_ProvideMapProviderFactory(Provider<Context> ctxProvider,
      Provider<MapConfig> configProvider) {
    this.ctxProvider = ctxProvider;
    this.configProvider = configProvider;
  }

  @Override
  public MapProvider get() {
    return provideMapProvider(ctxProvider.get(), configProvider.get());
  }

  public static MapModule_ProvideMapProviderFactory create(Provider<Context> ctxProvider,
      Provider<MapConfig> configProvider) {
    return new MapModule_ProvideMapProviderFactory(ctxProvider, configProvider);
  }

  public static MapProvider provideMapProvider(Context ctx, MapConfig config) {
    return Preconditions.checkNotNullFromProvides(MapModule.INSTANCE.provideMapProvider(ctx, config));
  }
}
