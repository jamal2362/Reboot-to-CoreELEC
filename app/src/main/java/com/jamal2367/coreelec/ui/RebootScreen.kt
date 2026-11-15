package com.jamal2367.coreelec.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jamal2367.coreelec.AdbStatus
import com.jamal2367.coreelec.LogKind
import com.jamal2367.coreelec.LogLine
import com.jamal2367.coreelec.R
import com.jamal2367.coreelec.RebootMode
import com.jamal2367.coreelec.RebootUiState
import com.jamal2367.coreelec.UpdateChecker
import com.jamal2367.coreelec.UpdateInfo
import com.jamal2367.coreelec.UpdateUiState

/** Duration of one full turn of the shape behind the logo. */
private const val LOGO_SHAPE_TURN_MS = 60_000
private const val TERMINAL_CURSOR_BLINK_MS = 1_000

/** How much a focused button grows. */
private const val FOCUS_SCALE = 1.03f

/** Where the terminal window starts, measured from the top of the screen. */
private val TERMINAL_TOP = 339.dp

/** Column for the line markers ($, ✓, i, …), about two monospace characters wide. */
private val TERMINAL_MARKER_WIDTH = 16.dp


@Composable
fun RebootScreen(
    uiState: RebootUiState,
    adbStatus: AdbStatus,
    deviceIp: String?,
    availableUpdate: UpdateInfo?,
    updateState: UpdateUiState,
    onReboot: (RebootMode) -> Unit,
    log: List<LogLine>,
    onInstallUpdate: () -> Unit,
    developerOptionsEnabled: Boolean,
    onOpenDeveloperSettings: () -> Unit,
) {
    val firstAction = remember { FocusRequester() }
    val secondAction = remember { FocusRequester() }

    // ADB is off (found at start or by a failed attempt): offer the way to switch it on,
    // as long as the developer options can be opened.
    val adbOff = adbStatus == AdbStatus.UNREACHABLE ||
        (uiState is RebootUiState.Failed && uiState.messageRes == R.string.error_adb_unreachable)
    val showDeveloperSettings = developerOptionsEnabled && !uiState.isBusy && adbOff

    // Put D-pad focus on the main action at start and whenever the actions become usable again,
    // unless the developer options button is shown: that one takes focus itself.
    LaunchedEffect(uiState.isBusy) {
        if (!uiState.isBusy && !showDeveloperSettings) firstAction.requestFocus()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box {
            // The brand pane's colour fills the whole row; the actions pane sits on top of it
            // with rounded corners at the top and bottom where the two halves meet.
            Row(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                BrandPane(
                    log = log,
                    showDeveloperSettings = showDeveloperSettings,
                    onOpenDeveloperSettings = onOpenDeveloperSettings,
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.42f)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical),
                        ),
                )
                ActionsPane(
                    enabled = !uiState.isBusy,
                    onReboot = onReboot,
                    firstAction = firstAction,
                    secondAction = secondAction,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.background,
                            RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
                        )
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.End + WindowInsetsSides.Vertical),
                        )
                        .padding(horizontal = 48.dp, vertical = 24.dp),
                )
            }

            // Bottom right: the update notice (if any) above the ADB status line.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.End + WindowInsetsSides.Bottom),
                    )
                    .padding(end = 48.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (availableUpdate != null) {
                    UpdateNotice(
                        update = availableUpdate,
                        state = updateState,
                        onInstall = onInstallUpdate,
                        // The cards' scrolling column reaches down behind this notice, so the
                        // D-pad search does not see the cards as "above" it: point Up at them.
                        modifier = Modifier.focusProperties { up = secondAction },
                    )
                }
                AdbStatusLine(status = adbStatus, deviceIp = deviceIp)
            }
        }
    }
}

