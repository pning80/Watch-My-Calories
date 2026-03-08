package com.pning80.watchmycalories.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WheelNumberPicker(
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
    count: Int,
    onScrollFinished: (index: Int) -> Int
) {
    val listState = rememberLazyListState(0)
    val itemHeight = 36
    val centralItemIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                0
            } else {
                val viewportCenter = layoutInfo.viewportEndOffset / 2
                visibleItemsInfo.minByOrNull { kotlin.math.abs(it.offset + it.size / 2 - viewportCenter) }?.index ?: 0
            }
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            onScrollFinished(centralItemIndex)
            listState.animateScrollToItem(centralItemIndex)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .width(50.dp)
                .height((itemHeight * 3).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(count) { index ->
                Box(
                    modifier = Modifier.height(itemHeight.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (startIndex + index).toString(),
                        style = if (centralItemIndex == index) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyMedium
                        }
                    )
                }
            }
        }
    }
}
