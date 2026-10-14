package com.knigiaudio.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knigiaudio.app.data.BookShort
import com.knigiaudio.app.data.Genre
import com.knigiaudio.app.data.Site

@Composable
fun HomeScreen(onBook: (BookShort) -> Unit, vm: FeedViewModel = viewModel()) {
    Column {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)) {
            Text(
                buildAnnotatedString {
                    append("Книги")
                    withStyle(SpanStyle(color = Palette.Amber)) { append(" Аудио") }
                },
                style = MaterialTheme.typography.headlineLarge,
            )
            Text("Слушайте бесплатно, где угодно", color = Palette.Muted, style = MaterialTheme.typography.bodyMedium)
        }

        val chips: List<Genre?> = listOf(null) + Site.genres
        LazyRow(
            state = rememberLazyListState(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(chips, key = { it?.path ?: "new" }) { g ->
                val selected = vm.genre == g
                FilterChip(
                    selected = selected,
                    onClick = { vm.select(g) },
                    label = { Text(g?.name ?: "Новинки") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Palette.Amber,
                        selectedLabelColor = Palette.OnAmber,
                        containerColor = Palette.Surface,
                        labelColor = Palette.Text,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true, selected = selected,
                        borderColor = Palette.Line, selectedBorderColor = Palette.Amber,
                    ),
                )
            }
        }

        BookGrid(
            items = vm.items,
            loading = vm.loading,
            error = vm.error,
            hasMore = vm.hasMore,
            onLoadMore = vm::loadMore,
            onRetry = vm::retry,
            onBook = onBook,
            empty = { EmptyState("Здесь пока пусто") },
        )
    }
}
