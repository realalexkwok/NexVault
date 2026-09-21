package com.nexvault.wallet.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nexvault.wallet.core.ui.preview.ThemePreviewWrapper
import com.nexvault.wallet.core.ui.theme.NexVaultDimens
import com.nexvault.wallet.core.ui.theme.NexVaultTheme
import com.nexvault.wallet.domain.model.token.PricePoint

/**
 * Minimal line chart for price or portfolio history.
 *
 * @param dataPoints At least two points are required; otherwise nothing is drawn.
 * @param lineColor Stroke color for the line.
 */
@Composable
fun SimpleLineChart(
    dataPoints: List<PricePoint>,
    modifier: Modifier = Modifier,
    lineColor: Color,
) {
    if (dataPoints.size < 2) return

    val minValue = dataPoints.minOf { it.value }
    val maxValue = dataPoints.maxOf { it.value }
    val valueRange = (maxValue - minValue).coerceAtLeast(0.01)

    Canvas(modifier = modifier.padding(NexVaultDimens.spacingSm)) {
        val stepX = size.width / (dataPoints.size - 1)
        val path = Path()
        dataPoints.forEachIndexed { index, point ->
            val x = index * stepX
            val y = size.height - ((point.value - minValue) / valueRange * size.height).toFloat()
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = NexVaultDimens.borderWidthStrong.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SimpleLineChartPreview() {
    ThemePreviewWrapper {
        SimpleLineChart(
            dataPoints =
                listOf(
                    PricePoint(timestamp = 1_700_000_000_000, value = 100.0),
                    PricePoint(timestamp = 1_700_086_400_000, value = 112.5),
                    PricePoint(timestamp = 1_700_172_800_000, value = 108.25),
                    PricePoint(timestamp = 1_700_259_200_000, value = 121.75),
                ),
            modifier = Modifier.size(width = 240.dp, height = 96.dp),
            lineColor = NexVaultTheme.colors.positive,
        )
    }
}
