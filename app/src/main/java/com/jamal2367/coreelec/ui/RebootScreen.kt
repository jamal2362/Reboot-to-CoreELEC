package com.jamal2367.coreelec.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jamal2367.coreelec.R
import com.jamal2367.coreelec.RebootMode
import com.jamal2367.coreelec.RebootUiState

@Composable
fun RebootScreen(
    uiState: RebootUiState,
    onReboot: (RebootMode) -> Unit,
) {
    var infoFor by rememberSaveable { mutableStateOf<RebootMode?>(null) }
    val firstButton = remember { FocusRequester() }

    // Put D-pad focus on the main action at start and whenever the buttons become usable again.
    LaunchedEffect(uiState.isBusy) {
        if (!uiState.isBusy) firstButton.requestFocus()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Logo()

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(12.dp))
            Status(uiState)

            Spacer(Modifier.height(20.dp))
            Column(
                modifier = Modifier.widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RebootRow(
                    mode = RebootMode.NORMAL,
                    primary = true,
                    enabled = !uiState.isBusy,
                    onReboot = onReboot,
                    onInfo = { infoFor = it },
                    buttonModifier = Modifier.focusRequester(firstButton),
                )
                RebootRow(
                    mode = RebootMode.UPDATE,
                    primary = false,
                    enabled = !uiState.isBusy,
                    onReboot = onReboot,
                    onInfo = { infoFor = it },
                )
            }
        }
    }

    infoFor?.let { mode ->
        InfoDialog(mode = mode, onDismiss = { infoFor = null })
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Logo() {
    Box(
        modifier = Modifier
            .size(120.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialShapes.Cookie9Sided.toShape()),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.coreelec),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Status(uiState: RebootUiState) {
    AnimatedContent(
        targetState = uiState,
        modifier = Modifier.heightIn(min = 72.dp),
        transitionSpec = {
            (fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec(), initialScale = 0.9f)) togetherWith
                fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
        },
        contentAlignment = Alignment.Center,
        label = "status",
    ) { state ->
        Row(
            modifier = Modifier.widthIn(max = 480.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.isBusy) {
                LoadingIndicator(modifier = Modifier.size(48.dp))
            }

            val text = when (state) {
                RebootUiState.Idle -> ""
                RebootUiState.Connecting -> stringResource(R.string.status_connecting)
                is RebootUiState.Rebooting -> stringResource(state.mode.labelRes) + "…"
                is RebootUiState.Failed ->
                    if (state.messageArg != null) {
                        stringResource(state.messageRes, state.messageArg)
                    } else {
                        stringResource(state.messageRes)
                    }
            }
            if (text.isNotEmpty()) {
                Text(
                    text = text,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (state is RebootUiState.Failed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = if (state.isBusy) TextAlign.Start else TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RebootRow(
    mode: RebootMode,
    primary: Boolean,
    enabled: Boolean,
    onReboot: (RebootMode) -> Unit,
    onInfo: (RebootMode) -> Unit,
    buttonModifier: Modifier = Modifier,
) {
    val height = ButtonDefaults.MediumContainerHeight
    val label = stringResource(mode.labelRes)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val buttonSource = remember { MutableInteractionSource() }
        val modifier = buttonModifier
            .weight(1f)
            .heightIn(min = height)
        val content: @Composable () -> Unit = {
            Text(text = label, style = ButtonDefaults.textStyleFor(height))
        }

        if (primary) {
            Button(
                onClick = { onReboot(mode) },
                shapes = ButtonDefaults.shapesFor(height),
                modifier = modifier,
                enabled = enabled,
                border = focusBorder(buttonSource),
                contentPadding = ButtonDefaults.contentPaddingFor(height),
                interactionSource = buttonSource,
            ) { content() }
        } else {
            FilledTonalButton(
                onClick = { onReboot(mode) },
                shapes = ButtonDefaults.shapesFor(height),
                modifier = modifier,
                enabled = enabled,
                border = focusBorder(buttonSource),
                contentPadding = ButtonDefaults.contentPaddingFor(height),
                interactionSource = buttonSource,
            ) { content() }
        }

        val infoSource = remember { MutableInteractionSource() }
        val infoFocused by infoSource.collectIsFocusedAsState()
        FilledTonalIconButton(
            onClick = { onInfo(mode) },
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier.size(IconButtonDefaults.mediumContainerSize()),
            colors = if (infoFocused) {
                IconButtonDefaults.filledIconButtonColors()
            } else {
                IconButtonDefaults.filledTonalIconButtonColors()
            },
            interactionSource = infoSource,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info_24),
                contentDescription = stringResource(R.string.information) + ": " + label,
                modifier = Modifier.size(IconButtonDefaults.mediumIconSize),
            )
        }
    }
}

/** A clearly visible outline for D-pad focus; the default state layer is too subtle on a TV. */
@Composable
private fun focusBorder(source: MutableInteractionSource): BorderStroke? {
    val focused by source.collectIsFocusedAsState()
    return if (focused) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null
}

@Composable
private fun InfoDialog(mode: RebootMode, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
        icon = {
            Icon(painter = painterResource(R.drawable.ic_info_24), contentDescription = null)
        },
        title = { Text(stringResource(mode.labelRes)) },
        text = { Text(AnnotatedString.fromHtml(stringResource(mode.infoRes))) },
    )
}
