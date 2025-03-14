package com.xayah.databackup.ui.activity.main.page.cloud

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.main.page.cloud.router.CloudRoutes
import com.xayah.databackup.ui.component.FabScaffold
import com.xayah.databackup.ui.component.ListItemCloudAccount
import com.xayah.databackup.ui.component.Loader
import com.xayah.databackup.ui.component.Serial
import com.xayah.databackup.ui.component.paddingBottom
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.component.paddingTop
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.util.readCloudActiveName

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun PageAccount(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<AccountViewModel>()
    val uiState by viewModel.uiState
    val cloudEntities by uiState.cloudEntities.collectAsState(initial = listOf())
    val activeName by context.readCloudActiveName().collectAsState(initial = "")

    LaunchedEffect(null) {
        viewModel.initialize()
    }

    FabScaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    navController.navigate(CloudRoutes.AccountDetail.route)
                },
                expanded = true,
                icon = { Icon(imageVector = Icons.Rounded.Add, contentDescription = null) },
                text = { Text(text = stringResource(id = R.string.add)) },
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) {
        Loader(modifier = Modifier.fillMaxSize(), isLoading = uiState.isLoading) {
            LazyColumn(
                modifier = Modifier.paddingHorizontal(CommonTokens.PaddingMedium),
                verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
            ) {
                item {
                    Spacer(modifier = Modifier.paddingTop(CommonTokens.PaddingMedium))
                }

                items(items = cloudEntities) { item ->
                    ListItemCloudAccount(
                        entity = item,
                        navController = navController,
                        onCardClick = {},
                        chipGroup = {
                            if (item.name == activeName) Serial(serial = stringResource(id = R.string.main_account))
                            if (item.account.type.isNotEmpty()) Serial(serial = item.account.type)
                            if (item.account.vendor.isNotEmpty()) Serial(serial = item.account.vendor)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.paddingBottom(CommonTokens.PaddingMedium))
                }
            }
        }
    }
}
