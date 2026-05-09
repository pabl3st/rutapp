package com.pabl3st.rutapp.di;

import com.pabl3st.rutapp.data.network.RutasApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

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
public final class NetworkModule_ProvideApiServiceFactory implements Factory<RutasApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideApiServiceFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public RutasApiService get() {
    return provideApiService(retrofitProvider.get());
  }

  public static NetworkModule_ProvideApiServiceFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideApiServiceFactory(retrofitProvider);
  }

  public static RutasApiService provideApiService(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideApiService(retrofit));
  }
}
