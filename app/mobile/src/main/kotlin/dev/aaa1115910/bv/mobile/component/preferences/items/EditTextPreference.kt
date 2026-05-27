package dev.aaa1115910.bv.mobile.component.preferences.items

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.schnettler.datastore.manager.DataStoreManager
import de.schnettler.datastore.manager.PreferenceRequest
import dev.aaa1115910.bv.dataStore
import dev.aaa1115910.bv.mobile.component.preferences.PreferenceGroupScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
private fun EditTextPreference(
    modifier: Modifier = Modifier,
    title: String,
    summary: String?,
    value: String,
    shape: Shape = RoundedCornerShape(0.dp),
    enabled: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf(value) }

    BaseListItem(
        modifier = modifier,
        headlineContent = { Text(text = title) },
        supportingContent = summary?.let { { Text(text = it) } },
        enabled = enabled,
        onClick = {
            input = value
            showDialog = true
        },
        shape = shape
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = title) },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueChange(input)
                        showDialog = false
                    }
                ) {
                    Text(text = "确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(text = "取消")
                }
            }
        )
    }
}

fun PreferenceGroupScope.editTextPreference(
    title: String,
    prefReq: PreferenceRequest<String>,
    emptySummary: String = "未设置",
    summary: ((String) -> String)? = null,
    transformValue: (String) -> String = { it.trim() },
) {
    preferences += { shape, modifier ->
        val scope = rememberCoroutineScope()
        val dataStoreManager = DataStoreManager(LocalContext.current.dataStore)
        val value by dataStoreManager.getPreferenceState(prefReq)
        val displaySummary = summary?.invoke(value)
            ?: value.takeIf { it.isNotBlank() }
            ?: emptySummary

        EditTextPreference(
            modifier = modifier,
            title = title,
            summary = displaySummary,
            value = value,
            shape = shape,
            onValueChange = { newValue ->
                scope.launch(Dispatchers.IO) {
                    dataStoreManager.editPreference(prefReq.key, transformValue(newValue))
                }
            }
        )
    }
}

