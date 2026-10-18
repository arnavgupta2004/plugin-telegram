package io.kestra.plugin.telegram;

import org.junit.jupiter.api.Test;

import io.kestra.plugin.telegram.TelegramSend.ParseMode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class TelegramTemplateEscapeTest {
    private static final String TEXT = "<company.team my-flow ➛ FAILED - Final task ID ➛ step_1> Failed on task `step_1` after PT1S (https://kestra.example/ui?a=1&b=2)";

    @Test
    void withoutParseMode_textIsUnchanged() {
        assertThat(TelegramTemplate.escape(TEXT, null), is(TEXT));
    }

    @Test
    void html_escapesAmpersandAndAngleBrackets() {
        assertThat(
            TelegramTemplate.escape(TEXT, ParseMode.HTML),
            is("&lt;company.team my-flow ➛ FAILED - Final task ID ➛ step_1&gt; Failed on task `step_1` after PT1S (https://kestra.example/ui?a=1&amp;b=2)")
        );
        assertThat(TelegramTemplate.escape("&lt; is already text", ParseMode.HTML), is("&amp;lt; is already text"));
    }

    @Test
    void markdownV2_escapesEveryReservedCharacter() {
        assertThat(
            TelegramTemplate.escape(TEXT, ParseMode.MARKDOWNV2),
            is("<company\\.team my\\-flow ➛ FAILED \\- Final task ID ➛ step\\_1\\> Failed on task \\`step\\_1\\` after PT1S \\(https://kestra\\.example/ui?a\\=1&b\\=2\\)")
        );
        assertThat(
            TelegramTemplate.escape("_*[]()~`>#+-=|{}.!\\", ParseMode.MARKDOWNV2),
            is("\\_\\*\\[\\]\\(\\)\\~\\`\\>\\#\\+\\-\\=\\|\\{\\}\\.\\!\\\\")
        );
    }
}
