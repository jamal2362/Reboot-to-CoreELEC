package com.jamal2367.coreelec.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jamal2367.coreelec.R
import com.jamal2367.coreelec.RebootMode
import com.jamal2367.coreelec.RebootUiState

@Composable
fun RebootScreen(
    uiState: RebootUiState,
    onReboot: (RebootMode) -> Unit,
) {
    val firstAction = remember { FocusRequester() }

    // Put D-pad focus on the main action at start and whenever the actions become usable again.
    LaunchedEffect(uiState.isBusy) {
        if (!uiState.isBusy) firstAction.requestFocus()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints {
            val wide = maxWidth >= 600.dp && maxWidth > maxHeight
            val brandColor = MaterialTheme.colorScheme.surfaceContainerLow

            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    BrandPane(
                        uiState = uiState,
                        logoSize = 170.dp,
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.42f)
                            .background(brandColor)
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical),
                            ),
                    )
                    ActionsPane(
                        enabled = !uiState.isBusy,
                        onReboot = onReboot,
                        firstAction = firstAction,
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.End + WindowInsetsSides.Vertical),
                            )
                            .padding(horizontal = 48.dp, vertical = 24.dp),
                    )
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    BrandPane(
                        uiState = uiState,
                        logoSize = 120.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.44f)
                            .background(brandColor)
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                            ),
                    )
                    ActionsPane(
                        enabled = !uiState.isBusy,
                        onReboot = onReboot,
                        firstAction = firstAction,
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                            )
                            .padding(20.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BrandPane(uiState: RebootUiState, logoSize: Dp, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(logoSize)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialShapes.Cookie12Sided.toShape()),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.coreelec),
                contentDescription = null,
                modifier = Modifier.size(logoSize * 0.64f),
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(10.dp))
        Status(uiState)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Status(uiState: RebootUiState) {
    val motion = MaterialTheme.motionScheme
    val enter = fadeIn(motion.defaultEffectsSpec()) +
        scaleIn(motion.defaultSpatialSpec(), initialScale = 0.9f)
    val exit = fadeOut(motion.fastEffectsSpec())

    AnimatedContent(
        targetState = uiState,
        transitionSpec = { enter togetherWith exit },
        contentAlignment = Alignment.Center,
        label = "status",
    ) { state ->
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.isBusy) {
                LoadingIndicator(modifier = Modifier.size(40.dp))
            }

            Text(
                text = when (state) {
                    RebootUiState.Idle -> stringResource(R.string.status_ready)
                    RebootUiState.Connecting -> stringResource(R.string.status_connecting)
                    is RebootUiState.Rebooting -> stringResource(state.mode.labelRes) + "…"
                    is RebootUiState.Failed ->
                        if (state.messageArg != null) {
                            stringResource(state.messageRes, state.messageArg)
                        } else {
                            stringResource(state.messageRes)
                        }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (state is RebootUiState.Failed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ActionsPane(
    enabled: Boolean,
    onReboot: (RebootMode) -> Unit,
    firstAction: FocusRequester,
    verticalArrangement: Arrangement.Vertical,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = verticalArrangement,
    ) {
        Text(
            text = stringResource(R.string.choose_reboot).uppercase(),
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        RebootAction(
            mode = RebootMode.NORMAL,
            primary = true,
            enabled = enabled,
            onReboot = onReboot,
            modifier = Modifier.focusRequester(firstAction),
        )
        RebootAction(
            mode = RebootMode.UPDATE,
            primary = false,
            enabled = enabled,
            onReboot = onReboot,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RebootAction(
    mode: RebootMode,
    primary: Boolean,
    enabled: Boolean,
    onReboot: (RebootMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val pressed by source.collectIsPressedAsState()
    // Expressive shape change: the card tightens its corners while pressed.
    val corner by animateDpAsState(
        targetValue = if (pressed) 12.dp else 24.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "corner",
    )

    Surface(
        onClick = { onReboot(mode) },
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        enabled = enabled,
        shape = RoundedCornerShape(corner),
        color = if (primary) colors.surfaceContainerHighest else colors.surfaceContainer,
        contentColor = colors.onSurface,
        // A clearly visible outline for D-pad focus; the default state layer is too subtle on a TV.
        border = if (focused) BorderStroke(2.dp, colors.onSurface) else null,
        interactionSource = source,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(if (primary) CoreElecRed else colors.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(mode.iconRes),
                    contentDescription = null,
                    tint = if (primary) Color.White else colors.onSurfaceVariant,
                )
            }

            Spacer(Modifier.width(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(mode.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(mode.descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}
