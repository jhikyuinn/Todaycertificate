package com.todaycertificate.exam;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ShortAnswerGraderTest {

    @Test
    void ignoresCaseAndOuterSpaces() {
        assertThat(ShortAnswerGrader.isCorrect("DDL", false, "  ddl ")).isTrue();
    }

    @Test
    void collapsesInnerSpaces() {
        assertThat(ShortAnswerGrader.isCorrect("GROUP BY", false, "group   by")).isTrue();
    }

    @Test
    void acceptsAnyAlternative() {
        assertThat(ShortAnswerGrader.isCorrect("DDL|데이터 정의어", false, "데이터 정의어")).isTrue();
    }

    @Test
    void exactMatchKeepsCase() {
        assertThat(ShortAnswerGrader.isCorrect("Hello", true, "hello")).isFalse();
        assertThat(ShortAnswerGrader.isCorrect("Hello", true, " Hello ")).isTrue();
    }

    @Test
    void exactMatchKeepsInnerSpaces() {
        assertThat(ShortAnswerGrader.isCorrect("a  b", true, "a b")).isFalse();
    }

    @Test
    void blankIsWrong() {
        assertThat(ShortAnswerGrader.isCorrect("DDL", false, "   ")).isFalse();
        assertThat(ShortAnswerGrader.isCorrect("DDL", false, null)).isFalse();
    }
}
