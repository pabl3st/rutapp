package com.pabl3st.rutapp.core.location;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class LocationManager_Factory implements Factory<LocationManager> {
  private final Provider<Context> contextProvider;

  public LocationManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public LocationManager get() {
    return newInstance(contextProvider.get());
  }

  public static LocationManager_Factory create(Provider<Context> contextProvider) {
    return new LocationManager_Factory(contextProvider);
  }

  public static LocationManager newInstance(Context context) {
    return new LocationManager(context);
  }
}
