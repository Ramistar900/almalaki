package com.almalaki.cafe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun RoyalRemoteDiagnosticsScreen(
    modifier: Modifier = Modifier
) {

    var lastKey by remember {
        mutableStateOf(
            RoyalUniversalRemoteDetector.getLastDetectedKey()
        )
    }

    /*
     * نراقب آخر حدث بشكل دوري.
     *
     * هذا مؤقت لمرحلة التشخيص فقط،
     * ولن يكون جزءاً من محرك الريموت النهائي.
     */
    LaunchedEffect(Unit) {

        while (true) {

            val detected =
                RoyalUniversalRemoteDetector
                    .getLastDetectedKey()

            if (detected != lastKey) {
                lastKey = detected
            }

            delay(100)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        Text(
            text = "ROYAL REMOTE DIAGNOSTICS",
            style = MaterialTheme.typography.headlineSmall,
            color = Gold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "اضغط أي زر في الريموت لمشاهدة البيانات الحقيقية",
            style = MaterialTheme.typography.bodyMedium,
            color = Cream
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        if (lastKey == null) {

            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF111111)
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "بانتظار زر من الريموت...",
                        color = Cream,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

        } else {

            val key = lastKey!!

            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF111111)
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    DiagnosticRow(
                        label = "Key Name",
                        value = key.keyName
                    )

                    DiagnosticRow(
                        label = "Key Code",
                        value = key.keyCode.toString()
                    )

                    DiagnosticRow(
                        label = "Action",
                        value = key.actionName
                    )

                    DiagnosticRow(
                        label = "Repeat Count",
                        value = key.repeatCount.toString()
                    )

                    DiagnosticRow(
                        label = "Device ID",
                        value = key.deviceId.toString()
                    )

                    DiagnosticRow(
                        label = "Scan Code",
                        value = key.scanCode.toString()
                    )

                    DiagnosticRow(
                        label = "Source",
                        value = key.source.toString()
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {

            Button(
                onClick = {
                    RoyalUniversalRemoteDetector
                        .clearLastDetectedKey()

                    lastKey = null
                }
            ) {

                Text(
                    text = "مسح آخر حدث"
                )
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = label,
            color = Gold,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = value,
            color = Cream,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
