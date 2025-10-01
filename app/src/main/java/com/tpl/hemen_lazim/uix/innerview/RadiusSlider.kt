package com.tpl.hemen_lazim.uix.innerview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RadiusSlider(
    radiusKm: Float,
    onRadiusChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    minKm: Float = 0.1f,
    maxKm: Float = 10f
) {
    var sliderWidth by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    
    // Local dragging offset - maintain during drag
    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableStateOf(0f) }
    
    // Calculate progress from radius (only when not dragging)
    val progress = if (isDragging) {
        (dragOffsetX / sliderWidth).coerceIn(0f, 1f)
    } else {
        ((radiusKm - minKm) / (maxKm - minKm)).coerceIn(0f, 1f)
    }
    
    // Update dragOffset when radius changes externally (and we're not dragging)
    LaunchedEffect(radiusKm, sliderWidth) {
        if (!isDragging && sliderWidth > 0) {
            dragOffsetX = sliderWidth * ((radiusKm - minKm) / (maxKm - minKm)).coerceIn(0f, 1f)
        }
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title and current value
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Arama Yarıçapı",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = String.format("%.1f km", radiusKm),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Red,
                fontSize = 22.sp
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Slider track with tick marks
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            // Tick marks and track
            Canvas(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight()
                    .onSizeChanged { size ->
                        if (sliderWidth == 0f && size.width > 0) {
                            sliderWidth = size.width.toFloat()
                            // Initialize drag offset
                            dragOffsetX = sliderWidth * ((radiusKm - minKm) / (maxKm - minKm)).coerceIn(0f, 1f)
                        }
                    }
            ) {
                val trackY = size.height / 2
                val trackWidth = size.width
                
                // Draw main track line
                drawLine(
                    color = Color.LightGray,
                    start = Offset(0f, trackY),
                    end = Offset(trackWidth, trackY),
                    strokeWidth = 6.dp.toPx()
                )
                
                // Draw active portion (from start to current position)
                val activeWidth = trackWidth * progress
                drawLine(
                    color = Color.Red.copy(alpha = 0.3f),
                    start = Offset(0f, trackY),
                    end = Offset(activeWidth, trackY),
                    strokeWidth = 6.dp.toPx()
                )
                
                // Draw vertical tick marks
                val numberOfTicks = 20
                for (i in 0..numberOfTicks) {
                    val tickX = (trackWidth / numberOfTicks) * i
                    val isMajorTick = i % 5 == 0
                    
                    val tickHeight = if (isMajorTick) 20.dp.toPx() else 12.dp.toPx()
                    val tickColor = if (isMajorTick) Color.Black else Color.Gray
                    val strokeWidth = if (isMajorTick) 2.5f else 1.5f
                    
                    drawLine(
                        color = tickColor,
                        start = Offset(tickX, trackY - tickHeight / 2),
                        end = Offset(tickX, trackY + tickHeight / 2),
                        strokeWidth = strokeWidth
                    )
                }
            }
            
            // Red draggable dot
            if (sliderWidth > 0) {
                val dotOffsetX = sliderWidth * progress * 0.92f
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = with(density) { (dotOffsetX - 22.dp.toPx()).toDp() })
                            .size(44.dp)
                            .shadow(12.dp, CircleShape)
                            .background(Color.Red, CircleShape)
                            .pointerInput(sliderWidth) {
                                detectDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        
                                        if (sliderWidth > 0) {
                                            // Update local drag offset
                                            dragOffsetX = (dragOffsetX + dragAmount.x).coerceIn(0f, sliderWidth)
                                            
                                            // Calculate new radius from offset
                                            val newProgress = (dragOffsetX / sliderWidth).coerceIn(0f, 1f)
                                            val newRadius = minKm + (newProgress * (maxKm - minKm))
                                            
                                            // Round to 1 decimal place
                                            val roundedRadius = (newRadius * 10).toInt() / 10f
                                            
                                            // Update parent immediately
                                            onRadiusChange(roundedRadius.coerceIn(minKm, maxKm))
                                        }
                                    }
                                )
                            }
                            .align(Alignment.CenterStart)
                    ) {
                        // White center dot for better visibility
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Color.White, CircleShape)
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Min and max labels with intermediate values
        Row(
            modifier = Modifier.fillMaxWidth(0.92f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${minKm}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "2.5",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontSize = 11.sp
            )
            Text(
                text = "5.0",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontSize = 11.sp
            )
            Text(
                text = "7.5",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontSize = 11.sp
            )
            Text(
                text = "${maxKm.toInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Hint text
        Text(
            text = "Kırmızı noktayı sürükleyerek arama alanını ayarlayın",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}