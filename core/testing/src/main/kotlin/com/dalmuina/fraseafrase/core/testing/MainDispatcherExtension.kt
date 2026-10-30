package com.dalmuina.fraseafrase.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Sustituye `Dispatchers.Main` por un [TestDispatcher] durante cada test.
 * Necesaria para probar ViewModels, que usan `viewModelScope` (ligado a Main).
 *
 * ```
 * class SessionViewModelTest {
 *     @JvmField
 *     @RegisterExtension
 *     val mainDispatcher = MainDispatcherExtension()
 * }
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherExtension(val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()) :
    BeforeEachCallback,
    AfterEachCallback {

    override fun beforeEach(context: ExtensionContext) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun afterEach(context: ExtensionContext) {
        Dispatchers.resetMain()
    }
}
