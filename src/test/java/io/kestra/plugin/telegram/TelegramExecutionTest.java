package io.kestra.plugin.telegram;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.repositories.LocalFlowRepositoryLoader;
import io.kestra.core.runners.TestRunner;
import io.kestra.core.utils.Await;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

/**
 * This test will only test the main task, this allow you to send any input
 * parameters to your task and test the returning behaviour easily.
 */
@KestraTest
class TelegramExecutionTest extends AbstractTelegramTest {
    @Inject
    protected TestRunner runner;
    @Inject
    protected LocalFlowRepositoryLoader repositoryLoader;;

    @BeforeEach
    protected void init() throws IOException, URISyntaxException {
        repositoryLoader.load(Objects.requireNonNull(TelegramExecutionTest.class.getClassLoader().getResource("flows")));
        this.runner.run();
        FakeTelegramController.message = null;
    }

    @Test
    void flow() throws Exception {
        var failedExecution = runAndCaptureExecution(
            "main-flow-that-fails",
            "telegram"
        );

        try {
            Await.until(
                () -> FakeTelegramController.message,
                Duration.ofMillis(100),
                Duration.ofSeconds(5)
            );
        } catch (TimeoutException e) {
            throw new RuntimeException("Timed out waiting for FakeTelegramController.message to be set", e);
        }

        assertThat(FakeTelegramController.token, comparesEqualTo("token"));
        assertThat(FakeTelegramController.message.getChatId(), comparesEqualTo("channel"));
        assertThat(FakeTelegramController.message.getText(), containsString("<io.kestra.tests main-flow-that-fails"));
        assertThat(FakeTelegramController.message.getText(), containsString("Failed on task `failed`"));
        assertThat(FakeTelegramController.message.getText(), containsString("Final task ID ➛ failed"));
        assertThat(FakeTelegramController.message.getText(), containsString("\n\nDisk is \"almost\" full\nenv: prod\nretries: 3"));
    }

    // With parseMode set, the notification must be escaped for that mode, or Telegram rejects it
    @Test
    void html_escapesTheNotification() throws Exception {
        var text = captureText("parse-mode-html-source", "telegram-parse-mode-html");

        assertThat(FakeTelegramController.message.getParseMode(), is("HTML"));
        assertThat(text, startsWith("&lt;io.kestra.tests parse-mode-html-source "));
        assertThat(text, containsString("Final task ID ➛ failed_step&gt; Failed on task `failed_step` after "));
        assertThat(text, containsString("\n\nDisk &lt;90% &amp; \"almost\" full (v1.2)!"));
    }

    @Test
    void markdownV2_escapesTheNotification() throws Exception {
        var text = captureText("parse-mode-markdownv2-source", "telegram-parse-mode-markdownv2");

        assertThat(FakeTelegramController.message.getParseMode(), is("MarkdownV2"));
        assertThat(text, startsWith("<io\\.kestra\\.tests parse\\-mode\\-markdownv2\\-source "));
        assertThat(text, containsString("Final task ID ➛ failed\\_step\\> Failed on task \\`failed\\_step\\` after "));
        assertThat(text, containsString("\n\nDisk <90% & \"almost\" full \\(v1\\.2\\)\\!"));
    }

    private String captureText(String sourceFlowId, String notificationFlowId) throws Exception {
        runAndCaptureExecution(sourceFlowId, notificationFlowId);
        Await.until(() -> FakeTelegramController.message, Duration.ofMillis(100), Duration.ofSeconds(5));
        return FakeTelegramController.message.getText();
    }
}
