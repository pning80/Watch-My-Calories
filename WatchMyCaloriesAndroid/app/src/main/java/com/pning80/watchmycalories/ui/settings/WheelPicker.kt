package com.pning80.watchmycalories.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun <T> WheelPicker(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    itemHeight: Dp = 40.dp,
    itemToString: (T) -> String = { it.toString() }
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = max(0, items.indexOf(selectedItem)))
    val visibleItemsCount = (height / itemHeight).toInt()
    val centeredItemIndex by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex + visibleItemsCount / 2
        }
    }

    LaunchedEffect(centeredItemIndex) {
        if (centeredItemIndex < items.size) {
            onItemSelected(items[centeredItemIndex])
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex + visibleItemsCount / 2
            listState.animateScrollToItem(centerIndex - visibleItemsCount/2)
        }
    }

    Box(modifier = modifier.height(height)) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = (height - itemHeight) / 2),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                Box(
                    modifier = Modifier.height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = itemToString(items[index]))
                }
            }
        }
    }
}