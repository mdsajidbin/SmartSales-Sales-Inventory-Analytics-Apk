package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CategorySalesPoint
import com.example.model.MonthlySalesPoint
import com.example.model.StockStatusSummary
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun MonthlySalesChart(
    dataPoints: List<MonthlySalesPoint>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_sales_chart"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sales Trend & Revenue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SmartTextPrimary
                )
                Text(
                    text = "Revenue ($)",
                    style = MaterialTheme.typography.labelSmall,
                    color = SmartTextSecondary
                )
            }

            if (dataPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sales recorded in this period",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartTextMuted
                    )
                }
            } else {
                val maxRevenue = (dataPoints.maxOfOrNull { it.revenue } ?: 100.0).coerceAtLeast(10.0)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val barCount = dataPoints.size
                        val spacing = canvasWidth / (barCount + 1)
                        val barWidth = (spacing * 0.55f).coerceIn(12f, 36f)

                        // Draw baseline
                        drawLine(
                            color = SmartCardBorder,
                            start = Offset(0f, canvasHeight),
                            end = Offset(canvasWidth, canvasHeight),
                            strokeWidth = 2f
                        )

                        // Draw bars & line connectors
                        val points = mutableListOf<Offset>()

                        dataPoints.forEachIndexed { index, point ->
                            val x = spacing * (index + 1)
                            val normalizedHeight = ((point.revenue / maxRevenue) * (canvasHeight - 24f)).toFloat()
                            val y = canvasHeight - normalizedHeight

                            points.add(Offset(x, y))

                            // Draw Bar with rounded top
                            val barBrush = Brush.verticalGradient(
                                colors = listOf(SmartIndigo, SmartCyan),
                                startY = y,
                                endY = canvasHeight
                            )

                            drawRoundRect(
                                brush = barBrush,
                                topLeft = Offset(x - barWidth / 2, y),
                                size = Size(barWidth, normalizedHeight.coerceAtLeast(4f)),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                            )
                        }

                        // Draw trend line across tops
                        if (points.size > 1) {
                            val path = Path().apply {
                                moveTo(points.first().x, points.first().y)
                                for (i in 1 until points.size) {
                                    lineTo(points[i].x, points[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = SmartIndigoDark,
                                style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                            )
                        }

                        // Draw dots at point tops
                        points.forEach { pt ->
                            drawCircle(
                                color = Color.White,
                                radius = 5f,
                                center = pt
                            )
                            drawCircle(
                                color = SmartIndigo,
                                radius = 3.5f,
                                center = pt
                            )
                        }
                    }
                }

                // X-Axis Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    dataPoints.forEach { point ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = point.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = SmartTextSecondary,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "$${point.revenue.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SmartIndigo,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryDistributionChart(
    categoryPoints: List<CategorySalesPoint>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_distribution_chart"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Sales by Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SmartTextPrimary
            )

            if (categoryPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No category sales recorded yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartTextMuted
                    )
                }
            } else {
                val colors = listOf(
                    SmartIndigo,
                    SmartCyan,
                    Color(0xFF8B5CF6),
                    StatusGreen,
                    StatusAmber,
                    Color(0xFFEC4899)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Donut Chart
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            val totalPercent = categoryPoints.sumOf { it.percentage.toDouble() }.toFloat().coerceAtLeast(1f)

                            categoryPoints.forEachIndexed { index, point ->
                                val sweepAngle = (point.percentage / totalPercent) * 360f
                                val color = colors[index % colors.size]

                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 24f)
                                )
                                startAngle += sweepAngle
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${categoryPoints.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SmartTextPrimary
                            )
                            Text(
                                text = "Categories",
                                style = MaterialTheme.typography.labelSmall,
                                color = SmartTextSecondary,
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Legend Column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryPoints.take(4).forEachIndexed { index, point ->
                            val color = colors[index % colors.size]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Text(
                                        text = point.categoryName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = SmartTextPrimary,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", point.percentage)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SmartTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StockStatusBarChart(
    stockSummary: StockStatusSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stock_status_chart"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Inventory Stock Health",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SmartTextPrimary
                )
                Text(
                    text = "${stockSummary.totalCount} Products",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SmartIndigo
                )
            }

            // Segmented Progress Bar
            val total = stockSummary.totalCount.coerceAtLeast(1).toFloat()
            val inStockWeight = stockSummary.inStockCount / total
            val lowStockWeight = stockSummary.lowStockCount / total
            val outOfStockWeight = stockSummary.outOfStockCount / total

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(SmartCardBorder)
            ) {
                if (inStockWeight > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(inStockWeight)
                            .fillMaxHeight()
                            .background(StatusGreen)
                    )
                }
                if (lowStockWeight > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(lowStockWeight)
                            .fillMaxHeight()
                            .background(StatusAmber)
                    )
                }
                if (outOfStockWeight > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(outOfStockWeight)
                            .fillMaxHeight()
                            .background(StatusRed)
                    )
                }
            }

            // Legend counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StockLegendItem(
                    label = "In Stock",
                    count = stockSummary.inStockCount,
                    color = StatusGreen,
                    bgColor = StatusGreenBg
                )
                StockLegendItem(
                    label = "Low Stock",
                    count = stockSummary.lowStockCount,
                    color = StatusAmber,
                    bgColor = StatusAmberBg
                )
                StockLegendItem(
                    label = "Out of Stock",
                    count = stockSummary.outOfStockCount,
                    color = StatusRed,
                    bgColor = StatusRedBg
                )
            }
        }
    }
}

@Composable
private fun StockLegendItem(
    label: String,
    count: Int,
    color: Color,
    bgColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = "$label: $count",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 12.sp
            )
        }
    }
}
