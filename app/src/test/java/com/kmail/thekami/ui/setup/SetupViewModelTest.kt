package com.kmail.thekami.ui.setup

import com.kmail.thekami.domain.mail.ConnectionTester
import com.kmail.thekami.domain.mail.MailError
import com.kmail.thekami.domain.mail.MailResult
import com.kmail.thekami.domain.model.Account
import com.kmail.thekami.domain.model.AccountCreator
import com.kmail.thekami.domain.model.AddAccountResult
import com.kmail.thekami.domain.model.ConnectionSecurity
import com.kmail.thekami.domain.model.NewAccount
import com.kmail.thekami.domain.model.ServerEndpoint
import com.kmail.thekami.domain.model.ServerFormError
import com.kmail.thekami.domain.model.SuggestionNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {

    private class FakeTester(
        var imap: MailResult<Unit> = MailResult.Success(Unit),
        var smtp: MailResult<Unit> = MailResult.Success(Unit),
    ) : ConnectionTester {
        var imapCalls = 0
        var smtpCalls = 0
        override suspend fun testImap(endpoint: ServerEndpoint, password: CharArray): MailResult<Unit> {
            imapCalls++
            return imap
        }
        override suspend fun testSmtp(endpoint: ServerEndpoint, password: CharArray): MailResult<Unit> {
            smtpCalls++
            return smtp
        }
    }

    private class FakeCreator(
        var outcome: AddAccountResult? = null,
    ) : AccountCreator {
        var calls = 0
        var lastDraft: NewAccount? = null
        override suspend fun addAccount(draft: NewAccount): AddAccountResult {
            calls++
            lastDraft = draft
            return outcome ?: AddAccountResult.Added(
                Account(
                    id = "id",
                    emailAddress = draft.emailAddress,
                    displayName = draft.emailAddress,
                    colorArgb = draft.colorArgb,
                    settings = draft.settings,
                    createdAtMillis = 0L,
                ),
            )
        }
    }

    private val tester = FakeTester()
    private val creator = FakeCreator()
    private lateinit var vm: SetupViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = SetupViewModel(tester, creator)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun ready(email: String = "me@example.com") {
        vm.onEmailChange(email)
        vm.onPasswordChange("pw")
        assertTrue(vm.submitCredentials())
    }

    // ---- credentials ----

    @Test
    fun invalidEmailIsRejectedAndFlagged() {
        vm.onEmailChange("not-an-email")
        vm.onPasswordChange("secret")
        assertFalse(vm.submitCredentials())
        assertTrue(vm.credentials.value.emailError)
        assertFalse(vm.credentials.value.passwordError)
    }

    @Test
    fun emptyPasswordIsRejectedAndFlagged() {
        vm.onEmailChange("me@example.com")
        assertFalse(vm.submitCredentials())
        assertFalse(vm.credentials.value.emailError)
        assertTrue(vm.credentials.value.passwordError)
    }

    @Test
    fun bothErrorsAreReportedTogether() {
        assertFalse(vm.submitCredentials())
        assertTrue(vm.credentials.value.emailError)
        assertTrue(vm.credentials.value.passwordError)
    }

    @Test
    fun editingAFieldClearsItsError() {
        vm.submitCredentials()
        vm.onEmailChange("a")
        assertFalse(vm.credentials.value.emailError)
        assertTrue(vm.credentials.value.passwordError)
        vm.onPasswordChange("x")
        assertFalse(vm.credentials.value.passwordError)
    }

    // ---- pre-fill and server forms ----

    @Test
    fun gmailAddressPrefillsBothServers() {
        ready("Someone@Gmail.com")
        assertEquals("imap.gmail.com", vm.incoming.value.form.host)
        assertEquals("993", vm.incoming.value.form.port)
        assertEquals("smtp.gmail.com", vm.outgoing.value.form.host)
        assertEquals("465", vm.outgoing.value.form.port)
        assertEquals("someone@gmail.com", vm.incoming.value.form.username)
        assertEquals("pw", vm.incoming.value.password)
        assertEquals(SuggestionNote.APP_PASSWORD_REQUIRED, vm.note.value)
    }

    @Test
    fun switchingSecurityChangesOnlyADefaultPort() {
        ready()
        vm.onServerSecurity(ServerSide.INCOMING, ConnectionSecurity.STARTTLS)
        assertEquals("143", vm.incoming.value.form.port)

        vm.onServerPort(ServerSide.INCOMING, "1234")
        vm.onServerSecurity(ServerSide.INCOMING, ConnectionSecurity.SSL_TLS)
        assertEquals("1234", vm.incoming.value.form.port)
    }

    @Test
    fun invalidServerFormIsFlagged() {
        ready()
        vm.onServerHost(ServerSide.INCOMING, "bad host")
        assertFalse(vm.submitServer(ServerSide.INCOMING))
        assertTrue(ServerFormError.HOST_INVALID in vm.incoming.value.errors)
        vm.onServerHost(ServerSide.INCOMING, "imap.example.com")
        assertTrue(vm.submitServer(ServerSide.INCOMING))
    }

    // ---- connect ----

    @Test
    fun successfulConnectSavesAccountAndZeroesPasswords() {
        ready("  Me@Example.com ")
        vm.connect()
        assertEquals(ConnectState.Connected("me@example.com"), vm.connectState.value)
        assertEquals(1, tester.imapCalls)
        assertEquals(1, tester.smtpCalls)
        val draft = creator.lastDraft!!
        assertEquals("me@example.com", draft.emailAddress)
        assertTrue(draft.imapPassword.all { it == '\u0000' })
        assertTrue(draft.smtpPassword.all { it == '\u0000' })
        assertEquals("", vm.incoming.value.password)
    }

    @Test
    fun incomingFailureStopsBeforeSmtpAndSaving() {
        tester.imap = MailResult.Failure(MailError.Authentication("x"))
        ready()
        vm.connect()
        assertEquals(
            ConnectState.Failed(ConnectFailure.Incoming(MailError.Authentication("x"))),
            vm.connectState.value,
        )
        assertEquals(0, tester.smtpCalls)
        assertEquals(0, creator.calls)
    }

    @Test
    fun outgoingFailureStopsBeforeSaving() {
        tester.smtp = MailResult.Failure(MailError.Tls("y"))
        ready()
        vm.connect()
        assertEquals(
            ConnectState.Failed(ConnectFailure.Outgoing(MailError.Tls("y"))),
            vm.connectState.value,
        )
        assertEquals(0, creator.calls)
    }

    @Test
    fun repositoryOutcomesMapToFailures() {
        ready()
        creator.outcome = AddAccountResult.AlreadyExists
        vm.connect()
        assertEquals(ConnectState.Failed(ConnectFailure.AlreadyExists), vm.connectState.value)

        creator.outcome = AddAccountResult.CredentialStorageFailed
        vm.connect()
        assertEquals(ConnectState.Failed(ConnectFailure.StorageFailed), vm.connectState.value)
    }

    @Test
    fun retryAfterFailureCanSucceed() {
        tester.imap = MailResult.Failure(MailError.Network("n"))
        ready()
        vm.connect()
        assertTrue(vm.connectState.value is ConnectState.Failed)
        tester.imap = MailResult.Success(Unit)
        vm.connect()
        assertEquals(ConnectState.Connected("me@example.com"), vm.connectState.value)
    }

    @Test
    fun connectWithoutValidFormsFailsWithoutCallingServers() {
        vm.connect()
        assertEquals(ConnectState.Failed(ConnectFailure.InvalidInput), vm.connectState.value)
        assertEquals(0, tester.imapCalls)
        assertNull(creator.lastDraft)
        assertNotNull(vm.connectState.value)
    }
}
