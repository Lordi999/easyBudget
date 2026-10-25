package de.meinbudget.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import de.meinbudget.app.Entry
import de.meinbudget.app.R
import de.meinbudget.app.formatSigned
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ActionWidth = 88.dp
private val EditGrey = Color(0xFF757575)

/**
 * Buchungszeile mit Wisch-Aktionen:
 * nach rechts wischen → "Löschen" (links, rot), nach links wischen → "Bearbeiten" (rechts, grau).
 * [revealed] wird von außen gesteuert, damit immer nur eine Zeile offen ist.
 */
@Composable
fun SwipeableEntryRow(
    entry: Entry,
    revealed: Boolean,
    onRevealedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    val maxOffset = with(LocalDensity.current) { ActionWidth.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Wird eine andere Zeile geöffnet, schließt sich diese.
    LaunchedEffect(revealed) {
        if (!revealed && offset.value != 0f) offset.animateTo(0f)
    }

    fun close() {
        scope.launch { offset.animateTo(0f) }
        onRevealedChange(false)
    }

    Box(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Aktionen hinter der Zeile
        Row(Modifier.matchParentSize()) {
            if (offset.value > 0f) {
                SwipeAction(Icons.Default.Delete, stringResource(R.string.delete), DeleteRed) {
                    close()
                    onDelete()
                }
            }
            Spacer(Modifier.weight(1f))
            if (offset.value < 0f) {
                SwipeAction(Icons.Default.Edit, stringResource(R.string.edit), EditGrey) {
                    close()
                    onEdit()
                }
            }
        }

        // Die eigentliche Zeile, die verschoben wird
        Row(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .background(MaterialTheme.colorScheme.surface)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            offset.snapTo((offset.value + delta).coerceIn(-maxOffset, maxOffset))
                        }
                    },
                    onDragStarted = { onRevealedChange(true) },
                    onDragStopped = {
                        val target = when {
                            offset.value > maxOffset / 2 -> maxOffset
                            offset.value < -maxOffset / 2 -> -maxOffset
                            else -> 0f
                        }
                        offset.animateTo(target)
                        onRevealedChange(target != 0f)
                    },
                )
                .clickable(enabled = revealed) { close() }
                .padding(16.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                entry.text.ifBlank { stringResource(R.string.no_description) },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                formatSigned(entry.amountCents),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = amountColor(entry.amountCents),
            )
        }
    }
}

@Composable
private fun SwipeAction(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        Modifier
            .width(ActionWidth)
            .fillMaxHeight()
            .background(color)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}
