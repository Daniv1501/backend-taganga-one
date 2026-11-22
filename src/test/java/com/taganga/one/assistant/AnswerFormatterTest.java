package com.taganga.one.assistant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerFormatterTest {

    @Test
    void shouldEscapeAnyHtmlFromTheModel() {
        String html = AnswerFormatter.toHtml("Hola <script>alert(1)</script> <img src=x onerror=alert(1)>");

        assertThat(html)
                .doesNotContain("<script", "<img")
                .contains("&lt;script&gt;", "&lt;img");
    }

    @Test
    void shouldConvertParagraphsBoldAndItalic() {
        assertThat(AnswerFormatter.toHtml("Respeta **el chinchorro**.\n\nY *disfruta* el mar."))
                .isEqualTo("<p>Respeta <b>el chinchorro</b>.</p><p>Y <i>disfruta</i> el mar.</p>");
    }

    @Test
    void shouldConvertBulletAndNumberedListsAfterAParagraph() {
        assertThat(AnswerFormatter.toHtml("Normas:\n- No bucear\n* No anclar\n1. Llévate tu basura"))
                .isEqualTo("<p>Normas:</p><ul><li>No bucear</li><li>No anclar</li><li>Llévate tu basura</li></ul>");
    }

    @Test
    void shouldJoinSingleLineBreaksAndTurnHeadingsIntoBold() {
        assertThat(AnswerFormatter.toHtml("## Isla Aguja\nSitio sagrado\nde la Línea Negra"))
                .isEqualTo("<p><b>Isla Aguja</b><br>Sitio sagrado<br>de la Línea Negra</p>");
    }

    @Test
    void shouldReturnEmptyForBlankText() {
        assertThat(AnswerFormatter.toHtml("  \n ")).isEmpty();
        assertThat(AnswerFormatter.toHtml(null)).isEmpty();
    }
}
