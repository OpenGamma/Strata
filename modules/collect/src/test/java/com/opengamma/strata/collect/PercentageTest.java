/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link Percentage}.
 */
class PercentageTest {

  static Object[][] data_values() {
    return new Object[][] {
        {Percentage.of(Decimal.of("0")), "0", Decimal.of("0")},
        {Percentage.of(Decimal.of("0.00")), "0", Decimal.of("0")},
        {Percentage.of(Decimal.of("1")), "1", Decimal.of("1")},
        {Percentage.of(Decimal.of("1.00000000001")), "1", Decimal.of("1")},
        {Percentage.of(Decimal.of("1.2")), "1.2", Decimal.of("1.2")},
        {Percentage.of(Decimal.of("1.2345")), "1.2345", Decimal.of("1.2345")},
        {Percentage.of(1.2345), "1.2345", Decimal.of("1.2345")},
        {Percentage.fromBasisPoints(BasisPoints.of(123.45)), "1.2345", Decimal.of("1.2345")},
        {Percentage.fromDecimalForm(0.012345), "1.2345", Decimal.of("1.2345")},
        {Percentage.fromDecimalForm(Decimal.of("0.012345")), "1.2345", Decimal.of("1.2345")},
    };
  }

  @ParameterizedTest
  @MethodSource("data_values")
  void test_values(Percentage test, String str, Decimal value) {
    assertThat(test.valuePercent()).isEqualTo(value);
    assertThat(test.toDecimalForm()).isEqualTo(value.multipliedBy(Decimal.ofScaled(1, 2)));
    assertThat(test.toBasisPoints()).isEqualTo(BasisPoints.of(value.multipliedBy(Decimal.of(100))));
    assertThat(test.toString()).isEqualTo(str + "%");
    assertThat(Percentage.parse(str + "%")).isEqualTo(test);
    assertThat(Percentage.parse(str + " %")).isEqualTo(test);
    assertThat(Percentage.parse(str + "pct")).isEqualTo(test);
    assertThat(Percentage.parse(str + " pct")).isEqualTo(test);
    assertThat(Percentage.parse(str)).isEqualTo(test);
    assertThat(Percentage.parse(str)).isLessThan(Percentage.parse("1000%"));
    assertThat(Percentage.parse(str)).isGreaterThan(Percentage.parse("-1000%"));

    assertThat(test.plus(Percentage.ZERO)).isEqualTo(test);
    assertThat(test.minus(Percentage.ZERO)).isEqualTo(test);
    assertThat(test.map(percentage -> percentage)).isEqualTo(test);
  }

  //-------------------------------------------------------------------------
  @Test
  void coverage() {
    Percentage test = Percentage.of(1.23);
    assertThat(test)
        .isEqualTo(Percentage.of(1.23))
        .isNotEqualTo(Percentage.of(1.24))
        .isNotEqualTo("")
        .isNotEqualTo(null)
        .hasSameHashCodeAs(Percentage.of(1.23))
        .doesNotHaveSameHashCodeAs(Percentage.of(1.24));
  }

}
