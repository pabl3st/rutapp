@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
package com.pabl3st.rutapp.feature.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.core.ui.theme.RutasColors
import com.pabl3st.rutapp.core.ui.theme.Spacing
import kotlin.math.cos
import kotlin.math.sin

// ── Paleta local ──────────────────────────────────────────────
// Auth es siempre dark — referencias directas a RutasColors (no dependen del theme)
private val CyanGlow    = RutasColors.Cyan400
private val CyanDim     = RutasColors.CyanLight
private val DarkBg      = RutasColors.Dark950
private val DarkCard    = RutasColors.Dark800
private val DarkBorder  = RutasColors.Dark700
private val DarkBorder2 = RutasColors.Dark600
private val TextPrimary = RutasColors.TextDark100
private val TextMuted   = RutasColors.TextDark400
private val TextSub     = RutasColors.TextDark200

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AuthRoot(
    onAuthenticated: () -> Unit,
    onExitApp: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    LaunchedEffect(ui.isAuthenticated) {
        if (ui.isAuthenticated) onAuthenticated()
    }

    BackHandler(enabled = true) {
        val handled = viewModel.handleBack()
        if (!handled) onExitApp()
    }

    if (ui.showExitDialog) {
        AuthDialog(
            title   = "¿Salir de RutasApp?",
            body    = "¿Seguro que quieres cerrar la aplicación?",
            confirm = "Salir",
            onConfirm = { viewModel.onExitConfirmed(); onExitApp() },
            onDismiss = viewModel::onExitDismissed,
        )
    }

    if (ui.showDiscardDialog) {
        AuthDialog(
            title   = "¿Descartar cambios?",
            body    = "Los datos introducidos se perderán.",
            confirm = "Descartar",
            onConfirm = viewModel::onDiscardConfirmed,
            onDismiss = viewModel::onDiscardDismissed,
        )
    }

    // ── Contenedor principal con fondo Field Pro Dark ─────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .drawBehind { drawDotGrid(CyanGlow.copy(alpha = 0.06f)) }
            .systemBarsPadding(),
    ) {
        AnimatedContent(
            targetState    = ui.screen,
            transitionSpec = {
                val enter = fadeIn(tween(280)) + slideInVertically(tween(280, easing = EaseOutCubic)) { it / 12 }
                val exit  = fadeOut(tween(180))
                enter togetherWith exit
            },
            label = "auth_phase",
        ) { screen ->
            when (screen) {
                AuthScreen.SPLASH              -> PhaseLoading()
                AuthScreen.CHOOSE_TYPE         -> PhaseChoose(
                    onLogin      = viewModel::onGoToLogin,
                    onIndividual = viewModel::onChooseIndividual,
                    onCompany    = viewModel::onChooseCompany,
                )
                AuthScreen.LOGIN               -> PhaseLogin(
                    ui               = ui,
                    onUsernameChange = viewModel::onUsernameChange,
                    onPasswordChange = viewModel::onPasswordChange,
                    onTogglePassword = viewModel::onTogglePassword,
                    onLogin          = viewModel::login,
                    onBack           = { viewModel.handleBack() },
                )
                AuthScreen.REGISTER_INDIVIDUAL -> PhaseRegisterIndividual(
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
                AuthScreen.REGISTER_COMPANY    -> PhaseRegisterCompany(
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
}

// ══════════════════════════════════════════════════════════════
// FASE 0 — SPLASH / VERIFICANDO SESIÓN
// ══════════════════════════════════════════════════════════════
@Composable
private fun PhaseLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppLogo(size = 56.dp)
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(
                color       = CyanGlow,
                strokeWidth = 2.dp,
                modifier    = Modifier.size(20.dp),
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// FASE 1 — ELEGIR TIPO DE CUENTA
// ══════════════════════════════════════════════════════════════
@Composable
private fun PhaseChoose(
    onLogin:      () -> Unit,
    onIndividual: () -> Unit,
    onCompany:    () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppLogo(size = 48.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            "RutasApp",
            color      = TextPrimary,
            fontSize   = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        )
        Text(
            "Gestión de rutas de campo",
            color    = TextMuted,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(52.dp))

        // Tipo: Personal
        ChooseTypeCard(
            icon        = Icons.Outlined.Person,
            title       = "Uso personal",
            description = "Comercial o agente independiente",
            onClick     = onIndividual,
        )
        Spacer(Modifier.height(10.dp))

        // Tipo: Empresa
        ChooseTypeCard(
            icon        = Icons.Outlined.Business,
            title       = "Para mi empresa",
            description = "Equipo de comerciales con roles",
            accent      = true,
            onClick     = onCompany,
        )

        Spacer(Modifier.height(40.dp))

        // Divider con texto
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            HorizontalDivider(Modifier.weight(1f), color = DarkBorder2)
            Text("  o  ", color = TextMuted, fontSize = 12.sp)
            HorizontalDivider(Modifier.weight(1f), color = DarkBorder2)
        }

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape  = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                width = 1.dp,
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TextSub,
            ),
        ) {
            Text("Ya tengo cuenta — Iniciar sesión", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ChooseTypeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    val borderColor = if (accent) CyanGlow.copy(alpha = 0.6f) else DarkBorder
    val bgColor     = if (accent) CyanGlow.copy(alpha = 0.05f) else DarkCard

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (accent) CyanGlow.copy(0.15f) else DarkBorder),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(20.dp),
                tint = if (accent) CyanGlow else TextSub)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = TextMuted, fontSize = 12.sp)
        }
        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(16.dp),
            tint = if (accent) CyanGlow else TextMuted)
    }
}

