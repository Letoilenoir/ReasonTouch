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
public final class DatabaseModule_ProvideChordEventDaoFactory implements Factory<ChordEventDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideChordEventDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public ChordEventDao get() {
    return provideChordEventDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideChordEventDaoFactory create(
      Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideChordEventDaoFactory(dbProvider);
  }

  public static ChordEventDao provideChordEventDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideChordEventDao(db));
  }
}
