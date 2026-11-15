package com.jamal2367.coreelec.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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

/** Frames to wait for the command input line to take focus after OK. */
private const val FOCUS_ATTEMPTS = 10

/** How much of the screen height a TV on-screen keyboard covers, at least. */
private const val KEYBOARD_SCREEN_SHARE = 0.45f

/** Widest the terminal window gets; the update notice below it matches. */
private val TERMINAL_MAX_WIDTH = 380.dp

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
    onRunCommand: (String) -> Unit,
    log: List<LogLine>,
    onInstallUpdate: () -> Unit,
    developerOptionsEnabled: Boolean,
    onOpenDeveloperSettings: () -> Unit,
) {
    val firstAction = remember { FocusRequester() }
    val terminalFocus = remember { FocusRequester() }

    // Whether the last action came from the terminal: focus then returns there, not to the cards.
    var terminalInUse by remember { mutableStateOf(false) }

    // ADB is off (found at start or by a failed attempt): offer the way to switch it on,
    // as long as the developer options can be opened. An installable update comes first:
    // until it is installed, its notice is shown instead of the developer options button.
    val adbOff = adbStatus == AdbStatus.UNREACHABLE ||
        (uiState is RebootUiState.Failed && uiState.messageRes == R.string.error_adb_unreachable)
    val updateFirst = availableUpdate?.apkUrl != null
    val showDeveloperSettings = developerOptionsEnabled && !uiState.isBusy && adbOff && !updateFirst

    // Put D-pad focus on the main action at start and whenever the actions become usable again,
    // unless the developer options button is shown: that one takes focus itself.
    // The terminal only takes commands once ADB is reachable and trusts this app.
    val terminalEnabled = !uiState.isBusy && adbStatus == AdbStatus.READY

    // ADB stopped being ready (e.g. found off after a command) while the terminal was in use:
    // it can no longer hold focus, so hand it back to the main action.
    LaunchedEffect(adbStatus) {
        if (adbStatus != AdbStatus.READY && terminalInUse && !uiState.isBusy) {
            terminalInUse = false
            if (!showDeveloperSettings) firstAction.requestFocus()
        }
    }

    LaunchedEffect(uiState.isBusy) {
        if (!uiState.isBusy && !showDeveloperSettings) {
            if (terminalInUse && terminalEnabled) terminalFocus.requestFocus() else firstAction.requestFocus()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box {
            // The brand pane's color fills the whole row; the actions pane sits on top of it
            // with rounded corners at the top and bottom where the two halves meet.
            Row(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                BrandPane(
                    log = log,
                    terminalEnabled = terminalEnabled,
                    terminalFocus = terminalFocus,
                    onRunCommand = { command ->
                        terminalInUse = true
                        onRunCommand(command)
                    },
                    update = availableUpdate,
                    updateState = updateState,
                    onInstallUpdate = onInstallUpdate,
                    showDeveloperSettings = showDeveloperSettings,
                    onOpenDeveloperSettings = onOpenDeveloperSettings,
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.42f)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.exclude(WindowInsets.ime).only(WindowInsetsSides.Start + WindowInsetsSides.Vertical),
                        ),
                )
                ActionsPane(
                    enabled = !uiState.isBusy,
                    onReboot = { mode ->
                        terminalInUse = false
                        onReboot(mode)
                    },
                    firstAction = firstAction,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.background,
                            RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
                        )
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.exclude(WindowInsets.ime).only(WindowInsetsSides.End + WindowInsetsSides.Vertical),
                        )
                        .padding(horizontal = 48.dp, vertical = 24.dp),
                )
            }

            AdbStatusLine(
                status = adbStatus,
                deviceIp = deviceIp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.exclude(WindowInsets.ime).only(WindowInsetsSides.End + WindowInsetsSides.Bottom),
                    )
                    .padding(end = 48.dp, bottom = 24.dp),
            )
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
        // Same fill as the terminal window above it; lighter while focused.
        color = focusColor(focused, colors.background),
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
        color = colors.surfaceContainer,
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun BrandPane(
    log: List<LogLine>,
    terminalEnabled: Boolean,
    terminalFocus: FocusRequester,
    onRunCommand: (String) -> Unit,
    update: UpdateInfo?,
    updateState: UpdateUiState,
    onInstallUpdate: () -> Unit,
    showDeveloperSettings: Boolean,
    onOpenDeveloperSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val logoSize = 156.dp

    // While the on-screen keyboard is up (typing a command), the terminal has to end above it.
    // Some TV keyboards report a height of 0 while showing, so reserve at least the share of the
    // screen a TV keyboard usually covers.
    val imeVisible = WindowInsets.isImeVisible
    val density = LocalDensity.current
    val imeHeight = with(density) { WindowInsets.ime.getBottom(density).toDp() }
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val keyboardRoom by animateDpAsState(
        targetValue = if (imeVisible) maxOf(imeHeight, screenHeight * KEYBOARD_SCREEN_SHARE) else 0.dp,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "keyboardRoom",
    )

    // Logo and title sit centred between the top of the screen and the terminal; the terminal
    // takes the rest of the height and ends level with the ADB status box in the bottom right
    // corner (both 24 dp above the bottom).
    Column(
        modifier = modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp + keyboardRoom),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The shape behind the logo turns very slowly; the logo itself stays upright.
        val rotation by rememberInfiniteTransition(label = "logoShape").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(LOGO_SHAPE_TURN_MS, easing = LinearEasing)),
            label = "rotation",
        )
        // While the keyboard is up, logo and title fold away so the terminal keeps some height.
        val logoAreaHeight by animateDpAsState(
            // Folded away, it still leaves the usual 24 dp above the terminal.
            targetValue = if (imeVisible) 24.dp else TERMINAL_TOP,
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            label = "logoAreaHeight",
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(logoAreaHeight)
                .clipToBounds()
                .graphicsLayer { alpha = ((logoAreaHeight - 24.dp) / (TERMINAL_TOP - 24.dp)).coerceIn(0f, 1f) },
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

        Terminal(
            lines = log,
            enabled = terminalEnabled,
            focusRequester = terminalFocus,
            onRunCommand = onRunCommand,
            modifier = Modifier.weight(1f),
        )

        // Below the terminal, where the developer options button goes as well. Like that button,
        // without the invisible 48 dp touch margin, so its bottom edge lines up with the ADB box.
        if (update != null) {
            Spacer(Modifier.height(14.dp))
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                UpdateNotice(
                    update = update,
                    state = updateState,
                    onInstall = onInstallUpdate,
                    // As wide as the terminal above it.
                    modifier = Modifier
                        .widthIn(max = TERMINAL_MAX_WIDTH)
                        .fillMaxWidth(),
                )
            }
        }

        DeveloperSettings(visible = showDeveloperSettings, onClick = onOpenDeveloperSettings)
    }
}