// ══════════════════════════════════════════════════════════════
// FASE 2 — LOGIN
// ══════════════════════════════════════════════════════════════
@Composable
private fun PhaseLogin(
    ui: AuthUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onLogin: () -> Unit,
    onBack: () -> Unit,
) {
    val focus = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        PhaseHeader(
            title    = "Bienvenido",
            subtitle = "Accede con tu usuario o email",
            onBack   = onBack,
        )
        Spacer(Modifier.height(36.dp))

        DarkTextField(
            value         = ui.username,
            onValueChange = onUsernameChange,
            label         = "Usuario o email",
            keyboardType  = KeyboardType.Email,
            imeAction     = ImeAction.Next,
            onNext        = { focus.moveFocus(FocusDirection.Down) },
        )
        Spacer(Modifier.height(12.dp))
        DarkPasswordField(
            value         = ui.password,
            onValueChange = onPasswordChange,
            visible       = ui.passwordVisible,
            onToggle      = onTogglePassword,
            imeAction     = ImeAction.Done,
            onDone        = onLogin,
        )

        AnimatedError(ui.error)
        Spacer(Modifier.height(28.dp))

        CyanButton("Entrar", ui.isLoading, onLogin)
        Spacer(Modifier.height(Spacing.xl))
    }
}

// ══════════════════════════════════════════════════════════════
// FASE 3 — REGISTRO INDIVIDUAL
// ══════════════════════════════════════════════════════════════
@Composable
private fun PhaseRegisterIndividual(
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        PhaseHeader(
            title    = "Cuenta personal",
            subtitle = "Para comerciales independientes",
            onBack   = onBack,
        )
        Spacer(Modifier.height(32.dp))

        DarkTextField(ui.name, onNameChange, "Nombre completo",
            cap = KeyboardCapitalization.Words, imeAction = ImeAction.Next) { focus.moveFocus(FocusDirection.Down) }
        Spacer(Modifier.height(10.dp))
        DarkTextField(ui.username, onUsernameChange, "Nombre de usuario",
            imeAction = ImeAction.Next) { focus.moveFocus(FocusDirection.Down) }
        Spacer(Modifier.height(10.dp))
        DarkTextField(ui.email, onEmailChange, "Email",
            keyboardType = KeyboardType.Email, imeAction = ImeAction.Next) { focus.moveFocus(FocusDirection.Down) }
        Spacer(Modifier.height(10.dp))
        DarkPasswordField(ui.password, onPasswordChange, ui.passwordVisible, onTogglePassword,
            ImeAction.Done, onRegister)

        AnimatedError(ui.error)
        Spacer(Modifier.height(28.dp))

        CyanButton("Crear cuenta", ui.isLoading, onRegister)
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onGoToLogin) {
            Text("Ya tengo cuenta — acceder", color = CyanGlow.copy(0.7f), fontSize = 13.sp)
        }
        Spacer(Modifier.height(Spacing.xl))
    }
}

