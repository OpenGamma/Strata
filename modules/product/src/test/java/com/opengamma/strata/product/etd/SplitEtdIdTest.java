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

import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.common.ExchangeId;

/**
 * Test {@link SplitEtdId}.
 */
public class SplitEtdIdTest {

  @Test
  public void test() {
    SplitEtdId test = sut();
    assertThat(test.getOption()).isEmpty();
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  //-------------------------------------------------------------------------
  static SplitEtdId sut() {
    return SplitEtdId.builder()
        .securityId(SecurityId.of("A", "B"))
        .type(EtdType.FUTURE)
        .exchangeId(ExchangeId.of("XLON"))
        .contractCode(EtdContractCode.of("AB"))
        .expiry(YearMonth.of(2020, 6))
        .variant(EtdVariant.ofMonthly())
        .build();
  }

  static SplitEtdId sut2() {
    return SplitEtdId.builder()
        .securityId(SecurityId.of("A", "C"))
        .type(EtdType.OPTION)
        .exchangeId(ExchangeId.of("XPAR"))
        .contractCode(EtdContractCode.of("BA"))
        .expiry(YearMonth.of(2020, 7))
        .variant(EtdVariant.ofWeekly(1))
        .option(SplitEtdOptionTest.sut())
        .build();
  }

}
