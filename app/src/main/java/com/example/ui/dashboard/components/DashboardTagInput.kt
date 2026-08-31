package com.example.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardTagInput(
    tags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    label: String = "ট্যাগসমূহ (Tags)",
    placeholder: String = "ট্যাগ লিখুন এবং কমা, স্পেস বা Enter চাপুন...",
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }

    fun addTag(raw: String) {
        val cleaned = raw.trim().trim(',', ' ')
        if (cleaned.isNotBlank() && !tags.contains(cleaned)) {
            onTagsChange(tags + cleaned)
        }
        textInput = ""
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = textInput,
            onValueChange = { newValue ->
                if (newValue.endsWith(",") || newValue.endsWith("  ")) {
                    addTag(newValue)
                } else {
                    textInput = newValue
                }
            },
            label = { Text(label) },
            placeholder = { Text(placeholder, fontSize = 12.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (textInput.isNotBlank()) {
                    IconButton(onClick = { addTag(textInput) }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Tag",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { addTag(textInput) }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dashboard_input_tags")
        )

        if (tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tags.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = { },
                        label = {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove $tag",
                                modifier = Modifier
                                    .size(14.dp)
                                    .testTag("dashboard_remove_tag_$tag"),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
