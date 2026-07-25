package com.example.equalizersoftware.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
object Widgets {
    
    @Composable
    fun VerticalThermometerBar(
        value: Float,
        onValueChange: (Float) -> Unit,
        label: String,
        modifier: Modifier = Modifier
    ) {
        var currentValue by remember { mutableStateOf(value) }
        
        Column(
            modifier = modifier
                .width(70.dp)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(label, fontSize = 9.sp, color = Color.White)
            
            // Glass tube - empty with color fill from bottom
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(120.dp)
                    .background(
                        color = Color(0xFF0A0A0A),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(2.dp, Color(0xFF666666), RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            // Drag up increases value, drag down decreases
                            val heightPx = size.height.toFloat()
                            val delta = -dragAmount.y / heightPx // negative because Y increases downward
                            val newValue = (currentValue + delta).coerceIn(0f, 1f)
                            currentValue = newValue
                            onValueChange(newValue)
                            change.consume()
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val heightPx = size.height
                            // Map tap position: top = max, bottom = min
                            val newValue = ((heightPx - offset.y) / heightPx).coerceIn(0f, 1f)
                            currentValue = newValue
                            onValueChange(newValue)
                        }
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                // Color fill from bottom (higher value = more fill)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(currentValue)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0078FF),
                                    Color(0xFFFF00FF),
                                    Color(0xFFFF0000)
                                )
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                )
            }
            
            Text("${(currentValue * 15).toInt()}dB", fontSize = 9.sp, color = Color(0xFFFFFF00))
        }
    }
    
    @Composable
    fun HorizontalThermometerBar(
        value: Float,
        onValueChange: (Float) -> Unit,
        label: String,
        modifier: Modifier = Modifier
    ) {
        var currentValue by remember { mutableStateOf(value) }
        
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontSize = 12.sp, color = Color.White, modifier = Modifier.width(120.dp))
                Text("${(currentValue * 100).toInt()}%", fontSize = 11.sp, color = Color(0xFFFFFF00))
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(
                        color = Color(0xFF0A0A0A),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(2.dp, Color(0xFF666666), RoundedCornerShape(14.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            // Drag right increases value
                            val widthPx = size.width.toFloat()
                            val delta = dragAmount.x / widthPx
                            val newValue = (currentValue + delta).coerceIn(0f, 1f)
                            currentValue = newValue
                            onValueChange(newValue)
                            change.consume()
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val widthPx = size.width
                            val newValue = (offset.x / widthPx).coerceIn(0f, 1f)
                            currentValue = newValue
                            onValueChange(newValue)
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                // Color fill from left
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(currentValue)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0078FF),
                                    Color(0xFFFFFF00),
                                    Color(0xFFFF0000)
                                )
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                )
            }
        }
    }
    
    @Composable
    fun StereoPairControl(
        leftValue: Float,
        onLeftChange: (Float) -> Unit,
        rightValue: Float,
        onRightChange: (Float) -> Unit,
        modifier: Modifier = Modifier
    ) {
        var leftCurrentValue by remember { mutableStateOf(leftValue) }
        var rightCurrentValue by remember { mutableStateOf(rightValue) }
        
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("◄ LEFT", fontSize = 12.sp, color = Color(0xFF0078FF), modifier = Modifier.width(80.dp))
                Text("${(leftCurrentValue * 30).toInt()}dB", fontSize = 11.sp, color = Color(0xFFFFFF00))
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(
                        color = Color(0xFF0A0A0A),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(2.dp, Color(0xFF666666), RoundedCornerShape(14.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            val widthPx = size.width.toFloat()
                            val delta = dragAmount.x / widthPx
                            val newValue = (leftCurrentValue + delta).coerceIn(0f, 1f)
                            leftCurrentValue = newValue
                            onLeftChange(newValue)
                            change.consume()
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val widthPx = size.width
                            val newValue = (offset.x / widthPx).coerceIn(0f, 1f)
                            leftCurrentValue = newValue
                            onLeftChange(newValue)
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(leftCurrentValue)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0078FF),
                                    Color(0xFFFFFF00),
                                    Color(0xFFFF0000)
                                )
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("RIGHT ►", fontSize = 12.sp, color = Color(0xFFFF0000), modifier = Modifier.width(80.dp))
                Text("${(rightCurrentValue * 30).toInt()}dB", fontSize = 11.sp, color = Color(0xFFFFFF00))
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(
                        color = Color(0xFF0A0A0A),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(2.dp, Color(0xFF666666), RoundedCornerShape(14.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            val widthPx = size.width.toFloat()
                            val delta = dragAmount.x / widthPx
                            val newValue = (rightCurrentValue + delta).coerceIn(0f, 1f)
                            rightCurrentValue = newValue
                            onRightChange(newValue)
                            change.consume()
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val widthPx = size.width
                            val newValue = (offset.x / widthPx).coerceIn(0f, 1f)
                            rightCurrentValue = newValue
                            onRightChange(newValue)
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(rightCurrentValue)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0078FF),
                                    Color(0xFFFFFF00),
                                    Color(0xFFFF0000)
                                )
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                )
            }
        }
    }
}
