package com.inputleaf.android.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShizukuActiveSessionPolicyTest {

    @Test
    fun shouldClearActiveSession_keepsSessionOnSuccessAndRetrying() {
        assertThat(shouldClearActiveSession(ConnectAttemptOutcome.Success)).isFalse()
        assertThat(shouldClearActiveSession(ConnectAttemptOutcome.Retrying)).isFalse()
    }

    @Test
    fun shouldClearActiveSession_clearsSessionOnRejectedAndTerminalFailure() {
        assertThat(shouldClearActiveSession(ConnectAttemptOutcome.Rejected)).isTrue()
        assertThat(shouldClearActiveSession(ConnectAttemptOutcome.TerminalFailure)).isTrue()
    }
}
