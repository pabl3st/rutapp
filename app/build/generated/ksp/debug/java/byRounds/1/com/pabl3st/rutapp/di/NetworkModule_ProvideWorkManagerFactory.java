package com.pabl3st.rutapp.di;

import android.content.Context;
import androidx.work.WorkManager;
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
public final class NetworkModule_ProvideWorkManagerFactory implements Factory<WorkManager> {
  private final Provider<Context> ctxProvider;

  public NetworkModule_ProvideWorkManagerFactory(Provider<Context> ctxProvider) {
    this.ctxProvider = ctxProvider;
  }

  @Override
  public WorkManager get() {
    return provideWorkManager(ctxProvider.get());
  }

  public static NetworkModule_ProvideWorkManagerFactory create(Provider<Context> ctxProvider) {
    return new NetworkModule_ProvideWorkManagerFactory(ctxProvider);
  }

  public static WorkManager provideWorkManager(Context ctx) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideWorkManager(ctx));
  }
}
