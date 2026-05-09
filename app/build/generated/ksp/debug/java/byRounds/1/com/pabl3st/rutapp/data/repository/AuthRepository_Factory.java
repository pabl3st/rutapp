package com.pabl3st.rutapp.data.repository;

import android.content.Context;
import com.pabl3st.rutapp.data.network.RutasApiService;
import com.pabl3st.rutapp.data.session.SessionManager;
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
public final class AuthRepository_Factory implements Factory<AuthRepository> {
  private final Provider<RutasApiService> apiProvider;

  private final Provider<SessionManager> sessionProvider;

  private final Provider<Context> contextProvider;

  public AuthRepository_Factory(Provider<RutasApiService> apiProvider,
      Provider<SessionManager> sessionProvider, Provider<Context> contextProvider) {
    this.apiProvider = apiProvider;
    this.sessionProvider = sessionProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public AuthRepository get() {
    return newInstance(apiProvider.get(), sessionProvider.get(), contextProvider.get());
  }

  public static AuthRepository_Factory create(Provider<RutasApiService> apiProvider,
      Provider<SessionManager> sessionProvider, Provider<Context> contextProvider) {
    return new AuthRepository_Factory(apiProvider, sessionProvider, contextProvider);
  }

  public static AuthRepository newInstance(RutasApiService api, SessionManager session,
      Context context) {
    return new AuthRepository(api, session, context);
  }
}
