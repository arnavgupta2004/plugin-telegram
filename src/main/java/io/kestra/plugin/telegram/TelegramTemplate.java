package io.kestra.plugin.telegram;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class TelegramTemplate extends TelegramSend {
    // https://core.telegram.org/bots/api#markdownv2-style
    private static final Pattern MARKDOWNV2_RESERVED = Pattern.compile("[_*\\[\\]()~`>#+\\-=|{}.!\\\\]");

    @Schema(
        title = "Template resource path",
        description = "Classpath Pebble template used to build the message payload before sending.",
        hidden = true
    )
    @PluginProperty(group = "advanced")
    protected Property<String> templateUri;

    @Schema(
        title = "Template variables",
        description = "Map of variables rendered and passed to the Pebble template; defaults to an empty map when not provided."
    )
    @PluginProperty(group = "advanced")
    protected Property<Map<String, Object>> templateRenderMap;

    @SuppressWarnings("unchecked")
    @Override
    public VoidOutput run(RunContext runContext) throws Exception {

        final var renderedTemplateUri = runContext.render(this.templateUri).as(String.class);
        if (renderedTemplateUri.isPresent()) {
            String template = IOUtils.toString(
                Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream(renderedTemplateUri.get())),
                StandardCharsets.UTF_8
            );

            var rText = runContext.render(
                template, templateRenderMap != null ? runContext.render(templateRenderMap).asMap(String.class, Object.class) : Map.of()
            );
            this.payload = Property.ofValue(escape(rText, renderParseMode(runContext).orElse(null)));
        }

        return super.run(runContext);
    }

    /**
     * The template renders plain text, so escape it for the parse mode Telegram will apply. Escaping happens
     * after the template has shortened the text, as Telegram counts the 4096 characters after parsing.
     */
    static String escape(String text, ParseMode parseMode) {
        if (parseMode == null) {
            return text;
        }

        return switch (parseMode) {
            case HTML -> text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            case MARKDOWNV2 -> MARKDOWNV2_RESERVED.matcher(text).replaceAll("\\\\$0");
        };
    }
}