@Composable
private fun UpdateNotice(
    update: UpdateInfo,
    state: UpdateUiState,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val canInstall = update.apkUrl != null
    val focusRequester = remember { FocusRequester() }

    // A new version is the most interesting thing on screen: OK should install it right away.
    LaunchedEffect(canInstall) {
        if (canInstall) focusRequester.requestFocus()
    }

    Surface(
        onClick = onInstall,
        modifier = modifier
            .focusRequester(focusRequester)
            .focusScale(focused),
        enabled = canInstall,
        shape = RoundedCornerShape(20.dp),
        // Same fill as the reboot cards and the ADB status box; lighter while focused.
        color = focusColor(focused, colors.surfaceContainerLow),
        contentColor = colors.onSurface,
        interactionSource = source,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painter = painterResource(R.drawable.ic_first_boot_24), contentDescription = null)
            Column {
                Text(
                    text = stringResource(R.string.update_available, update.version),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = when {
                        !canInstall -> UpdateChecker.RELEASES_PAGE
                        state is UpdateUiState.Downloading && state.percent != null ->
                            stringResource(R.string.update_downloading_percent, state.percent)
                        state is UpdateUiState.Downloading -> stringResource(R.string.update_downloading)
                        state is UpdateUiState.Installing -> stringResource(R.string.update_installing)
                        state is UpdateUiState.Failed ->
                            stringResource(R.string.update_failed, state.message.orEmpty())
                        else -> stringResource(R.string.update_press_to_install)
                    },
                    modifier = Modifier.widthIn(max = 360.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state is UpdateUiState.Failed) {
                        colors.error
                    } else {
                        colors.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun AdbStatusLine(status: AdbStatus, deviceIp: String?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val label = stringResource(
        when (status) {
            AdbStatus.CHECKING -> R.string.adb_status_checking
            AdbStatus.READY -> R.string.adb_status_ready
            AdbStatus.NEEDS_AUTH -> R.string.adb_status_needs_auth
            AdbStatus.UNREACHABLE -> R.string.adb_status_unreachable
        },
    )

    // Same fill as the reboot cards (e.g. "First reboot") when they are not focused.
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(
                        color = when (status) {
                            AdbStatus.CHECKING -> colors.outline
                            AdbStatus.READY -> StatusGreen
                            AdbStatus.NEEDS_AUTH -> colors.error
                            AdbStatus.UNREACHABLE -> StatusRed
                        },
                        shape = CircleShape,
                    ),
            )
            Text(
                text = if (deviceIp != null) "$label • $deviceIp" else label,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BrandPane(
    log: List<LogLine>,
    showDeveloperSettings: Boolean,
    onOpenDeveloperSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val logoSize = 156.dp

    // Logo and title sit centred between the top of the screen and the terminal; the terminal
    // takes the rest of the height and ends level with the ADB status box in the bottom right
    // corner (both 24 dp above the bottom).
    Column(
        modifier = modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The shape behind the logo turns very slowly; the logo itself stays upright.
        val rotation by rememberInfiniteTransition(label = "logoShape").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(LOGO_SHAPE_TURN_MS, easing = LinearEasing)),
            label = "rotation",
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TERMINAL_TOP),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(logoSize), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .graphicsLayer { rotationZ = rotation }
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                MaterialShapes.Cookie9Sided.toShape(),
                            ),
                    )
                    Image(
                        painter = painterResource(R.drawable.coreelec),
                        contentDescription = null,
                        modifier = Modifier.size(logoSize * 0.64f),
                    )
                }

                Spacer(Modifier.height(15.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 27.sp, lineHeight = 34.sp),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Terminal(log, modifier = Modifier.weight(1f))

        DeveloperSettings(visible = showDeveloperSettings, onClick = onOpenDeveloperSettings)
    }
}

/** A small terminal window: the ADB steps scroll in from the bottom, under a blinking cursor. */
@Composable
private fun Terminal(lines: List<LogLine>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)

    // Keep the newest line (and the cursor after it) in view.
    LaunchedEffect(lines.size) {
        listState.animateScrollToItem(lines.size)
    }

    val cursorAlpha by rememberInfiniteTransition(label = "cursor").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(TERMINAL_CURSOR_BLINK_MS, easing = { if (it < 0.5f) 0f else 1f })),
        label = "cursorAlpha",
    )

    Surface(
        modifier = Modifier
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .then(modifier),
        shape = RoundedCornerShape(16.dp),
        // Same as the background of the actions side on the right.
        color = colors.background,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            userScrollEnabled = false,
        ) {
            items(lines) { line ->
                val text = line.text ?: stringResource(line.textRes, *line.args.toTypedArray())
                val color = when (line.kind) {
                    LogKind.COMMAND, LogKind.INFO, LogKind.HINT -> colors.onSurface
                    LogKind.SUCCESS -> StatusGreen
                    LogKind.WARNING -> colors.error
                    LogKind.ERROR -> StatusRed
                }
                // The marker sits in its own column, so wrapped lines stay indented under the text.
                Row {
                    Text(
                        text = when (line.kind) {
                            LogKind.COMMAND -> "$"
                            LogKind.SUCCESS -> "✓"
                            LogKind.WARNING -> "!"
                            LogKind.ERROR -> "✗"
                            LogKind.INFO, LogKind.HINT -> "i"
                        },
                        modifier = Modifier.width(TERMINAL_MARKER_WIDTH),
                        style = textStyle,
                        color = color,
                    )
                    Text(text = text, style = textStyle, color = color)
                }
            }
            item {
                Text(
                    text = "_",
                    modifier = Modifier.alpha(cursorAlpha),
                    style = textStyle,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

/** The way to the developer options, below the terminal, only when ADB was found switched off. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DeveloperSettings(visible: Boolean, onClick: () -> Unit) {
    val motion = MaterialTheme.motionScheme
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(motion.defaultEffectsSpec()) + expandVertically(motion.defaultSpatialSpec()),
        exit = fadeOut(motion.fastEffectsSpec()) + shrinkVertically(motion.fastSpatialSpec()),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(14.dp))
            // Without the invisible 48 dp touch margin, so the button's visible bottom edge
            // lines up with the ADB status box in the bottom right corner.
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                DeveloperSettingsButton(onClick = onClick)
            }
        }
    }
}

@Composable
private fun DeveloperSettingsButton(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }

    // Shown after a failed attempt: opening the settings is the obvious next step.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusScale(focused),
        shape = RoundedCornerShape(20.dp),
        // Same fill as the turning cookie shape behind the logo; lighter while focused.
        color = focusColor(focused, colors.surfaceContainerHigh),
        contentColor = colors.onSurface,
        interactionSource = source,
    ) {
        Text(
            text = stringResource(R.string.open_developer_settings),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun ActionsPane(
    enabled: Boolean,
    onReboot: (RebootMode) -> Unit,
    firstAction: FocusRequester,
    secondAction: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
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
            modifier = Modifier.focusRequester(secondAction),
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
            .focusScale(focused)
            .alpha(if (enabled) 1f else 0.5f),
        enabled = enabled,
        shape = RoundedCornerShape(corner),
        color = focusColor(focused, colors.surfaceContainerLow),
        contentColor = colors.onSurface,
        interactionSource = source,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(if (primary) CoreELECRed else colors.surfaceContainerHighest, CircleShape),
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

/**
 * D-pad focus on a TV: the focused button grows a little (like on the Google TV home screen).
 * Goes together with [focusColor]; there is no outline.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Modifier.focusScale(focused: Boolean): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (focused) FOCUS_SCALE else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "focusScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** The fill of a button: [base] normally, [FocusContainer] while focused. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun focusColor(focused: Boolean, base: Color): Color {
    val color by animateColorAsState(
        targetValue = if (focused) FocusContainer else base,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "focusColor",
    )
    return color
}

