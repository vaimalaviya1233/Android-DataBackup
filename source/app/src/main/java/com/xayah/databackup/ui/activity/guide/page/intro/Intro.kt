package com.xayah.databackup.ui.activity.guide.page.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xayah.databackup.ui.activity.guide.page.env.envContents
import com.xayah.databackup.ui.activity.guide.page.env.envSubtitles
import com.xayah.databackup.ui.activity.guide.page.env.envTitles
import com.xayah.databackup.ui.component.IntroCard
import com.xayah.databackup.ui.component.paddingBottom
import com.xayah.databackup.ui.component.paddingTop
import com.xayah.databackup.ui.token.CommonTokens

@ExperimentalMaterial3Api
@Composable
fun PageIntro() {
    val titles = envTitles()
    val subtitles = envSubtitles()
    val contents = envContents()

    LazyColumn(
        modifier = Modifier.paddingTop(CommonTokens.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
    ) {
        items(count = titles.size, key = { it }) {
            IntroCard(
                serial = (it + 1).digitToChar(),
                title = titles[it],
                subtitle = subtitles[it],
                content = contents[it]
            )
        }
        item {
            Spacer(modifier = Modifier.paddingBottom(CommonTokens.PaddingSmall))
        }
    }
}
