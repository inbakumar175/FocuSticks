package com.example.focusticks.ui.theme.Screens.task

data class MyTask(
    val id: String = "",
    val title: String = "",
    val subject: String = "",
    val category: String = "",
    val difficulty: String = "",
    val due: String = "",
    val remindBeforeMinutes: Long = 0L,
    val completed: Boolean = false
)