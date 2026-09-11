package com.inputleaf.android.service

internal enum class ConnectAttemptOutcome {
    Success,
    Retrying,
    Rejected,
    TerminalFailure,
}

internal fun shouldClearActiveSession(outcome: ConnectAttemptOutcome): Boolean =
    outcome == ConnectAttemptOutcome.Rejected ||
        outcome == ConnectAttemptOutcome.TerminalFailure
