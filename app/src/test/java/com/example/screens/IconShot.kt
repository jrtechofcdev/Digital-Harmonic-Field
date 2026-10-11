package com.example.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.BrandMark
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Confere o ícone como o Android desenha (vetores reais), em várias máscaras e tamanhos. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w420dp-h420dp-xxhdpi")
class IconShot {
    @get:Rule val rule = createComposeRule()

    @Composable
    private fun Adaptive(size: Dp, shape: Shape, mono: Boolean = false) {
        // A camada tem 108 dp; o lançador mostra só os 72 do meio.
        Box(Modifier.size(size).clip(shape).background(if (mono) Color(0xFF2B3036) else Color.Black)) {
            Box(Modifier.size(size).graphicsLayer(scaleX = 1.5f, scaleY = 1.5f)) {
                if (mono) {
                    Image(painterResource(R.mipmap.ic_launcher_monochrome), null, Modifier.size(size), colorFilter = ColorFilter.tint(Color(0xFFCFE3FF)))
                } else {
                    Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.size(size))
                    Image(painterResource(R.mipmap.ic_launcher_foreground), null, Modifier.size(size))
                }
            }
        }
    }

    @Test fun icon() {
        rule.setContent {
            Column(Modifier.fillMaxSize().background(Color(0xFF3A3F47)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Adaptive(180.dp, CircleShape)
                    Adaptive(180.dp, RoundedCornerShape(30))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Adaptive(64.dp, CircleShape)
                    Adaptive(48.dp, CircleShape)
                    Adaptive(64.dp, RoundedCornerShape(30), mono = true)
                    Box(Modifier.size(64.dp).background(Color(0xFF0B0C0F))) { BrandMark(size = 64.dp) }
                    Box(Modifier.size(64.dp).background(Color(0xFF0B0C0F)), contentAlignment = androidx.compose.ui.Alignment.Center) { BrandMark(size = 24.dp) }
                }
            }
        }
        rule.onRoot().captureRoboImage("build/shots/icone.png")
    }
}
