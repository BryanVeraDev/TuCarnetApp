package tu.paquete.ui.terms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.tucarnetapp.ui.theme.TuCarnetAppTheme
import com.example.tucarnetapp.ui.theme.UfpsBlancoFavorito
import com.example.tucarnetapp.ui.theme.UfpsInfoPrincipal
import com.example.tucarnetapp.ui.theme.UfpsPrincipal
import com.example.tucarnetapp.ui.theme.UfpsSecundario
import com.example.tucarnetapp.ui.theme.UfpsTextoClaro
import com.example.tucarnetapp.ui.theme.UfpsTextoOscuro
import com.example.tucarnetapp.ui.theme.UfpsTextoPrincipal

@Composable
fun TermsConditionsScreen(
    onCancel: () -> Unit = {},
    onAccept: () -> Unit = {}
) {
    var checked by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            BottomBarButtons(
                checked = checked,
                onCancel = onCancel,
                onAccept = onAccept
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 24.dp)
                .fillMaxSize()
        ) {

            Spacer(Modifier.height(16.dp))

            // 🚨 TÍTULO FIJO
            Text(
                text = "Acuerdo de Términos y Condiciones de Uso",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = UfpsTextoOscuro
            )

            Spacer(Modifier.height(16.dp))

            // 🚨 SOLO ESTA PARTE HACE SCROLL
            Column(
                modifier = Modifier
                    .weight(1f)               // hace que ocupe el espacio disponible
                    .verticalScroll(rememberScrollState())
            ) {

                Text(
                    buildAnnotatedString {
                        bold("TuCarnet UFPS")
                        append(" es una herramienta institucional de la ")
                        bold("Universidad Francisco de Paula Santander")
                        append(" para la gestión académica y administrativa de los estudiantes.")
                    },
                    fontSize = 14.sp,
                    lineHeight = 20.sp, 
                    color = UfpsTextoOscuro
                )

                Spacer(Modifier.height(16.dp))

                TermsContent()

                Spacer(Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = UfpsInfoPrincipal,
                            uncheckedColor = UfpsTextoPrincipal
                        )
                    )
                    Text("He leído los términos y condiciones", fontSize = 14.sp)
                }

                Spacer(Modifier.height(20.dp))
            }

            // Nota: No se necesita Spacer extra aquí, la BottomBar ya maneja el fondo
        }
    }

}

// ========== CONTENIDO ==========
@Composable
fun TermsContent() {
    Section("1. Finalidad institucional:") {
        Text(
            "La aplicación se usa exclusivamente dentro de la Universidad para procesos internos. No tiene fines comerciales ni externos.",
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }

    Section("2. Datos personales tratados:") {
        Text("Se almacenarán algunos datos personales, como:", fontSize = 14.sp)

        Bullet("Nombre completo")
        Bullet("Documento de identidad")
        Bullet("Código estudiantil")
        Bullet("Correo institucional")
        Bullet("Tipo de sangre")
        Bullet("Fotografía del usuario")
    }

    Section("3. Estos datos se emplean solo para identificación, registro y servicios institucionales.")

    Section("4. Uso y confidencialidad:") {
        Text(
            "Los datos se usan únicamente por la Universidad. No se compartirán con terceros ni se usarán fuera del ámbito institucional.",
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

// ===== Helpers =====

@Composable
fun Section(title: String, content: @Composable (() -> Unit)? = null) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = UfpsTextoOscuro
        )
        if (content != null) {
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
fun Bullet(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(start = 12.dp, top = 4.dp)
    ) {
        Text("•", fontSize = 18.sp)
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 14.sp)
    }
}

fun AnnotatedString.Builder.bold(text: String) {
    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
        append(text)
    }
}

// ===== BOTONERA =====
@Composable
fun BottomBarButtons(
    checked: Boolean,
    onCancel: () -> Unit,
    onAccept: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(
                containerColor = UfpsBlancoFavorito,
                contentColor = UfpsPrincipal
            ),
            border = BorderStroke(1.dp, UfpsPrincipal),
            shape = RoundedCornerShape(50),
            modifier = Modifier.weight(1f)
        ) {
            Text("Cancelar", fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp)
        }

        Spacer(Modifier.width(12.dp))

        Button(
            onClick = onAccept,
            enabled = checked,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (checked) UfpsSecundario else UfpsTextoClaro,
                contentColor = if (checked) UfpsBlancoFavorito else UfpsTextoPrincipal
            ),
            shape = RoundedCornerShape(50),
            modifier = Modifier.weight(1f)
        ) {
            Text("Tomar foto", fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewTermsConditionsScreen() {
    TuCarnetAppTheme() {        // ← Esto es lo clave
        TermsConditionsScreen()
    }
}


