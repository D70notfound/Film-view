package net.nepuview.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import net.nepuview.data.WatchProgress
import net.nepuview.repository.FilmRepository
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: FilmRepository
    private lateinit var viewModel: PlayerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = mock(FilmRepository::class.java)
        viewModel = PlayerViewModel(repo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial m3u8Url is null`() {
        assertNull(viewModel.m3u8Url.value)
    }

    @Test
    fun `onM3u8Found sets url`() {
        viewModel.onM3u8Found("https://cdn.vr-m.net/stream/master.m3u8")
        assertEquals("https://cdn.vr-m.net/stream/master.m3u8", viewModel.m3u8Url.value)
    }

    @Test
    fun `onM3u8Found only stores first url`() {
        viewModel.onM3u8Found("https://cdn.vr-m.net/stream/first.m3u8")
        viewModel.onM3u8Found("https://cdn.vr-m.net/stream/second.m3u8")
        assertEquals("https://cdn.vr-m.net/stream/first.m3u8", viewModel.m3u8Url.value)
    }

    @Test
    fun `clearM3u8 resets url to null`() {
        viewModel.onM3u8Found("https://cdn.vr-m.net/stream/master.m3u8")
        viewModel.clearM3u8()
        assertNull(viewModel.m3u8Url.value)
    }

    @Test
    fun `setCurrentFilm resets m3u8Url`() {
        viewModel.onM3u8Found("https://cdn.vr-m.net/stream/master.m3u8")
        viewModel.setCurrentFilm("film1", "Inception", "poster.jpg", "https://nepu.to/watch/1")
        assertNull(viewModel.m3u8Url.value)
    }

    @Test
    fun `saveProgress calls repo when filmId is set`() = runTest {
        viewModel.setCurrentFilm("film1", "Inception", "poster.jpg", "https://nepu.to/watch/1")
        viewModel.saveProgress(30_000L, 120_000L)
        val captor = org.mockito.ArgumentCaptor.forClass(WatchProgress::class.java)
        verify(repo).saveProgress(captor.capture())
        val saved = captor.value
        assertEquals("film1", saved.filmId)
        assertEquals("Inception", saved.filmTitle)
        assertEquals("poster.jpg", saved.posterUrl)
        assertEquals("https://nepu.to/watch/1", saved.playerUrl)
        assertEquals(30_000L, saved.positionMs)
        assertEquals(120_000L, saved.durationMs)
    }

    @Test
    fun `saveProgress does nothing when no filmId set`() = runTest {
        viewModel.saveProgress(30_000L, 120_000L)
        verifyNoInteractions(repo)
    }
}
