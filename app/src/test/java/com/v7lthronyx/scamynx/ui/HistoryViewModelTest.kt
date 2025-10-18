package com.v7lthronyx.scamynx.ui

import com.v7lthronyx.scamynx.domain.model.MlReport
import com.v7lthronyx.scamynx.domain.model.NetworkReport
import com.v7lthronyx.scamynx.domain.model.RiskBreakdown
import com.v7lthronyx.scamynx.domain.model.RiskCategory
import com.v7lthronyx.scamynx.domain.model.Provider
import com.v7lthronyx.scamynx.domain.model.ScanRequest
import com.v7lthronyx.scamynx.domain.model.ScanResult
import com.v7lthronyx.scamynx.domain.model.ScanState
import com.v7lthronyx.scamynx.domain.model.ScanTargetType
import com.v7lthronyx.scamynx.domain.model.VendorVerdict
import com.v7lthronyx.scamynx.domain.model.VerdictStatus
import com.v7lthronyx.scamynx.domain.repository.ScanRepository
import com.v7lthronyx.scamynx.ui.history.HistoryViewModel
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeScanRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = FakeScanRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refresh pulls latest history`() = runTest(dispatcher) {
        val viewModel = HistoryViewModel(repository)
        advanceUntilIdle()
        assertEquals(0, viewModel.state.value.items.size)

        repository.emitHistory(listOf(sampleResult()))
        advanceUntilIdle()
        assertEquals(1, viewModel.state.value.items.size)

        repository.emitHistory(emptyList())
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(0, viewModel.state.value.items.size)
    }

    @Test
    fun `risk breakdown drives category selection`() = runTest(dispatcher) {
        val viewModel = HistoryViewModel(repository)
        advanceUntilIdle()

        val breakdown = RiskBreakdown(
            categories = mapOf(
                RiskCategory.MEDIUM to 0.62,
                RiskCategory.LOW to 0.28,
            ),
        )

        repository.emitHistory(listOf(sampleResult(risk = 1.4, breakdown = breakdown)))
        advanceUntilIdle()

        val item = viewModel.state.value.items.single()
        assertEquals(RiskCategory.MEDIUM, item.riskCategory)
    }

    private fun sampleResult(
        risk: Double = 0.3,
        breakdown: RiskBreakdown = RiskBreakdown(),
    ): ScanResult = ScanResult(
        sessionId = UUID.randomUUID().toString(),
        targetType = ScanTargetType.URL,
        targetLabel = "https://example.com",
        normalizedUrl = "https://example.com",
        vendors = listOf(
            VendorVerdict(provider = Provider.VIRUS_TOTAL, status = VerdictStatus.CLEAN, score = 0.0),
        ),
        network = NetworkReport(certValid = true, headers = emptyMap()),
        ml = MlReport(probability = 0.1),
        risk = risk,
        breakdown = breakdown,
        createdAt = Clock.System.now(),
    )
}

private class FakeScanRepository : ScanRepository {
    private val historyFlow = MutableSharedFlow<List<ScanResult>>(replay = 1)

    init {
        historyFlow.tryEmit(emptyList())
    }
    private var currentHistory: List<ScanResult> = emptyList()

    suspend fun emitHistory(list: List<ScanResult>) {
        currentHistory = list
        historyFlow.emit(list)
    }

    override fun analyze(request: ScanRequest) = flowOf<ScanState>()

    override fun observeHistory(limit: Int): Flow<List<ScanResult>> = historyFlow

    override suspend fun getHistory(limit: Int, offset: Int): List<ScanResult> = currentHistory

    override suspend fun getScan(sessionId: String): ScanResult? = currentHistory.firstOrNull { it.sessionId == sessionId }

    override suspend fun clearHistory() {
        emitHistory(emptyList())
    }
}
