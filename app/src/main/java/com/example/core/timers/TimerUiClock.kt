package com.example.core.timers

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Relógio de 1 s para a UI; só emite enquanto alguma tela observa (o estado real vive no Room). */
fun timerUiClock(): Flow<Long> = flow {
    while (true) {
        emit(System.currentTimeMillis())
        delay(1000L)
    }
}
