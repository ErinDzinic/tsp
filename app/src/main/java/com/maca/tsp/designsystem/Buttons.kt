package com.maca.tsp.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.maca.tsp.R
import com.maca.tsp.ui.theme.TspTheme

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = TspTheme.spacing.spacing6)
        .height(50.dp),
    isDisabled: Boolean = false,
    icon: Int = R.drawable.path_9,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = { if (!isDisabled) onClick() },
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor =
                if (isDisabled) TspTheme.colors.onSurface.copy(alpha = 0.12f)
                else TspTheme.colors.colorGrayishBlack.copy(alpha = 0.8f)
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ){
            Icon(
                modifier = Modifier.padding(end = TspTheme.spacing.spacing1),
                painter = painterResource(icon),
                contentDescription = null,
                tint = Color.White
            )

            Text(
                text = text,
                color = Color.White,
                style = TspTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = TspTheme.spacing.spacing2)
        .height(50.dp),
    isDisabled: Boolean = false,
    icon: Int = com.maca.tsp.R.drawable.ic_arrow_right,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = { if (!isDisabled) onClick() },
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        border = BorderStroke(1.dp, TspTheme.colors.colorPurple),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = TspTheme.colors.colorPurple,
            containerColor = TspTheme.colors.colorPurple,
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ){
            Text(
                text = text,
                color = TspTheme.colors.background,
                style = TspTheme.typography.bodyLarge
            )

            Icon(
                modifier = Modifier.padding(start = TspTheme.spacing.spacing1).size(TspTheme.spacing.spacing4),
                painter = painterResource(icon),
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

@Composable
fun TspCircularIconButton(
    icon: Painter,
    modifier: Modifier = Modifier,
    isDisabled: Boolean = false,
    backgroundColor: Color = TspTheme.colors.colorGrayishBlack,
    iconColor: Color = Color.White,
    onClick: () -> Unit
) {
    IconButton(
        onClick = { if (!isDisabled) onClick() },
        modifier = modifier
            .size(50.dp)
            .border(2.dp, TspTheme.colors.white.copy(alpha = 0.3f), CircleShape)
            .background(backgroundColor, CircleShape),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        )
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(TspTheme.spacing.spacing3)
        )
    }
}