// ══════════════════════════════════════════════════════════════
// FASE 4 — REGISTRO EMPRESA
// ══════════════════════════════════════════════════════════════
@Composable
private fun PhaseRegisterCompany(
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        PhaseHeader(
            title    = "Cuenta de empresa",
            subtitle = "Para gestores de equipos comerciales",
            onBack   = onBack,
        )
        Spacer(Modifier.height(32.dp))

        // Empresa
        FieldGroupLabel("Empresa")
        Spacer(Modifier.height(8.dp))
        DarkTextField(ui.companyName, onCompanyNameChange, "Nombre de la empresa",
            cap = KeyboardCapitalization.Words, imeAction = ImeAction.Next) { focus.moveFocus(FocusDirection.Down) }

        Spacer(Modifier.height(20.dp))
        FieldGroupLabel("Tu cuenta de administrador")
        Spacer(Modifier.height(8.dp))

        DarkTextField(ui.name, onNameChange, "Tu nombre completo",
            cap = KeyboardCapitalization.Words, imeAction = ImeAction.Next) { focus.moveFocus(FocusDirection.Down) }
        Spacer(Modifier.height(10.dp))
        DarkTextField(ui.username, onUsernameChange, "Nombre de usuario",
            imeAction = ImeAction.Next) { focus.moveFocus(FocusDirection.Down) }
        Spacer(Modifier.height(10.dp))
        DarkTextField(ui.email, onEmailChange, "Email",
            keyboardType = KeyboardType.Email, imeAction = ImeAction.Next) { focus.moveFocus(FocusDirection.Down) }
        Spacer(Modifier.height(10.dp))
        DarkPasswordField(ui.password, onPasswordChange, ui.passwordVisible, onTogglePassword,
            ImeAction.Done, onRegister)

        AnimatedError(ui.error)
        Spacer(Modifier.height(28.dp))

        CyanButton("Crear empresa", ui.isLoading, onRegister)
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onGoToLogin) {
            Text("Ya tengo cuenta — acceder", color = CyanGlow.copy(0.7f), fontSize = 13.sp)
        }
        Spacer(Modifier.height(Spacing.xl))
    }
}

// ══════════════════════════════════════════════════════════════
// COMPONENTES COMPARTIDOS
// ══════════════════════════════════════════════════════════════

@Composable
private fun AppLogo(size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(CyanGlow.copy(alpha = 0.12f))
            .border(1.dp, CyanGlow.copy(alpha = 0.35f), RoundedCornerShape(size * 0.28f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector        = Icons.Outlined.Route,
            contentDescription = "RutasApp",
            modifier           = Modifier.size(size * 0.52f),
            tint               = CyanGlow,
        )
    }
}

@Composable
private fun PhaseHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkCard)
                .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver",
                Modifier.size(16.dp), tint = TextSub)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp)
            Text(subtitle, color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FieldGroupLabel(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(CyanGlow.copy(0.6f)))
        Spacer(Modifier.width(8.dp))
        Text(label, color = CyanGlow.copy(0.7f), fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
        Spacer(Modifier.width(8.dp))
        HorizontalDivider(Modifier.weight(1f), color = DarkBorder)
    }
}

