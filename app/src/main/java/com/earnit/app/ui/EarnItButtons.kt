package com.earnit.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PillShape = RoundedCornerShape(50)
private val ButtonShape = RoundedCornerShape(12.dp)

/** Standard primary action button — golden background, white text, caps style. */
@Composable
fun EarnItPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = ButtonShape,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, disabledElevation = 0.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFCCCCCC),
                disabledContentColor = Color(0xFF999999),
            ),
    ) {
        Text(text, style = buttonLabelStyle)
    }
}

/** Standard outlined cancel/secondary button — caps style. */
@Composable
fun EarnItOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = ButtonShape,
    ) {
        Text(text, style = buttonLabelStyle)
    }
}

/** Collapsible group header row — title + expand/collapse icon, optional leading slot for a checkbox. */
@Composable
internal fun CollapsibleGroupHeader(
    title: String,
    isCollapsed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingContent?.invoke()
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Consistent info toggle icon used in section headers and field labels. */
@Composable
fun InfoIconButton(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    val resolvedTint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.secondary else tint
    IconButton(onClick = onClick, modifier = modifier.size(24.dp)) {
        Icon(
            if (expanded) Icons.Default.Info else Icons.Outlined.Info,
            contentDescription = "Info",
            tint = resolvedTint,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** Primary-color CLAIM pill — used on reward cards. */
@Composable
fun ClaimPillButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .shadow(4.dp, PillShape)
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                null,
                modifier = Modifier.size(13.dp),
                tint = Color.White,
            )
            Text(
                "CLAIM",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
        }
    }
}

/** Full-row tappable radio option — label + wide tap target. */
@Composable
fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
        )
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Accent-colored pill for card actions (+ LOG, + ADD TASKS, etc.).
 * Disabled state renders in muted grey — same shape, no shadow.
 */
@Composable
fun LogPillButton(
    label: String,
    accentColor: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .shadow(elevation = if (enabled) 4.dp else 0.dp, shape = PillShape)
                .clip(PillShape)
                .background(
                    if (enabled) {
                        Brush.horizontalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.15f).compositeOver(MaterialTheme.colorScheme.surface),
                                accentColor.copy(alpha = 0.32f).compositeOver(MaterialTheme.colorScheme.surface),
                            ),
                        )
                    } else {
                        Brush.horizontalGradient(listOf(Color(0xFFEDE8DC), Color(0xFFE0DAD0)))
                    },
                ).border(1.5.dp, if (enabled) accentColor else Color(0xFFCEC8BC), PillShape)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.8.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (enabled) accentColor else Color(0xFFB0A898),
        )
    }
}

/**
 * Dismissible, one-time tip banner — accent-tinted card with a message, an optional action
 * slot, and a close button. Callers own persistence of the dismissed state.
 */
@Composable
fun DismissibleTipBanner(
    text: String,
    onDismiss: () -> Unit,
    dismissContentDescription: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                .padding(start = 14.dp, top = 10.dp, end = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (action != null) {
                Spacer(Modifier.height(8.dp))
                action()
            }
        }
        IconButton(onClick = onDismiss) {
            Icon(
                Icons.Default.Close,
                contentDescription = dismissContentDescription,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
