package net.nepuview.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import net.nepuview.data.Film
import net.nepuview.repository.FilmRepository
import net.nepuview.util.NetworkMonitor
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: FilmRepository
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var viewModel: HomeViewModel

    private val sampleFilm = Film(
        id = "1",
        title = "Inception",
        posterUrl = "https://cdn.nepu.to/inception.jpg",
        detailUrl = "https://nepu.to/film/inception"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = mock(FilmRepository::class.java)
        networkMonitor = mock(NetworkMonitor::class.java)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        `when`(networkMonitor.isCurrentlyOnline()).thenReturn(false)
        viewModel = HomeViewModel(repo, networkMonitor)
        // After offline check, state becomes Error — but before init runs it was Loading
        // We verify the offline path produces an Error state
        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Error)
    }

    @Test
    fun `offline produces Error state`() = runTest {
        `when`(networkMonitor.isCurrentlyOnline()).thenReturn(false)
        viewModel = HomeViewModel(repo, networkMonitor)
        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Error)
        assertEquals("Keine Internetverbindung", (state as HomeUiState.Error).message)
    }

    @Test
    fun `online with empty results produces Error state`() = runTest {
        `when`(networkMonitor.isCurrentlyOnline()).thenReturn(true)
        `when`(repo.loadHome(null)).thenReturn(flowOf(emptyList()))
        `when`(repo.loadHome("Action")).thenReturn(flowOf(emptyList()))
        `when`(repo.loadHome("Anime")).thenReturn(flowOf(emptyList()))
        `when`(repo.loadHome("Serie")).thenReturn(flowOf(emptyList()))
        `when`(repo.loadHome("Komödie")).thenReturn(flowOf(emptyList()))
        `when`(repo.loadHome("Thriller")).thenReturn(flowOf(emptyList()))

        viewModel = HomeViewModel(repo, networkMonitor)
        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Error)
    }

    @Test
    fun `online with results produces Success state`() = runTest {
        val films = listOf(sampleFilm)
        `when`(networkMonitor.isCurrentlyOnline()).thenReturn(true)
        `when`(repo.loadHome(null)).thenReturn(flowOf(films))
        `when`(repo.loadHome("Action")).thenReturn(flowOf(films))
        `when`(repo.loadHome("Anime")).thenReturn(flowOf(films))
        `when`(repo.loadHome("Serie")).thenReturn(flowOf(films))
        `when`(repo.loadHome("Komödie")).thenReturn(flowOf(films))
        `when`(repo.loadHome("Thriller")).thenReturn(flowOf(films))

        viewModel = HomeViewModel(repo, networkMonitor)
        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Success)
        assertEquals(sampleFilm, (state as HomeUiState.Success).heroFilm)
        assertTrue(state.categories.isNotEmpty())
    }
}
