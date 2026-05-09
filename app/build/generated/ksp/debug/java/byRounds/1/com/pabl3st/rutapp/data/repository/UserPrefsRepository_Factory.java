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
public final class UserPrefsRepository_Factory implements Factory<UserPrefsRepository> {
  private final Provider<Context> ctxProvider;

  private final Provider<SessionManager> sessionProvider;

  private final Provider<RutasApiService> apiProvider;

  public UserPrefsRepository_Factory(Provider<Context> ctxProvider,
      Provider<SessionManager> sessionProvider, Provider<RutasApiService> apiProvider) {
    this.ctxProvider = ctxProvider;
    this.sessionProvider = sessionProvider;
    this.apiProvider = apiProvider;
  }

  @Override
  public UserPrefsRepository get() {
    return newInstance(ctxProvider.get(), sessionProvider.get(), apiProvider.get());
  }

  public static UserPrefsRepository_Factory create(Provider<Context> ctxProvider,
      Provider<SessionManager> sessionProvider, Provider<RutasApiService> apiProvider) {
    return new UserPrefsRepository_Factory(ctxProvider, sessionProvider, apiProvider);
  }

  public static UserPrefsRepository newInstance(Context ctx, SessionManager session,
      RutasApiService api) {
    return new UserPrefsRepository(ctx, session, api);
  }
}
