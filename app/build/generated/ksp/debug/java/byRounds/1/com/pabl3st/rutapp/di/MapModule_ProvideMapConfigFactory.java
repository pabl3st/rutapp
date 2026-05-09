package com.pabl3st.rutapp.di;

import com.pabl3st.rutapp.core.map.MapConfig;
import com.pabl3st.rutapp.data.session.SessionManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class MapModule_ProvideMapConfigFactory implements Factory<MapConfig> {
  private final Provider<SessionManager> sessionProvider;

  public MapModule_ProvideMapConfigFactory(Provider<SessionManager> sessionProvider) {
    this.sessionProvider = sessionProvider;
  }

  @Override
  public MapConfig get() {
    return provideMapConfig(sessionProvider.get());
  }

  public static MapModule_ProvideMapConfigFactory create(Provider<SessionManager> sessionProvider) {
    return new MapModule_ProvideMapConfigFactory(sessionProvider);
  }

  public static MapConfig provideMapConfig(SessionManager session) {
    return Preconditions.checkNotNullFromProvides(MapModule.INSTANCE.provideMapConfig(session));
  }
}
