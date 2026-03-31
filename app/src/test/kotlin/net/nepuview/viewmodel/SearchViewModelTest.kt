package net.nepuview.viewmodel

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import net.nepuview.data.Film
import net.nepuview.repository.FilmRepository
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: FilmRepository
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var viewModel: SearchViewModel

    private val sampleFilm = Film(
        id = "1", title = "Interstellar",
        posterUrl = "", detailUrl = "https://nepu.to/film/interstellar"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = mock(FilmRepository::class.java)
        prefs = mock(SharedPreferences::class.java)
        editor = mock(SharedPreferences.Editor::class.java)
        context = mock(Context::class.java)
        `when`(context.getSharedPreferences("search_history", Context.MODE_PRIVATE))
            .thenReturn(prefs)
        `when`(prefs.getString("terms", "")).thenReturn("")
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putString(any(), any())).thenReturn(editor)
        viewModel = SearchViewModel(repo, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty query and results`() {
        assertEquals("", viewModel.query.value)
        assertTrue(viewModel.results.value.isEmpty())
    }

    @Test
    fun `search updates query`() = runTest {
        `when`(repo.search("interstellar")).thenReturn(flowOf(listOf(sampleFilm)))
        viewModel.search("interstellar")
        assertEquals("interstellar", viewModel.query.value)
    }

    @Test
    fun `search blank input does nothing`() = runTest {
        viewModel.search("   ")
        assertEquals("", viewModel.query.value)
        verifyNoInteractions(repo)
    }

    @Test
    fun `search stores term in history`() = runTest {
        `when`(repo.search("inception")).thenReturn(flowOf(emptyList()))
        viewModel.search("inception")
        assertTrue(viewModel.history.value.contains("inception"))
    }

    @Test
    fun `clearQuery resets state`() = runTest {
        `when`(repo.search("test")).thenReturn(flowOf(listOf(sampleFilm)))
        viewModel.search("test")
        viewModel.clearQuery()
        assertEquals("", viewModel.query.value)
        assertTrue(viewModel.results.value.isEmpty())
    }

    @Test
    fun `removeHistory removes term`() = runTest {
        `when`(repo.search("gone")).thenReturn(flowOf(emptyList()))
        viewModel.search("gone")
        viewModel.removeHistory("gone")
        assertFalse(viewModel.history.value.contains("gone"))
    }
}
