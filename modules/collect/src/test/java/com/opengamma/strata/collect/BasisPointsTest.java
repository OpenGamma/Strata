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
 * Test {@link BasisPoints}.
 */
class BasisPointsTest {

  static Object[][] data_values() {
    return new Object[][] {
        {BasisPoints.of(Decimal.of("0")), "0", Decimal.of("0")},
        {BasisPoints.of(Decimal.of("0.00")), "0", Decimal.of("0")},
        {BasisPoints.of(Decimal.of("1")), "1", Decimal.of("1")},
        {BasisPoints.of(Decimal.of("1.000000001")), "1", Decimal.of("1")},
        {BasisPoints.of(Decimal.of("1.2")), "1.2", Decimal.of("1.2")},
        {BasisPoints.of(Decimal.of("1.2345")), "1.2345", Decimal.of("1.2345")},
        {BasisPoints.of(1.2345), "1.2345", Decimal.of("1.2345")},
        {BasisPoints.fromPercentage(Percentage.of(1.2345)), "123.45", Decimal.of("123.45")},
        {BasisPoints.fromDecimalForm(0.012345), "123.45", Decimal.of("123.45")},
        {BasisPoints.fromDecimalForm(Decimal.of("0.012345")), "123.45", Decimal.of("123.45")},
    };
  }

  @ParameterizedTest
  @MethodSource("data_values")
  void test_values(BasisPoints test, String str, Decimal value) {
    assertThat(test.valueBasisPoints()).isEqualTo(value);
    assertThat(test.toDecimalForm()).isEqualTo(value.multipliedBy(Decimal.ofScaled(1, 4)));
    assertThat(test.toPercentage()).isEqualTo(Percentage.of(value.multipliedBy(Decimal.ofScaled(1, 2))));
    assertThat(test.toString()).isEqualTo(str + "bps");
    assertThat(BasisPoints.parse(str + "bps")).isEqualTo(test);
    assertThat(BasisPoints.parse(str + " bps")).isEqualTo(test);
    assertThat(BasisPoints.parse(str)).isEqualTo(test);
    assertThat(BasisPoints.parse(str)).isLessThan(BasisPoints.parse("1000bps"));
    assertThat(BasisPoints.parse(str)).isGreaterThan(BasisPoints.parse("-1000bps"));

    assertThat(test.plus(BasisPoints.ZERO)).isEqualTo(test);
    assertThat(test.minus(BasisPoints.ZERO)).isEqualTo(test);
    assertThat(test.map(percentage -> percentage)).isEqualTo(test);
  }

  //-------------------------------------------------------------------------
  @Test
  void coverage() {
    BasisPoints test = BasisPoints.of(1.23);
    assertThat(test)
        .isEqualTo(BasisPoints.of(1.23))
        .isNotEqualTo(BasisPoints.of(1.24))
        .isNotEqualTo("")
        .isNotEqualTo(null)
        .hasSameHashCodeAs(BasisPoints.of(1.23))
        .doesNotHaveSameHashCodeAs(BasisPoints.of(1.24));
  }

}
