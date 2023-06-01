/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link FieldName}.
 */
public class FieldNameTest {

  @Test
  public void test_of() {
    FieldName yieldToMaturity = FieldName.of("YieldToMaturity");
    assertThat(yieldToMaturity).isEqualTo(FieldName.YIELD_TO_MATURITY);
    FieldName parYield = FieldName.of("ParYield");
    assertThat(parYield).isEqualTo(FieldName.PAR_YIELD);
    FieldName cleanRealPrice = FieldName.of("CleanRealPrice");
    assertThat(cleanRealPrice).isEqualTo(FieldName.CLEAN_REAL_PRICE);
    FieldName cleanNominalPrice = FieldName.of("CleanNominalPrice");
    assertThat(cleanNominalPrice).isEqualTo(FieldName.CLEAN_NOMINAL_PRICE);
    FieldName realYieldToMaturity = FieldName.of("RealYieldToMaturity");
    assertThat(realYieldToMaturity).isEqualTo(FieldName.REAL_YIELD_TO_MATURITY);
    FieldName nominalYieldToMaturity = FieldName.of("NominalYieldToMaturity");
    assertThat(nominalYieldToMaturity).isEqualTo(FieldName.NOMINAL_YIELD_TO_MATURITY);
  }

  //-----------------------------------------------------------------------
  @Test
  public void coverage() {
    FieldName test = FieldName.of("Foo");
    assertThat(test.toString()).isEqualTo("Foo");
    assertSerialization(test);
    assertJodaConvert(FieldName.class, test);
  }

}
