package com.maca.tsp.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.maca.tsp.R
import com.maca.tsp.data.enums.ImageFilterType
import com.maca.tsp.ui.theme.TspTheme

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = TspTheme.spacing.spacing6)
        .height(TspTheme.spacing.spacing6_25),
    isDisabled: Boolean = false,
    icon: Int = R.drawable.path_9,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = { if (!isDisabled) onClick() },
        modifier = modifier,
        shape = RoundedCornerShape(TspTheme.spacing.spacing3_75),
        colors = ButtonDefaults.buttonColors(
            containerColor =
                if (isDisabled) TspTheme.colors.onSurface.copy(alpha = 0.12f)
                else TspTheme.colors.colorGrayishBlack.copy(alpha = 0.8f)
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
        .height(TspTheme.spacing.spacing6_25),
    isDisabled: Boolean = false,
    showIcon: Boolean = true,
    icon: Int = R.drawable.ic_right,
    iconSize: Dp = TspTheme.spacing.spacing4,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = { if (!isDisabled) onClick() },
        modifier = modifier,
        shape = RoundedCornerShape(TspTheme.spacing.spacing3_75),
        border = BorderStroke(TspTheme.spacing.unit, TspTheme.colors.colorPurple),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = TspTheme.colors.colorPurple,
            containerColor = TspTheme.colors.colorPurple,
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                color = TspTheme.colors.background,
                style = TspTheme.typography.bodyLarge
            )

            if(showIcon){
                Icon(
                    modifier = Modifier
                        .padding(start = TspTheme.spacing.spacing1)
                        .size(iconSize),
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun TspCircularIconButton(
    icon: Painter,
    buttonSize: Dp = TspTheme.spacing.spacing6_25,
    modifier: Modifier = Modifier,
    isDisabled: Boolean = false,
    backgroundColor: Color = TspTheme.colors.colorGrayishBlack,
    iconColor: Color = Color.White,
    onClick: () -> Unit
) {
    IconButton(
        onClick = { if (!isDisabled) onClick() },
        modifier = modifier
            .size(buttonSize)
            .border(
                TspTheme.spacing.extra_xxs,
                TspTheme.colors.white.copy(alpha = 0.3f),
                CircleShape
            )
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

@Composable
fun FilterButton(
    filterType: ImageFilterType,
    isSelected: Boolean,
    buttonSize: Dp = TspTheme.spacing.spacing6_25,
    onClick: () -> Unit
) {
    val backgroundColor =
        if (isSelected) TspTheme.colors.colorPurple else TspTheme.colors.colorGrayishBlack
    val iconColor = if (isSelected) TspTheme.colors.darkYellow else Color.White

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TspCircularIconButton(
            icon = painterResource(id = filterType.iconRes),
            buttonSize = buttonSize,
            backgroundColor = backgroundColor,
            iconColor = iconColor,
            onClick = onClick,
            modifier = Modifier.padding(horizontal = TspTheme.spacing.spacing0_5)
        )
        Text(
            modifier = Modifier.padding(top = TspTheme.spacing.spacing1),
            text = filterType.displayName,
            color = TspTheme.colors.background,
            style = TspTheme.typography.labelMedium
        )
    }
}

@Composable
fun BackButton(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = TspTheme.spacing.spacing2)
        .height(TspTheme.spacing.spacing6_25),
    isDisabled: Boolean = false,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = { if (!isDisabled) onClick() },
        modifier = modifier,
        shape = RoundedCornerShape(TspTheme.spacing.spacing3_75),
        border = BorderStroke(TspTheme.spacing.unit, TspTheme.colors.colorPurple),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = TspTheme.colors.colorPurple,
            containerColor = TspTheme.colors.colorPurple,
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier
                    .size(TspTheme.spacing.spacing4),
                painter = painterResource(R.drawable.ic_back),
                contentDescription = null,
                tint = Color.White
            )

            Text(
                text = stringResource(R.string.back),
                color = TspTheme.colors.background,
                style = TspTheme.typography.bodyLarge
            )
        }
    }
}