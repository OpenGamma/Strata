/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.product.common.PutCall;

/**
 * Test {@link SplitEtdOption}.
 */
public class SplitEtdOptionTest {

  @Test
  public void test() {
    SplitEtdOption test = sut();
    assertThat(test.getUnderlyingExpiryMonth()).isEmpty();
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  //-------------------------------------------------------------------------
  static SplitEtdOption sut() {
    return SplitEtdOption.of(0, PutCall.PUT, 1.23);
  }

  static SplitEtdOption sut2() {
    return SplitEtdOption.of(1, PutCall.CALL, 2.34, YearMonth.of(2021, 1));
  }

}
