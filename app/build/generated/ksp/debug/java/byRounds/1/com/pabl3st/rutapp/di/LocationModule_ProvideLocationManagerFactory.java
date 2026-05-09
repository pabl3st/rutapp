package com.pabl3st.rutapp.di;

import android.content.Context;
import com.pabl3st.rutapp.core.location.LocationManager;
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
public final class LocationModule_ProvideLocationManagerFactory implements Factory<LocationManager> {
  private final Provider<Context> ctxProvider;

  public LocationModule_ProvideLocationManagerFactory(Provider<Context> ctxProvider) {
    this.ctxProvider = ctxProvider;
  }

  @Override
  public LocationManager get() {
    return provideLocationManager(ctxProvider.get());
  }

  public static LocationModule_ProvideLocationManagerFactory create(Provider<Context> ctxProvider) {
    return new LocationModule_ProvideLocationManagerFactory(ctxProvider);
  }

  public static LocationManager provideLocationManager(Context ctx) {
    return Preconditions.checkNotNullFromProvides(LocationModule.INSTANCE.provideLocationManager(ctx));
  }
}
