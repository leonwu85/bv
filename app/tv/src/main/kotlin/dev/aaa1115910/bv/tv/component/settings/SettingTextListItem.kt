package dev.aaa1115910.bv.tv.component.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.component.TvAlertDialog

@Composable
fun SettingTextListItem(
    modifier: Modifier = Modifier,
    title: String,
    supportText: String,
    value: String,
    emptyValueText: String = "默认",
    placeholder: String = "",
    transformValue: (String) -> String = { it.trim().replace("\n", "") },
    onValueChange: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    SettingListItem(
        modifier = modifier,
        title = title,
        supportText = supportText,
        valueText = value.ifBlank { emptyValueText },
        onClick = { showDialog = true }
    )

    SettingTextEditDialog(
        show = showDialog,
        title = title,
        value = value,
        placeholder = placeholder,
        onHideDialog = { showDialog = false },
        onValueChange = { nextValue ->
            onValueChange(transformValue(nextValue))
        }
    )
}

@Composable
private fun SettingTextEditDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String,
    value: String,
    placeholder: String,
    onHideDialog: () -> Unit,
    onValueChange: (String) -> Unit
) {
    var input by remember(show) { mutableStateOf(value) }

    if (show) {
        TvAlertDialog(
            modifier = modifier,
            title = { Text(text = title) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        singleLine = true,
                        maxLines = 1,
                        shape = MaterialTheme.shapes.medium,
                        placeholder = if (placeholder.isNotBlank()) {
                            { Text(text = placeholder) }
                        } else {
                            null
                        }
                    )
                }
            },
            onDismissRequest = onHideDialog,
            confirmButton = {
                Button(onClick = {
                    onValueChange(input)
                    onHideDialog()
                }) {
                    Text(text = stringResource(id = R.string.common_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onHideDialog) {
                    Text(text = stringResource(id = R.string.common_cancel))
                }
            }
        )
    }
}
