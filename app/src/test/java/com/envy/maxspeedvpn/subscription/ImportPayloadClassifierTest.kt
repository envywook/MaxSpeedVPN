package com.envy.maxspeedvpn.subscription

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.test.assertFailsWith
import org.junit.Test

class ImportPayloadClassifierTest {
    @Test
    fun `classifies HTTPS subscription without fetching it`() {
        val payload = ImportPayloadClassifier.classify("https://provider.example/subscription")
        assertEquals("https://provider.example/subscription", (payload as ImportPayload.Subscription).request.url)
    }

    @Test
    fun `classifies VLESS as local profile without treating it as subscription URL`() {
        val payload = ImportPayloadClassifier.classify(
            "vless://00000000-0000-4000-8000-000000000001@server.example:443?security=tls&type=tcp#Example",
        ) as ImportPayload.Profiles
        assertEquals(1, payload.profiles.size)
        assertEquals("vless", payload.profiles.single().protocol)
        assertEquals("server.example", payload.profiles.single().address)
    }

    @Test
    fun `classifies Naive as sing-box local profile`() {
        val payload = ImportPayloadClassifier.classify(
            "naive+https://fixture-user:fixture-pass@naive.example:443?insecure=false#Naive",
        ) as ImportPayload.Profiles
        assertEquals("naive", payload.profiles.single().protocol)
        assertTrue(payload.profiles.single().config.contains("\"type\":\"naive\""))
    }

    @Test
    fun `classifies canonical Mieru deep link`() {
        val payload = ImportPayloadClassifier.classify(
            "mieru://fixture-user:fixture-pass@mieru.example:443?transport=TCP#Mieru",
        )
        assertTrue(payload is ImportPayload.MieruProfile)
    }

    @Test
    fun `accepts common meiru spelling as Mieru alias`() {
        val payload = ImportPayloadClassifier.classify(
            "meiru://fixture-user:fixture-pass@mieru.example:443?transport=TCP#Mieru",
        )
        assertTrue(payload is ImportPayload.MieruProfile)
    }

    @Test
    fun `accepts only branded deep link`() {
        val encoded = "https%3A%2F%2Fprovider.example%2Fsub"
        assertTrue(ImportPayloadClassifier.classify("maxspeedvpn://add?url=$encoded") is ImportPayload.Subscription)
        assertFailsWith<IllegalArgumentException> {
            ImportPayloadClassifier.classify("lust://add?url=$encoded")
        }
    }

    @Test
    fun `QR policy rejects insecure subscription but accepts direct server URI`() {
        runCatching { ImportPayloadClassifier.classify("http://provider.example/sub", requireHttpsSubscription = true) }
            .onSuccess { error("HTTP subscription must be rejected") }
        assertTrue(
            ImportPayloadClassifier.classify(
                "vless://00000000-0000-4000-8000-000000000001@server.example:443?security=tls#Example",
                requireHttpsSubscription = true,
            ) is ImportPayload.Profiles,
        )
    }

    @Test
    fun `classifies newline-delimited direct URIs as local profiles`() {
        val payload = ImportPayloadClassifier.classify(
            "vless://00000000-0000-4000-8000-000000000001@first.example:443?security=tls#First\n" +
                "vless://00000000-0000-4000-8000-000000000002@second.example:8443?security=tls#Second",
        ) as ImportPayload.Profiles

        assertEquals(2, payload.profiles.size)
        assertEquals(listOf("First", "Second"), payload.profiles.map(ServerProfile::name))
    }

    @Test
    fun `rejects multiline subscription oversized and unknown payloads without exposing input`() {
        listOf("https://provider.example/sub\nhttps://provider.example/second", "x".repeat(4_097), "unknown://fixture-secret")
            .forEach { value ->
                val failure = runCatching { ImportPayloadClassifier.classify(value) }.exceptionOrNull()
                assertTrue(failure is IllegalArgumentException)
                assertTrue(failure?.message?.contains("fixture-secret") != true)
            }
    }

    @Test
    fun `classifies a sing-box profile document with Naive and Mieru outbounds`() {
        val payload = ImportPayloadClassifier.classify(
            """{"outbounds":[
                {"type":"urltest","tag":"select","outbounds":["naive-out","mieru-out"]},
                {"type":"naive","tag":"naive-out","server":"naive.example","server_port":443,"username":"fixture-user","password":"fixture-password","tls":{"enabled":true,"server_name":"naive.example"}},
                {"type":"mieru","tag":"mieru-out","server":"mieru.example","server_port":2012,"transport":"TCP","username":"fixture-user","password":"fixture-password","multiplexing":"MULTIPLEXING_HIGH"},
                {"type":"direct","tag":"direct"}
            ]}""",
        ) as ImportPayload.Profiles

        assertEquals(listOf("naive", "mieru"), payload.profiles.map(ServerProfile::protocol))
        assertEquals(listOf("naive.example", "mieru.example"), payload.profiles.map(ServerProfile::address))
    }

    @Test
    fun `classifies a sing-box Mieru outbound with port ranges`() {
        val payload = ImportPayloadClassifier.classify(
            """{"outbounds":[{"type":"mieru","tag":"mieru-range","server":"mieru.example","server_ports":["2012-2014","443-443"],"transport":"TCP","username":"fixture-user","password":"fixture-password"}]}""",
        ) as ImportPayload.Profiles

        assertEquals(1, payload.profiles.size)
        assertEquals("mieru", payload.profiles.single().protocol)
        assertEquals(2012, payload.profiles.single().port)
        assertTrue(payload.profiles.single().config.contains("server_ports"))
    }

    @Test
    fun `allows a larger document only through the explicit file import limit`() {
        val document = """{"outbounds":[{"type":"naive","tag":"naive","server":"naive.example","server_port":443,"username":"fixture-user","password":"fixture-password","tls":{"enabled":true}}],"padding":"${"x".repeat(4_100)}"}"""

        assertTrue(runCatching { ImportPayloadClassifier.classify(document) }.isFailure)
        assertTrue(
            ImportPayloadClassifier.classify(document, maxLength = ImportPayloadClassifier.MAX_FILE_LENGTH)
                is ImportPayload.Profiles,
        )
    }

    @Test
    fun `rejects a sing-box document without supported proxy outbounds`() {
        val failure = runCatching {
            ImportPayloadClassifier.classify("""{"outbounds":[{"type":"direct","tag":"direct"}]}""")
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `QR rejects multiple direct URIs`() {
        val failure = runCatching {
            QrImportClassifier.classify(
                "vless://00000000-0000-4000-8000-000000000001@first.example:443?security=tls#First\n" +
                    "vless://00000000-0000-4000-8000-000000000002@second.example:8443?security=tls#Second",
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }
}
