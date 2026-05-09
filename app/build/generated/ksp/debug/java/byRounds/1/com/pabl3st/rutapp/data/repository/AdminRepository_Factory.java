package com.pabl3st.rutapp.data.repository;

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
public final class AdminRepository_Factory implements Factory<AdminRepository> {
  private final Provider<RutasApiService> apiProvider;

  private final Provider<SessionManager> sessionProvider;

  public AdminRepository_Factory(Provider<RutasApiService> apiProvider,
      Provider<SessionManager> sessionProvider) {
    this.apiProvider = apiProvider;
    this.sessionProvider = sessionProvider;
  }

  @Override
  public AdminRepository get() {
    return newInstance(apiProvider.get(), sessionProvider.get());
  }

  public static AdminRepository_Factory create(Provider<RutasApiService> apiProvider,
      Provider<SessionManager> sessionProvider) {
    return new AdminRepository_Factory(apiProvider, sessionProvider);
  }

  public static AdminRepository newInstance(RutasApiService api, SessionManager session) {
    return new AdminRepository(api, session);
  }
}
