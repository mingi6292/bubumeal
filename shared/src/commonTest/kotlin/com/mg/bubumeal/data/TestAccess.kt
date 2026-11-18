package com.mg.bubumeal.data

import com.mg.bubumeal.domain.PlanAccess
import kotlin.test.assertIs

/** 테스트용: 접근이 허용된 결과라고 확인하고 값을 꺼낸다. */
fun <T> PlanAccess<T>.granted(): T = assertIs<PlanAccess.Granted<T>>(this).value
