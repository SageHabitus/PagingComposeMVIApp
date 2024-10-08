package com.example.composemvi.data.book

import android.util.Log
import androidx.paging.PagingData
import androidx.paging.map
import com.example.composemvi.data.book.dummy.book.DummyBooks.getMockBookEntities
import com.example.composemvi.data.book.dummy.book.DummyBooks.getMockBookResponse
import com.example.composemvi.data.book.paging.BookDiffCallback
import com.example.composemvi.data.mapper.toDataModel
import com.example.composemvi.data.repository.BookRepository
import com.example.composemvi.data.source.local.entity.BookEntity
import com.example.composemvi.util.MainCoroutineRule
import com.example.composemvi.util.TestPagingDataDiffer
import io.mockk.mockkStatic
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class BookRepositoryTest {

    @Mock
    private lateinit var repository: BookRepository

    private lateinit var mockWebServer: MockWebServer
    private lateinit var bookEntities: List<BookEntity>
    private lateinit var bookMockResponse: MockResponse

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        mockWebServer = MockWebServer()
        mockkStatic(Log::class)
        bookEntities = getMockBookEntities()
        bookMockResponse = getMockBookResponse()
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `페이징 데이터를 성공적으로 로드하고 크기를 확인해야 한다`() = runBlocking {
        val pagingData = PagingData.from(bookEntities).map { it.toDataModel() }

        `when`(repository.getAllBooks()).thenReturn(flowOf(pagingData))

        val resultFlow = repository.getAllBooks()
        var itemCount = 0

        val differ = TestPagingDataDiffer(BookDiffCallback())

        resultFlow.collectLatest { pagingData ->
            differ.submitData(pagingData)
            differ.snapshot().items.forEach {
                itemCount++
            }
        }

        assertEquals(bookEntities.size, itemCount)
    }

    @Test
    fun `리모트 API에서 책 데이터를 성공적으로 로드하고 변환해야 한다`() = runBlocking {
        mockWebServer.enqueue(bookMockResponse)

        val pagingData = PagingData.from(bookEntities).map { it.toDataModel() }

        `when`(repository.searchAndCacheBooks("query")).thenReturn(flowOf(pagingData))

        val resultFlow = repository.searchAndCacheBooks("query")
        var itemCount = 0

        val differ = TestPagingDataDiffer(BookDiffCallback())

        resultFlow.collectLatest { pagingData ->
            differ.submitData(pagingData)
            differ.snapshot().items.forEach {
                itemCount++
                assertTrue(it.title.isNotEmpty())
            }
        }

        assertEquals(bookEntities.size, itemCount)
    }
}
