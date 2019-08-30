/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swaption;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.product.common.SettlementType;

/**
 * Test {@link CashSwaptionSettlement}.
 */
public class CashSwaptionSettlementTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    CashSwaptionSettlement test = CashSwaptionSettlement.of(date(2015, 6, 30), CashSwaptionSettlementMethod.CASH_PRICE);
    assertThat(test.getMethod()).isEqualTo(CashSwaptionSettlementMethod.CASH_PRICE);
    assertThat(test.getSettlementDate()).isEqualTo(date(2015, 6, 30));
    assertThat(test.getSettlementType()).isEqualTo(SettlementType.CASH);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    CashSwaptionSettlement test = CashSwaptionSettlement.of(date(2015, 6, 30), CashSwaptionSettlementMethod.CASH_PRICE);
    coverImmutableBean(test);
    CashSwaptionSettlement test2 = CashSwaptionSettlement.of(date(2015, 7, 30), CashSwaptionSettlementMethod.PAR_YIELD);
    coverBeanEquals(test, test2);
    coverEnum(CashSwaptionSettlementMethod.class);
    coverEnum(SettlementType.class);
  }

  @Test
  public void test_serialization() {
    CashSwaptionSettlement test = CashSwaptionSettlement.of(date(2015, 6, 30), CashSwaptionSettlementMethod.CASH_PRICE);
    assertSerialization(test);
  }

}
