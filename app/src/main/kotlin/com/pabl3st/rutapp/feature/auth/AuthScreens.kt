package com.pabl3st.rutapp.feature.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.core.ui.theme.Spacing

// ── Auth Root ──────────────────────────────────────────────────
@Composable
fun AuthRoot(
    onAuthenticated: () -> Unit,
    onExitApp: () -> Unit,           // callback para salir de la app
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    // Autenticación exitosa → navegar a Home
    LaunchedEffect(ui.isAuthenticated) {
        if (ui.isAuthenticated) onAuthenticated()
    }

    // BackHandler global del flujo auth — intercepta SIEMPRE el botón de sistema
    // handleBack() decide qué hacer según la pantalla actual
    BackHandler(enabled = true) {
        val handled = viewModel.handleBack()
        if (!handled) onExitApp()
    }

    // Diálogo: ¿Seguro que quieres salir de la app?
    if (ui.showExitDialog) {
        ExitAppDialog(
            onConfirm = {
                viewModel.onExitConfirmed()
                onExitApp()
            },
            onDismiss = viewModel::onExitDismissed,
        )
    }

    // Diálogo: ¿Descartar cambios del formulario?
    if (ui.showDiscardDialog) {
        DiscardChangesDialog(
            onConfirm = viewModel::onDiscardConfirmed,
            onDismiss = viewModel::onDiscardDismissed,
        )
    }

    AnimatedContent(
        targetState   = ui.screen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label          = "auth_screen",
    ) { screen ->
        when (screen) {
            AuthScreen.SPLASH              -> SplashScreen()
            AuthScreen.CHOOSE_TYPE         -> ChooseTypeScreen(
                onIndividual = viewModel::onChooseIndividual,
                onCompany    = viewModel::onChooseCompany,
                onLogin      = viewModel::onGoToLogin,
            )
            AuthScreen.LOGIN               -> LoginScreen(
                ui               = ui,
                onUsernameChange = viewModel::onUsernameChange,
                onPasswordChange = viewModel::onPasswordChange,
                onTogglePassword = viewModel::onTogglePassword,
                onLogin          = viewModel::login,
                onBack           = { viewModel.handleBack() },
            )
            AuthScreen.REGISTER_INDIVIDUAL -> RegisterIndividualScreen(
                ui               = ui,
                onNameChange     = viewModel::onNameChange,
                onUsernameChange = viewModel::onUsernameChange,
                onEmailChange    = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onTogglePassword = viewModel::onTogglePassword,
                onRegister       = viewModel::registerIndividual,
                onBack           = { viewModel.handleBack() },
                onGoToLogin      = viewModel::onGoToLogin,
            )
            AuthScreen.REGISTER_COMPANY    -> RegisterCompanyScreen(
                ui                  = ui,
                onCompanyNameChange = viewModel::onCompanyNameChange,
                onNameChange        = viewModel::onNameChange,
                onUsernameChange    = viewModel::onUsernameChange,
                onEmailChange       = viewModel::onEmailChange,
                onPasswordChange    = viewModel::onPasswordChange,
                onTogglePassword    = viewModel::onTogglePassword,
                onRegister          = viewModel::registerCompany,
                onBack              = { viewModel.handleBack() },
                onGoToLogin         = viewModel::onGoToLogin,
            )
        }
    }
}

// ── Diálogos ────────────────────────────────────────────────────

@Composable
fun ExitAppDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon    = null,
        title   = { Text("¿Salir de RutasApp?") },
        text    = { Text("¿Seguro que quieres cerrar la aplicación?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Salir", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
fun DiscardChangesDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("¿Descartar cambios?") },
        text    = { Text("Los datos introducidos se perderán.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Descartar", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Continuar editando") }
        },
    )
}

// ── Splash ──────────────────────────────────────────────────────
@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

// ── Choose Type ─────────────────────────────────────────────────
@Composable
private fun ChooseTypeScreen(
    onIndividual: () -> Unit,
    onCompany: () -> Unit,
    onLogin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text  = "RutasApp",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "Gestión profesional de rutas de campo",
            style     = MaterialTheme.typography.bodyLarge,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(56.dp))

        Text(
            text  = "¿Cómo quieres usar RutasApp?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.lg))

        TypeCard(
            icon        = Icons.Outlined.Person,
            title       = "Uso personal",
            description = "Para comerciales o agentes independientes. Solo tú, sin equipo.",
            onClick     = onIndividual,
        )
        Spacer(Modifier.height(Spacing.md))
        TypeCard(
            icon        = Icons.Outlined.Business,
            title       = "Para mi empresa",
            description = "Gestiona un equipo de comerciales con distintos roles y permisos.",
            onClick     = onCompany,
        )
        Spacer(Modifier.height(Spacing.xl))

        TextButton(onClick = onLogin) {
            Text("Ya tengo cuenta — Iniciar sesión")
        }
    }
}

