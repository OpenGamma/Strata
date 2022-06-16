/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link Decimal}.
 */
public class FixedScaleDecimalTest {

  private static final Object DUMMY_OBJECT = "";

  public static Object[][] dataValues() {
    return new Object[][] {
        {"1", 0, "1"},
        {"1.2", 1, "1.2"},
        {"1.2", 2, "1.20"},
        {"1.2", 3, "1.200"},
        {"-2", 0, "-2"},
        {"-2.3", 1, "-2.3"},
        {"-2.3", 2, "-2.30"},
    };
  }

  @ParameterizedTest
  @MethodSource("dataValues")
  public void testValues(String decimalStr, int fixedScale, String fixedStr) {
    Decimal decimal = Decimal.of(decimalStr);
    FixedScaleDecimal test = FixedScaleDecimal.of(decimal, fixedScale);
    assertThat(test.decimal()).isEqualTo(decimal.roundToScale(fixedScale, RoundingMode.HALF_UP));
    assertThat(test.fixedScale()).isEqualTo(fixedScale);
    assertThat(test.toBigDecimal()).isEqualTo(new BigDecimal(decimalStr).setScale(fixedScale, RoundingMode.HALF_UP));
    assertThat(test.equals(DUMMY_OBJECT)).isFalse();
    assertThat(test.equals(test)).isTrue();
    assertThat(test.equals(FixedScaleDecimal.parse("0"))).isFalse();
    assertThat(test).doesNotHaveSameHashCodeAs(FixedScaleDecimal.parse("0"));
    assertThat(test.toString()).isEqualTo(fixedStr);
  }

  @Test
  public void testMap() {
    FixedScaleDecimal test = FixedScaleDecimal.parse("1.25");
    assertThat(test.map(d -> d.multipliedBy(2))).isEqualTo(FixedScaleDecimal.parse("2.50"));
  }

  @Test
  public void testParse() {
    assertThat(FixedScaleDecimal.parse("1.25")).isEqualTo(FixedScaleDecimal.of(Decimal.parse("1.25"), 2));
    assertThat(FixedScaleDecimal.parse("1.20")).isEqualTo(FixedScaleDecimal.of(Decimal.parse("1.2"), 2));
    assertThat(FixedScaleDecimal.parse("1")).isEqualTo(FixedScaleDecimal.of(Decimal.parse("1"), 0));
  }

  public static Object[][] dataBad() {
    return new Object[][] {
        {"1.2", 0},
        {"1.2567", 0},
        {"1.2", -1},
        {"1.2", 19},
    };
  }

  @ParameterizedTest
  @MethodSource("dataBad")
  public void testBad(String decimalStr, int fixedScale) {
    assertThatIllegalArgumentException().isThrownBy(() -> FixedScaleDecimal.of(Decimal.of(decimalStr), fixedScale));
  }

  //-------------------------------------------------------------------------
  @Test
  public void testCompareTo() {
    assertThat(FixedScaleDecimal.parse("1.20"))
        .isEqualByComparingTo(FixedScaleDecimal.of(Decimal.of("1.2"), 2))
        .isLessThan(FixedScaleDecimal.parse("1.21"))
        .isGreaterThan(FixedScaleDecimal.parse("1.19"));
  }

  @Test
  public void testEquals() {
    FixedScaleDecimal test1 = FixedScaleDecimal.parse("1.2");
    FixedScaleDecimal test2 = FixedScaleDecimal.parse("1.20");
    assertThat(test1).isEqualTo(FixedScaleDecimal.parse("1.2"));
    assertThat(test1).isNotEqualTo(test2);
  }

}
