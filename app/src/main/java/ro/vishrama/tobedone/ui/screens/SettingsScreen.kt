@file:OptIn(ExperimentalMaterial3Api::class)

package ro.vishrama.tobedone.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.vishrama.tobedone.data.AppStore
import ro.vishrama.tobedone.data.ThemeSetting
import ro.vishrama.tobedone.ui.FormCard
import ro.vishrama.tobedone.ui.SectionHeader
import ro.vishrama.tobedone.ui.TextInput

@Composable
fun SettingsScreen(store: AppStore, padding: PaddingValues) {
    val theme by store.theme.collectAsStateWithLifecycle()
    val currency by store.currency.collectAsStateWithLifecycle()
    var currencyText by rememberSaveable { mutableStateOf(currency) }

    ScreenList(padding) {
        item { SectionHeader("Appearance") }
        item {
            FormCard {
                Text("Theme", style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeSetting.entries.forEachIndexed { index, t ->
                        SegmentedButton(
                            selected = t == theme,
                            onClick = { store.setTheme(t) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeSetting.entries.size),
                        ) { Text(t.label) }
                    }
                }
            }
        }

        item { SectionHeader("Shopping") }
        item {
            FormCard {
                TextInput(
                    value = currencyText,
                    onValueChange = {
                        currencyText = it.take(5).uppercase()
                        if (currencyText.isNotBlank()) store.setCurrency(currencyText)
                    },
                    label = "Currency for new items (e.g. RON, EUR, VND)",
                    onDone = { store.setCurrency(currencyText) },
                )
            }
        }
    }
}
