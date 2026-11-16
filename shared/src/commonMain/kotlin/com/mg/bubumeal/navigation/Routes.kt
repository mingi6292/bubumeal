package com.mg.bubumeal.navigation

import kotlinx.serialization.Serializable

// 화면 경로. 이름은 docs/screen-flow.md 2장과 같다.
// 인자가 필요해지면(3단계 이후) data class로 바꾼다.

@Serializable
data object LoginRoute

@Serializable
data object StartRoute

@Serializable
data object WeeklyRoute

@Serializable
data object CodeInputRoute

@Serializable
data object PlanSwitchRoute

@Serializable
data object PlanSettingsRoute

@Serializable
data object IssueCodeRoute