@Composable
private fun DarkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    cap: KeyboardCapitalization = KeyboardCapitalization.None,
    onNext: () -> Unit = {},
) {
    OutlinedTextField(
        value           = value,
        onValueChange   = onValueChange,
        label           = { Text(label, fontSize = 13.sp) },
        singleLine      = true,
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(12.dp),
        colors          = darkFieldColors(),
        keyboardOptions = KeyboardOptions(
            keyboardType   = keyboardType,
            capitalization = cap,
            imeAction      = imeAction,
        ),
        keyboardActions = KeyboardActions(onNext = { onNext() }, onDone = { onNext() }),
    )
}

@Composable
private fun DarkPasswordField(
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
        label                = { Text("Contraseña", fontSize = 13.sp) },
        singleLine           = true,
        modifier             = Modifier.fillMaxWidth(),
        shape                = RoundedCornerShape(12.dp),
        colors               = darkFieldColors(),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon         = {
            IconButton(onClick = onToggle, modifier = Modifier.semantics {
                contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña"
            }) {
                Icon(
                    if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    null, tint = TextMuted, modifier = Modifier.size(18.dp),
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
    )
}

@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor    = DarkBorder,
    focusedBorderColor      = CyanGlow.copy(alpha = 0.7f),
    unfocusedContainerColor = DarkCard,
    focusedContainerColor   = DarkCard,
    unfocusedLabelColor     = TextMuted,
    focusedLabelColor       = CyanGlow,
    cursorColor             = CyanGlow,
    focusedTextColor        = TextPrimary,
    unfocusedTextColor      = TextPrimary,
)

@Composable
private fun AnimatedError(error: String?) {
    AnimatedVisibility(
        visible = error != null,
        enter   = fadeIn() + expandVertically(),
        exit    = fadeOut() + shrinkVertically(),
    ) {
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f))
                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp, modifier = Modifier.semantics {
                        contentDescription = "Error: $error"
                    })
            }
        }
    }
}

@Composable
private fun CyanButton(text: String, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        enabled  = !loading,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape    = RoundedCornerShape(14.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = CyanGlow,
            contentColor   = RutasColors.Dark950,
            disabledContainerColor = CyanGlow.copy(alpha = 0.4f),
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(18.dp),
                color       = RutasColors.Dark950,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.1).sp)
        }
    }
}

// ── Dialog genérico oscuro ────────────────────────────────────
@Composable
private fun AuthDialog(
    title:     String,
    body:      String,
    confirm:   String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest    = onDismiss,
        containerColor      = DarkCard,
        titleContentColor   = TextPrimary,
        textContentColor    = TextSub,
        tonalElevation      = 0.dp,
        shape               = RoundedCornerShape(18.dp),
        title               = { Text(title, fontWeight = FontWeight.SemiBold) },
        text                = { Text(body, fontSize = 14.sp) },
        confirmButton       = {
            TextButton(onClick = onConfirm) {
                Text(confirm, color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton       = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextMuted)
            }
        },
    )
}

// ── Dialogs públicos (para compatibilidad externa) ────────────
@Composable
fun ExitAppDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) =
    AuthDialog("¿Salir de RutasApp?", "¿Seguro que quieres cerrar la aplicación?",
        "Salir", onConfirm, onDismiss)

@Composable
fun DiscardChangesDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) =
    AuthDialog("¿Descartar cambios?", "Los datos introducidos se perderán.",
        "Descartar", onConfirm, onDismiss)

// ── Fondo: patrón de puntos en cuadrícula ─────────────────────
private fun DrawScope.drawDotGrid(color: Color) {
    val spacing = 28.dp.toPx()
    val radius  = 1.2.dp.toPx()
    var x = spacing
    while (x < size.width) {
        var y = spacing
        while (y < size.height) {
            drawCircle(color = color, radius = radius, center = Offset(x, y))
            y += spacing
        }
        x += spacing
    }
}

// ── Constantes de animación ────────────────────────────────────
private val EaseOutCubic = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)
