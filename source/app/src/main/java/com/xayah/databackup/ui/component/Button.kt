package com.xayah.databackup.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.theme.ColorScheme
import com.xayah.databackup.ui.token.ButtonTokens
import com.xayah.databackup.ui.token.CardTokens

@Composable
fun IconTextButton(icon: ImageVector, text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        content = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.paddingEnd(ButtonTokens.IconTextButtonPadding),
            )
            TitleSmallBoldText(text = text)
        }
    )
}

@Composable
fun CommonButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        content = { TitleSmallBoldText(text = text) }
    )
}

@Composable
fun TextButton(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        content = { TitleSmallBoldText(text = text) },
        contentPadding = ButtonDefaults.ContentPadding
    )
}

@ExperimentalMaterial3Api
@Composable
fun CardActionButton(modifier: Modifier = Modifier, text: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ColorScheme.tertiaryContainer()),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .padding(CardTokens.ContentPadding)
                .paddingHorizontal(CardTokens.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(ButtonTokens.CardActionButtonPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null)
            LabelLargeText(text = text)
        }
    }
}

@Composable
fun ArrowBackButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            imageVector = Icons.Rounded.ArrowBack,
            contentDescription = null
        )
    }
}

@ExperimentalMaterial3Api
@Composable
fun CheckListButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_rounded_checklist),
            contentDescription = null
        )
    }
}
