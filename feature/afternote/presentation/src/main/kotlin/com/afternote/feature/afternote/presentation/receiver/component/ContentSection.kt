package com.afternote.feature.afternote.presentation.receiver.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.theme.AfternoteDesign

@Composable
fun ContentSection(
    title: String,
    desc: String,
    subDesc: String,
    btnText: String,
    imageResource: Painter,
    modifier: Modifier = Modifier,
    onButtonClick: () -> Unit = {},
) {
    Column(modifier = modifier.padding(bottom = 24.dp)) {
        Text(
            text = title,
            style = AfternoteDesign.typography.bodyLargeB,
            color = AfternoteDesign.colors.gray9,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Flat style as per image
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Background decoration (Simulating the 3D icons in the image)
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd),
                ) {
                    Image(
                        painter = imageResource,
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(170.dp),
                    )
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = desc,
                        style =
                            AfternoteDesign.typography.textField.copy(
                                fontWeight = FontWeight.Medium,
                                color = AfternoteDesign.colors.gray9,
                            ),
                    )
                    Text(
                        text = subDesc,
                        style = AfternoteDesign.typography.captionLargeR,
                        color = AfternoteDesign.colors.gray9,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    )

                    Button(
                        onClick = onButtonClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AfternoteDesign.colors.gray9),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Text(
                            text = btnText,
                            style =
                                AfternoteDesign.typography.captionLargeR.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = AfternoteDesign.colors.gray9,
                                ),
                        )

                        Spacer(modifier = Modifier.width(4.dp))
                        RightArrowIcon(AfternoteDesign.colors.gray9)
                    }
                }
            }
        }
    }
}
