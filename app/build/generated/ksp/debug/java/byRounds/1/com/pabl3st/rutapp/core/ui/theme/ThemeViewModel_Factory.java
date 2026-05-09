package com.pabl3st.rutapp.core.ui.theme;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ThemeViewModel_Factory implements Factory<ThemeViewModel> {
  private final Provider<ThemeRepository> themeRepoProvider;

  public ThemeViewModel_Factory(Provider<ThemeRepository> themeRepoProvider) {
    this.themeRepoProvider = themeRepoProvider;
  }

  @Override
  public ThemeViewModel get() {
    return newInstance(themeRepoProvider.get());
  }

  public static ThemeViewModel_Factory create(Provider<ThemeRepository> themeRepoProvider) {
    return new ThemeViewModel_Factory(themeRepoProvider);
  }

  public static ThemeViewModel newInstance(ThemeRepository themeRepo) {
    return new ThemeViewModel(themeRepo);
  }
}
