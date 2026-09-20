package com.example.plantos.backend.alert;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

class AnomalyParserTests {
    private static ValidatorFactory validators;
    private AnomalyParser parser;

    @BeforeAll
    static void openValidators() { validators = Validation.buildDefaultValidatorFactory(); }
    @AfterAll
    static void closeValidators() { validators.close(); }
    @BeforeEach
    void setup() { parser = new AnomalyParser(JsonMapper.builder().build(), validators.getValidator()); }

    @Test
    void readsDotnetContractIncludingOffsetTimestamps() {
        var event = parser.parse("TEST-PRESS", AnomalyTestData.JSON);
        assertThat(event.anomalyType()).isEqualTo(AnomalyType.HIGH_TEMPERATURE);
        assertThat(event.observedValue()).isEqualTo(94.83);
        assertThat(event.occurredAt().toString()).isEqualTo("2026-09-18T10:00:00Z");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "null", "{}", "{bad", "[]"})
    void rejectsMalformedOrMissingPayloads(String payload) {
        assertThatThrownBy(() -> parser.parse("TEST-PRESS", payload)).isInstanceOf(InvalidAnomalyException.class);
    }

    @ParameterizedTest
    @CsvSource({"CRITICAL,HEALTHY", "HIGH_TEMPERATURE,UNKNOWN", "MachineAnomaly,MachineHealth",
            "94.83,89", "34,101", "TEST-PRESS,OTHER", "\"schemaVersion\":1,\"schemaVersion\":2",
            "\"unit\":\"C\",\"unit\":\"mm/s\"",
            "9e2f353b-8f63-4568-9dce-9245126b9001,00000000-0000-0000-0000-000000000000"})
    void rejectsInvalidContractValues(String from, String to) {
        assertThatThrownBy(() -> parser.parse("TEST-PRESS", AnomalyTestData.JSON.replace(from, to)))
                .isInstanceOf(InvalidAnomalyException.class);
    }

    @Test
    void rejectsMissingNumericFieldInsteadOfTreatingItAsZero() {
        assertThatThrownBy(() -> parser.parse("TEST-PRESS", AnomalyTestData.JSON.replace(
                ",\"healthScore\":34", ""))).isInstanceOf(InvalidAnomalyException.class);
    }

    @Test
    void thresholdEqualityIsValidAndMissingKeyIsNot() {
        assertThat(parser.parse("TEST-PRESS", AnomalyTestData.JSON.replace("94.83", "90")).observedValue())
                .isEqualTo(90);
        assertThatThrownBy(() -> parser.parse(null, AnomalyTestData.JSON)).isInstanceOf(InvalidAnomalyException.class);
    }
}
