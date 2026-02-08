// ui/components/Badges.kt
package com.antechrist.adherentsapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import com.antechrist.adherentsapp.R

@Composable
fun BureauBadge(
    modifier: Modifier = Modifier,
    label: String = "",
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    labelColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    outline: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
        tonalElevation = 0.dp,
        border = if (outline) BorderStroke(1.dp, labelColor.copy(alpha = 0.45f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_bureau_member),
                contentDescription = "Membre du bureau",
                tint = iconTint,
                modifier = Modifier
                    .height(18.dp)
                    .width(24.dp)
                    //.size(18.dp)
            )
            //Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                color = labelColor,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
