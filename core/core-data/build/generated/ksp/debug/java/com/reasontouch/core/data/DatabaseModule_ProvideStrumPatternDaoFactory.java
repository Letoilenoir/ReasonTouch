package com.reasontouch.core.data;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class DatabaseModule_ProvideStrumPatternDaoFactory implements Factory<StrumPatternDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideStrumPatternDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public StrumPatternDao get() {
    return provideStrumPatternDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideStrumPatternDaoFactory create(
      Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideStrumPatternDaoFactory(dbProvider);
  }

  public static StrumPatternDao provideStrumPatternDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideStrumPatternDao(db));
  }
}
