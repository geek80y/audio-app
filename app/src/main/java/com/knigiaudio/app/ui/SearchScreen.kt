package com.knigiaudio.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knigiaudio.app.data.BookShort
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(onBook: (BookShort) -> Unit, vm: SearchViewModel = viewModel()) {
    var text by rememberSaveable { mutableStateOf(vm.query) }
    val focus = LocalFocusManager.current
    val requester = androidx.compose.runtime.remember { FocusRequester() }

    // поиск «на лету» с задержкой, чтобы не дёргать сайт на каждую букву
    LaunchedEffect(text) {
        delay(700)
        vm.submit(text)
    }
    LaunchedEffect(Unit) { if (text.isEmpty()) runCatching { requester.requestFocus() } }

    Column {
        Text(
            "Поиск", style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 12.dp),
        )
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Название книги или автор") },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Palette.Muted) },
            trailingIcon = {
                if (text.isNotEmpty()) IconButton(onClick = { text = "" }) { Icon(Icons.Rounded.Close, "Очистить") }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Palette.SurfaceHi,
                unfocusedContainerColor = Palette.Surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Palette.Amber,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.submit(text); focus.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .focusRequester(requester),
        )

        BookGrid(
            items = vm.items,
            loading = vm.loading,
            error = vm.error,
            hasMore = vm.hasMore,
            onLoadMore = vm::loadMore,
            onRetry = vm::retry,
            onBook = { focus.clearFocus(); onBook(it) },
            modifier = Modifier.padding(top = 8.dp),
            empty = {
                if (vm.query.length < 2) EmptyState("Найдите книгу", "Введите хотя бы 2 буквы названия или фамилии автора")
                else EmptyState("Ничего не найдено", "Попробуйте изменить запрос")
            },
        )
    }
}