/**
 * A small terminal window: the ADB steps scroll in from the bottom, under a "$ _" prompt.
 * Pressing OK on it opens the prompt for an ADB command of one's own (see [onRunCommand]).
 */
@Composable
private fun Terminal(
    lines: List<LogLine>,
    enabled: Boolean,
    focusRequester: FocusRequester,
    onRunCommand: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)

    var hasFocus by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    val inputFocus = remember { FocusRequester() }
    var inputHadFocus by remember { mutableStateOf(false) }
    var editRequest by remember { mutableIntStateOf(0) }
    val keyboard = LocalSoftwareKeyboardController.current

    fun stopEditing() {
        editing = false
        inputHadFocus = false
        focusRequester.requestFocus()
    }

    fun submit() {
        val command = input
        input = ""
        stopEditing()
        if (command.isNotBlank()) onRunCommand(command)
    }

    // OK on the terminal: move into the prompt line and bring up the on-screen keyboard. The
    // input field only exists from the next frame on, so try for a few frames; if it never
    // takes focus, leave the input mode instead of getting stuck in it.
    LaunchedEffect(editRequest) {
        if (editRequest == 0) return@LaunchedEffect
        listState.scrollToItem(0)
        repeat(FOCUS_ATTEMPTS) {
            withFrameNanos { }
            if (inputHadFocus) {
                keyboard?.show()
                return@LaunchedEffect
            }
            runCatching { inputFocus.requestFocus() }
        }
        withFrameNanos { }
        if (inputHadFocus) keyboard?.show() else editing = false
    }

    // While a command or a reboot runs, no new command can be typed.
    LaunchedEffect(enabled) {
        if (!enabled) editing = false
    }

    // Back leaves the prompt instead of closing the app.
    BackHandler(enabled = editing) { stopEditing() }

    val cursorAlpha by rememberInfiniteTransition(label = "cursor").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(TERMINAL_CURSOR_BLINK_MS, easing = { if (it < 0.5f) 0f else 1f })),
        label = "cursorAlpha",
    )

    Surface(
        onClick = {
            editing = true
            editRequest++
        },
        modifier = Modifier
            .widthIn(max = TERMINAL_MAX_WIDTH)
            .fillMaxWidth()
            .then(modifier)
            .focusRequester(focusRequester)
            .onFocusChanged { hasFocus = it.hasFocus }
            .focusScale(hasFocus),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        // Same as the background of the actions side on the right; the cookie color while focused.
        color = focusColor(hasFocus, colors.background),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            userScrollEnabled = false,
            // Laid out from the bottom: the prompt (item 0) is always in view and composed, also
            // when the height changes with the keyboard; short logs still start at the top.
            reverseLayout = true,
            verticalArrangement = Arrangement.Top,
        ) {
            // The prompt: "$ _" with a blinking cursor, or the input line after OK.
            item {
                Row {
                    Text(
                        text = "$",
                        modifier = Modifier.width(TERMINAL_MARKER_WIDTH),
                        style = textStyle,
                        color = colors.onSurface,
                    )
                    when {
                        editing -> BasicTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(inputFocus)
                                .onFocusChanged {
                                    // Leaving the line with the D-pad ends the input.
                                    if (it.isFocused) {
                                        inputHadFocus = true
                                    } else if (inputHadFocus) {
                                        inputHadFocus = false
                                        editing = false
                                    }
                                },
                            singleLine = true,
                            textStyle = textStyle.copy(color = colors.onSurface),
                            cursorBrush = SolidColor(colors.onSurface),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrectEnabled = false,
                                keyboardType = KeyboardType.Ascii,
                                imeAction = ImeAction.Go,
                            ),
                            keyboardActions = KeyboardActions(onGo = { submit() }, onDone = { submit() }),
                        )
                        else -> Text(
                            text = "_",
                            modifier = Modifier.alpha(cursorAlpha),
                            style = textStyle,
                            color = colors.onSurface,
                        )
                    }
                }
            }
            items(lines.asReversed()) { line ->
                val text = line.text ?: stringResource(line.textRes, *line.args.toTypedArray())
                val color = when (line.kind) {
                    LogKind.COMMAND, LogKind.INFO, LogKind.HINT -> colors.onSurface
                    LogKind.OUTPUT -> colors.onSurfaceVariant
                    LogKind.SUCCESS -> StatusGreen
                    LogKind.WARNING -> colors.error
                    LogKind.ERROR -> StatusRed
                }
                // The marker sits in its own column, so wrapped lines stay indented under the text.
                Row {
                    Text(
                        text = when (line.kind) {
                            LogKind.COMMAND -> "$"
                            LogKind.OUTPUT -> ""
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
        }
    }
}

/** The way to the developer options, below the terminal, only when ADB was found switched off. */
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
        )
    }
}

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
        color = focusColor(focused, colors.surfaceContainer),
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
@Composable
private fun focusColor(focused: Boolean, base: Color): Color {
    val color by animateColorAsState(
        targetValue = if (focused) FocusContainer else base,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "focusColor",
    )
    return color
}

