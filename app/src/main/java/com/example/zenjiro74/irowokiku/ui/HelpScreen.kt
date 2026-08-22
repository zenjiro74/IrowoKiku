package com.example.zenjiro74.irowokiku.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.zenjiro74.irowokiku.R

@Composable
fun HelpScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    AboutScaffold(
        title = stringResource(R.string.screen_help),
        onBack = onBack,
        modifier = modifier,
    ) { contentModifier ->
        Column(
            modifier = contentModifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = stringResource(R.string.help_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            HelpSection(R.string.help_usage_title, R.string.help_usage_body)
            HelpSection(R.string.help_readout_title, R.string.help_readout_body)
            HelpSection(R.string.help_lock_title, R.string.help_lock_body)
            HelpSection(R.string.help_dark_title, R.string.help_dark_body)
            HelpSection(R.string.help_metric_title, R.string.help_metric_body)
        }
    }
}

@Composable
private fun HelpSection(titleRes: Int, bodyRes: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
