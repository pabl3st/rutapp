package com.pabl3st.rutapp.fcm;

import com.pabl3st.rutapp.data.network.RutasApiService;
import com.pabl3st.rutapp.data.session.SessionManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class FcmTokenRepository_Factory implements Factory<FcmTokenRepository> {
  private final Provider<SessionManager> sessionProvider;

  private final Provider<RutasApiService> apiProvider;

  public FcmTokenRepository_Factory(Provider<SessionManager> sessionProvider,
      Provider<RutasApiService> apiProvider) {
    this.sessionProvider = sessionProvider;
    this.apiProvider = apiProvider;
  }

  @Override
  public FcmTokenRepository get() {
    return newInstance(sessionProvider.get(), apiProvider.get());
  }

  public static FcmTokenRepository_Factory create(Provider<SessionManager> sessionProvider,
      Provider<RutasApiService> apiProvider) {
    return new FcmTokenRepository_Factory(sessionProvider, apiProvider);
  }

  public static FcmTokenRepository newInstance(SessionManager session, RutasApiService api) {
    return new FcmTokenRepository(session, api);
  }
}
