package com.example.zenjiro74.irowokiku.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.zenjiro74.irowokiku.R
import com.example.zenjiro74.irowokiku.about.OssEntry
import com.example.zenjiro74.irowokiku.about.OssLicense
import com.example.zenjiro74.irowokiku.about.OssLicenses

@Composable
fun LicensesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val entries = remember { OssLicenses.load(context.assets) }

    // 全文表示は画面回転やプロセス復帰をまたいでも維持したいので saveable な id で持つ。
    var shownLicenseId by rememberSaveable { mutableStateOf<String?>(null) }
    val shownLicense = entries.firstOrNull { it.license.id == shownLicenseId }?.license

    when {
        shownLicense != null -> LicenseTextScreen(
            license = shownLicense,
            onBack = { shownLicenseId = null },
            modifier = modifier,
        )

        else -> AboutScaffold(
            title = stringResource(R.string.screen_licenses),
            onBack = onBack,
            modifier = modifier,
        ) { contentModifier ->
            LazyColumn(
                modifier = contentModifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.licenses_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                items(entries, key = { it.project }) { entry ->
                    LicenseCard(
                        entry = entry,
                        onShowText = { shownLicenseId = entry.license.id },
                    )
                }
            }
        }
    }
}

@Composable
private fun LicenseCard(entry: OssEntry, onShowText: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = entry.project,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = entry.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Text(
                text = stringResource(R.string.licenses_artifact_count, entry.artifacts.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            Text(
                text = entry.license.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.licenses_show_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onShowText)
                    .padding(vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun LicenseTextScreen(
    license: OssLicense,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val text = remember(license) { OssLicenses.readLicenseText(context.assets, license) }

    AboutScaffold(title = license.name, onBack = onBack, modifier = modifier) { contentModifier ->
        Column(
            modifier = contentModifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = text,
                // ライセンス全文は桁揃えされた原文なので等幅で、折り返さず横スクロールさせる。
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface,
                softWrap = false,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(20.dp),
            )
        }
    }
}