@Composable
private fun TypeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = title },
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier          = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(Spacing.md))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Login ────────────────────────────────────────────────────────
@Composable
private fun LoginScreen(
    ui: AuthUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onLogin: () -> Unit,
    onBack: () -> Unit,
) {
    val focus = LocalFocusManager.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Iniciar sesión") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(Spacing.xl))

            AuthTextField(
                value         = ui.username,
                onValueChange = onUsernameChange,
                label         = "Usuario o email",
                keyboardType  = KeyboardType.Email,
                imeAction     = ImeAction.Next,
                onNext        = { focus.moveFocus(FocusDirection.Down) },
            )
            Spacer(Modifier.height(Spacing.md))
            PasswordField(
                value         = ui.password,
                onValueChange = onPasswordChange,
                visible       = ui.passwordVisible,
                onToggle      = onTogglePassword,
                imeAction     = ImeAction.Done,
                onDone        = onLogin,
            )

            ErrorText(ui.error)
            Spacer(Modifier.height(Spacing.lg))

            LoadingButton(
                text     = "Entrar",
                loading  = ui.isLoading,
                onClick  = onLogin,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

// ── Register Individual ──────────────────────────────────────────
@Composable
private fun RegisterIndividualScreen(
    ui: AuthUiState,
    onNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit,
    onGoToLogin: () -> Unit,
) {
    val focus = LocalFocusManager.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cuenta personal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(Spacing.sm))
            Text("Para comerciales independientes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Spacing.xl))

            AuthTextField(value = ui.name, onValueChange = onNameChange,
                label = "Nombre completo", capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next, onNext = { focus.moveFocus(FocusDirection.Down) })
            Spacer(Modifier.height(Spacing.md))
            AuthTextField(value = ui.username, onValueChange = onUsernameChange,
                label = "Nombre de usuario",
                imeAction = ImeAction.Next, onNext = { focus.moveFocus(FocusDirection.Down) })
            Spacer(Modifier.height(Spacing.md))
            AuthTextField(value = ui.email, onValueChange = onEmailChange,
                label = "Email", keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next, onNext = { focus.moveFocus(FocusDirection.Down) })
            Spacer(Modifier.height(Spacing.md))
            PasswordField(value = ui.password, onValueChange = onPasswordChange,
                visible = ui.passwordVisible, onToggle = onTogglePassword,
                imeAction = ImeAction.Done, onDone = onRegister)

            ErrorText(ui.error)
            Spacer(Modifier.height(Spacing.lg))

            LoadingButton(text = "Crear cuenta", loading = ui.isLoading,
                onClick = onRegister, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.md))
            TextButton(onClick = onGoToLogin) { Text("Ya tengo cuenta") }
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

// ── Register Company ─────────────────────────────────────────────
@Composable
private fun RegisterCompanyScreen(
    ui: AuthUiState,
    onCompanyNameChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit,
    onGoToLogin: () -> Unit,
) {
    val focus = LocalFocusManager.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cuenta de empresa") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(Spacing.sm))
            Text("Para gestores de equipos comerciales",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Spacing.xl))

            AuthTextField(value = ui.companyName, onValueChange = onCompanyNameChange,
                label = "Nombre de la empresa", capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next, onNext = { focus.moveFocus(FocusDirection.Down) })
            Spacer(Modifier.height(Spacing.md))

            HorizontalDivider()
            Spacer(Modifier.height(Spacing.sm))
            Text("Tu cuenta de administrador",
                style    = MaterialTheme.typography.labelLarge,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.sm))

            AuthTextField(value = ui.name, onValueChange = onNameChange,
                label = "Tu nombre completo", capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next, onNext = { focus.moveFocus(FocusDirection.Down) })
            Spacer(Modifier.height(Spacing.md))
            AuthTextField(value = ui.username, onValueChange = onUsernameChange,
                label = "Nombre de usuario",
                imeAction = ImeAction.Next, onNext = { focus.moveFocus(FocusDirection.Down) })
            Spacer(Modifier.height(Spacing.md))
            AuthTextField(value = ui.email, onValueChange = onEmailChange,
                label = "Email", keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next, onNext = { focus.moveFocus(FocusDirection.Down) })
            Spacer(Modifier.height(Spacing.md))
            PasswordField(value = ui.password, onValueChange = onPasswordChange,
                visible = ui.passwordVisible, onToggle = onTogglePassword,
                imeAction = ImeAction.Done, onDone = onRegister)

            ErrorText(ui.error)
            Spacer(Modifier.height(Spacing.lg))

            LoadingButton(text = "Crear empresa", loading = ui.isLoading,
                onClick = onRegister, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.md))
            TextButton(onClick = onGoToLogin) { Text("Ya tengo cuenta") }
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

// ── Componentes compartidos ──────────────────────────────────────

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    onNext: () -> Unit = {},
) {
    OutlinedTextField(
        value           = value,
        onValueChange   = onValueChange,
        label           = { Text(label) },
        singleLine      = true,
        modifier        = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType   = keyboardType,
            capitalization = capitalization,
            imeAction      = imeAction,
        ),
        keyboardActions = KeyboardActions(onNext = { onNext() }, onDone = { onNext() }),
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggle: () -> Unit,
    imeAction: ImeAction = ImeAction.Done,
    onDone: () -> Unit = {},
) {
    OutlinedTextField(
        value                = value,
        onValueChange        = onValueChange,
        label                = { Text("Contraseña") },
        singleLine           = true,
        modifier             = Modifier.fillMaxWidth(),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon         = {
            IconButton(
                onClick  = onToggle,
                modifier = Modifier.semantics {
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña"
                }
            ) {
                Icon(
                    if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = null,
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
    )
}

@Composable
private fun ErrorText(error: String?) {
    if (error != null) {
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text     = error,
            color    = MaterialTheme.colorScheme.error,
            style    = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Error: $error" },
        )
    }
}

@Composable
private fun LoadingButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick  = onClick,
        enabled  = !loading,
        modifier = modifier.height(Spacing.touchMin),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(20.dp),
                color       = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
