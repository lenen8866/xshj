package com.sda.books.reader.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EventBus {
    private val _chapterHeightFlow = MutableSharedFlow<Int>(replay = 0)
    val chapterHeightFlow = _chapterHeightFlow.asSharedFlow()

    suspend fun sendChapterHeight(height: Int) {
        _chapterHeightFlow.emit(height)
    }
}
