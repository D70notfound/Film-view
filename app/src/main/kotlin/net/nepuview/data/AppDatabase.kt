package net.nepuview.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [
        WatchProgress::class,
        WatchHistory::class,
        FavoriteFilm::class,
        WatchlistFilm::class,
        DownloadedFilm::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun filmDao(): FilmDao

    companion object {
        const val DATABASE_NAME = "nepuview.db"
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .build()

    @Provides
    fun provideFilmDao(db: AppDatabase): FilmDao = db.filmDao()
}
