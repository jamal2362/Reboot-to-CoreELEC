package com.jamal2367.coreelec

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jamal2367.coreelec.ui.AppTheme

/** Debug-only: shows the logo inside every Material shape to pick one. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class ShapeGalleryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val shapes = listOf(
            "Circle" to MaterialShapes.Circle, "Square" to MaterialShapes.Square,
            "Slanted" to MaterialShapes.Slanted, "Arch" to MaterialShapes.Arch,
            "Fan" to MaterialShapes.Fan, "Arrow" to MaterialShapes.Arrow,
            "SemiCircle" to MaterialShapes.SemiCircle, "Oval" to MaterialShapes.Oval,
            "Pill" to MaterialShapes.Pill, "Triangle" to MaterialShapes.Triangle,
            "Diamond" to MaterialShapes.Diamond, "ClamShell" to MaterialShapes.ClamShell,
            "Pentagon" to MaterialShapes.Pentagon, "Gem" to MaterialShapes.Gem,
            "Sunny" to MaterialShapes.Sunny, "VerySunny" to MaterialShapes.VerySunny,
            "Cookie4Sided" to MaterialShapes.Cookie4Sided, "Cookie6Sided" to MaterialShapes.Cookie6Sided,
            "Cookie7Sided" to MaterialShapes.Cookie7Sided, "Cookie9Sided" to MaterialShapes.Cookie9Sided,
            "Cookie12Sided" to MaterialShapes.Cookie12Sided, "Ghostish" to MaterialShapes.Ghostish,
            "Clover4Leaf" to MaterialShapes.Clover4Leaf, "Clover8Leaf" to MaterialShapes.Clover8Leaf,
            "Burst" to MaterialShapes.Burst, "SoftBurst" to MaterialShapes.SoftBurst,
            "Boom" to MaterialShapes.Boom, "SoftBoom" to MaterialShapes.SoftBoom,
            "Flower" to MaterialShapes.Flower, "Puffy" to MaterialShapes.Puffy,
            "PuffyDiamond" to MaterialShapes.PuffyDiamond, "PixelCircle" to MaterialShapes.PixelCircle,
            "PixelTriangle" to MaterialShapes.PixelTriangle, "Bun" to MaterialShapes.Bun,
            "Heart" to MaterialShapes.Heart,
        )

        setContent {
            AppTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    shapes.chunked(7).forEach { row ->
                        Row(Modifier.fillMaxSize().weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { (name, polygon) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, polygon.toShape()),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Image(
                                            painter = painterResource(R.drawable.coreelec),
                                            contentDescription = null,
                                            modifier = Modifier.size(46.dp),
                                        )
                                    }
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
