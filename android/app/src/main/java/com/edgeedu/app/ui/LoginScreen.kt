package com.edgeedu.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.edgeedu.app.R
import com.edgeedu.app.content.ContentScope
import com.edgeedu.app.ui.theme.EdgeEduGradients

private val MEDIUM_LABELS = mapOf("English" to "English", "Hindi" to "हिंदी", "Marathi" to "मराठी")

/**
 * Login / Profile screen (PRD §12.1, §16) in the design-kit look: a gradient
 * hero, then a local-profile form (name + class + medium). Submitting triggers
 * the one-time content download.
 */
@Composable
fun LoginScreen(onSubmit: (name: String, standard: Int, medium: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var standard by remember { mutableIntStateOf(10) }
    var medium by remember { mutableStateOf("English") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        // Hero
        Box(
            Modifier
                .fillMaxWidth()
                .background(EdgeEduGradients.Header, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .padding(top = 48.dp, bottom = 32.dp, start = 24.dp, end = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.edgeedu_logo),
                    contentDescription = "EdgeEdu logo",
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)),
                )
                Text(
                    "EdgeEdu",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    "Your offline learning companion",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Box(
                    Modifier
                        .padding(top = 14.dp)
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        "Works 100% offline after setup",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        // Form
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text("Set up your profile", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Set once — remembered until you log out.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Class", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ContentScope.STANDARDS.forEach { std ->
                    SelectChip(
                        label = "Class $std",
                        selected = standard == std,
                        gradient = EdgeEduGradients.Header,
                        modifier = Modifier.weight(1f),
                    ) { standard = std }
                }
            }

            Text("Medium of instruction", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ContentScope.MEDIUMS.forEach { med ->
                    SelectChip(
                        label = MEDIUM_LABELS[med] ?: med,
                        selected = medium == med,
                        gradient = EdgeEduGradients.Accent,
                        modifier = Modifier.weight(1f),
                    ) { medium = med }
                }
            }

            Button(
                onClick = { onSubmit(name, standard, medium) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (name.isNotBlank()) EdgeEduGradients.Header
                            else Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                            ),
                            RoundedCornerShape(16.dp),
                        )
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Start Learning  →",
                        color = if (name.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Text(
                "First login needs internet once to download Class $standard · " +
                    "${MEDIUM_LABELS[medium] ?: medium} content. After that, everything " +
                    "works fully offline until you log out.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SelectChip(
    label: String,
    selected: Boolean,
    gradient: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val base = if (selected) {
        modifier.background(gradient, RoundedCornerShape(16.dp))
    } else {
        modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    }
    Box(
        base.clickable(onClick = onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}
