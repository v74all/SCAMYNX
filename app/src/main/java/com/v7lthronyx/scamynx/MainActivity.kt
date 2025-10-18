package com.v7lthronyx.scamynx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.v7lthronyx.scamynx.ui.app.ScamynxAppRoot
import com.v7lthronyx.scamynx.ui.app.ScamynxAppViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: ScamynxAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScamynxAppRoot(viewModel = viewModel)
        }
    }
}
