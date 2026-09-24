package com.nhowe.ember.ui.goals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.nhowe.ember.domain.model.ResolvedGoal
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalActionsSheet(
    goal: ResolvedGoal,
    date: LocalDate,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onSkip: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                "${goal.version.emoji}  ${goal.version.title}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(4.dp))
            ListItem(
                headlineContent = { Text("Edit goal") },
                supportingContent = { Text("Changes apply from today; past days keep their record") },
                leadingContent = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
            )
            ListItem(
                headlineContent = { Text("Skip for ${if (date == LocalDate.now()) "today" else date}") },
                supportingContent = { Text("Removed from this day only, no penalty") },
                leadingContent = { Icon(Icons.Rounded.SkipNext, contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onSkip),
            )
        }
    }
}
