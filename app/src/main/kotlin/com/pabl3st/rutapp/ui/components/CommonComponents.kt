package com.pabl3st.rutapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Snackbar estándar para errores de ViewModel. */
@Composable
fun ErrorSnackbarEffect(
    error:             String?,
    snackbarHostState: SnackbarHostState,
    onClear:           () -> Unit,
) {
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            onClear()
        }
    }
}

/** Estado vacío estándar con icono, título, subtítulo y acción opcional. */
@Composable
fun EmptyStateBox(
    icon:        ImageVector,
    title:       String,
    subtitle:    String?      = null,
    actionLabel: String?      = null,
    onAction:    (() -> Unit)? = null,
    modifier:    Modifier     = Modifier.fillMaxSize(),
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier            = Modifier.padding(horizontal = 40.dp),
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                modifier           = Modifier.size(56.dp),
                tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
            Text(title,
                style     = MaterialTheme.typography.titleSmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
            subtitle?.let {
                Text(it,
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center)
            }
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                FilledTonalButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}
