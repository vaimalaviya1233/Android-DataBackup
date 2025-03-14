package com.xayah.databackup.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TopSpacer(innerPadding: PaddingValues) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(innerPadding.calculateTopPadding())
    )
}


@Composable
fun BottomSpacer(innerPadding: PaddingValues) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(innerPadding.calculateBottomPadding())
    )
}
