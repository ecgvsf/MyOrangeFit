package com.example.myorangefit.compose

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.vicoTheme
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.Defaults
import com.patrykandpatrick.vico.core.common.shape.Shape

@SuppressLint("RestrictedApi")
@Composable
fun ComposeBarChart(
    data: List<Pair<String, Int>>,
    color: Color = Color(0xFF42A5F5)
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(data) {
        modelProducer.runTransaction {
            columnSeries { series(data.map { it.second }) }
        }
    }

    // Valori custom (puoi modificarli a piacere)
    val barWidth = 24.dp
    val barRoundness = 100 // percentuale

    val columnLayer = rememberColumnCartesianLayer(
        columnProvider = ColumnCartesianLayer.ColumnProvider.series(
            vicoTheme.columnCartesianLayerColors.map { color ->
                rememberLineComponent(
                    color = color,
                    thickness = Defaults.COLUMN_WIDTH.dp,
                    shape = Shape.rounded(barRoundness)
                )
            }
        )
    )

    val startAxis = rememberStartAxis()
    val bottomAxis = rememberBottomAxis(
        valueFormatter = CartesianValueFormatter.decimal()
    )

    CartesianChartHost(
        chart = rememberCartesianChart(
            columnLayer,
            startAxis = startAxis,
            bottomAxis = bottomAxis
        ),
        modelProducer = modelProducer,
    )
}

@Preview
@Composable
private fun VerticalBarChartPreview() {
    ComposeBarChart(
        data = listOf(
            "Lun" to 4,
            "Mar" to 2,
            "Mer" to 6,
            "Gio" to 3,
            "Ven" to 5,
            "Sab" to 1,
            "Dom" to 4
        )
    )
}
