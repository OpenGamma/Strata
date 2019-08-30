/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link EtdVariant}.
 */
public class EtdVariantTest {

  @Test
  public void test_monthly() {
    EtdVariant test = EtdVariant.ofMonthly();
    assertThat(test.getType()).isEqualTo(EtdExpiryType.MONTHLY);
    assertThat(test.getDateCode()).isNotPresent();
    assertThat(test.getSettlementType()).isNotPresent();
    assertThat(test.getOptionType()).isNotPresent();
    assertThat(test.isFlex()).isFalse();
    assertThat(test.getCode()).isEmpty();
  }

  @Test
  public void test_weekly() {
    EtdVariant test = EtdVariant.ofWeekly(2);
    assertThat(test.getType()).isEqualTo(EtdExpiryType.WEEKLY);
    assertThat(test.getDateCode().getAsInt()).isEqualTo(2);
    assertThat(test.getSettlementType()).isNotPresent();
    assertThat(test.getOptionType()).isNotPresent();
    assertThat(test.isFlex()).isFalse();
    assertThat(test.getCode()).isEqualTo("W2");
  }

  @Test
  public void test_daily() {
    EtdVariant test = EtdVariant.ofDaily(24);
    assertThat(test.getType()).isEqualTo(EtdExpiryType.DAILY);
    assertThat(test.getDateCode().getAsInt()).isEqualTo(24);
    assertThat(test.getSettlementType()).isNotPresent();
    assertThat(test.getOptionType()).isNotPresent();
    assertThat(test.isFlex()).isFalse();
    assertThat(test.getCode()).isEqualTo("24");
  }

  @Test
  public void test_flexFuture() {
    EtdVariant test = EtdVariant.ofFlexFuture(2, EtdSettlementType.CASH);
    assertThat(test.getType()).isEqualTo(EtdExpiryType.DAILY);
    assertThat(test.getDateCode().getAsInt()).isEqualTo(2);
    assertThat(test.getSettlementType().get()).isEqualTo(EtdSettlementType.CASH);
    assertThat(test.getOptionType()).isNotPresent();
    assertThat(test.isFlex()).isTrue();
    assertThat(test.getCode()).isEqualTo("02C");
  }

  @Test
  public void test_flexOption() {
    EtdVariant test = EtdVariant.ofFlexOption(24, EtdSettlementType.CASH, EtdOptionType.AMERICAN);
    assertThat(test.getType()).isEqualTo(EtdExpiryType.DAILY);
    assertThat(test.getDateCode().getAsInt()).isEqualTo(24);
    assertThat(test.getSettlementType().get()).isEqualTo(EtdSettlementType.CASH);
    assertThat(test.getOptionType().get()).isEqualTo(EtdOptionType.AMERICAN);
    assertThat(test.isFlex()).isTrue();
    assertThat(test.getCode()).isEqualTo("24CA");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  @Test
  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static EtdVariant sut() {
    return EtdVariant.MONTHLY;
  }

  static EtdVariant sut2() {
    return EtdVariant.ofFlexOption(6, EtdSettlementType.CASH, EtdOptionType.EUROPEAN);
  }

}
