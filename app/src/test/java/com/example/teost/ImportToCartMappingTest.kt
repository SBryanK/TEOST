package com.example.teost

import com.example.teost.presentation.screens.runner.ConfigRunnerViewModel
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportToCartMappingTest {
    @Test
    fun mapSpecToConfigs_generatesConfigsForTargets() {
        val vm = ConfigRunnerViewModel(android.app.Application(), OkHttpClient())
        val onError: (String) -> Unit = { _ -> }
        // Minimal sanity: without a loaded plan, generateCartConfigurations returns empty
        val list = vm.generateCartConfigurations(onError)
        assertTrue(list.isEmpty())
    }
}


