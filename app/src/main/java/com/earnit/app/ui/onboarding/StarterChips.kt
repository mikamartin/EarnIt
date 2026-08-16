package com.earnit.app.ui.onboarding

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.earnit.app.ui.Strings

/**
 * Bigger-ticket reward starter chips shown while the reward-name field is still empty — they
 * disappear once the user types (or taps one), so there's no separate dismiss control needed.
 * Laid out as a fixed 2x2 grid (not a scrolling row) so all four are visible at once — a
 * horizontally scrolling row left most of them off-screen with no hint that more existed.
 */
@Composable
fun StarterChipsRow(
    onChipSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chips =
        remember {
            listOf(
                Strings.ONBOARDING_STARTER_CHIP_1,
                Strings.ONBOARDING_STARTER_CHIP_2,
                Strings.ONBOARDING_STARTER_CHIP_3,
                Strings.ONBOARDING_STARTER_CHIP_4,
            )
        }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.chunked(2).forEach { rowChips ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowChips.forEach { chip ->
                    StarterChip(chip, onClick = { onChipSelected(chip) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StarterChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}
