package com.afternote.feature.afternote.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.afternote.feature.afternote.presentation.author.edit.model.AfternoteEditState
import com.afternote.feature.afternote.presentation.author.edit.model.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.navigation.AfternoteEditStateHandling

/**
 * AfternoteScreen의 UI/네비게이션 상태를 관리하는 State Holder.
 *
 * 비즈니스 데이터는 ViewModel에서 관리하고,
 * UI에 종속적인 상태만 여기서 관리합니다.
 */
@Stable
class AfternoteAppState(
    val navController: NavHostController,
    val playlistHolder: MemorialPlaylistStateHolder,
    private val editStateHolder: MutableState<AfternoteEditState?>,
) {
    /** UDF-friendly snapshot + callbacks for nav graph (no [MutableState] leak). */
    val editHandling: AfternoteEditStateHandling
        get() =
            AfternoteEditStateHandling(
                state = editStateHolder.value,
                onStateChanged = { editStateHolder.value = it },
                onClear = { editStateHolder.value = null },
            )
}

@Composable
fun rememberAfternoteAppState(
    navController: NavHostController = rememberNavController(),
    playlistHolder: MemorialPlaylistStateHolder = remember { MemorialPlaylistStateHolder() },
    editStateHolder: MutableState<AfternoteEditState?> = remember { mutableStateOf(null) },
): AfternoteAppState =
    remember(navController, playlistHolder, editStateHolder) {
        AfternoteAppState(navController, playlistHolder, editStateHolder)
    }
