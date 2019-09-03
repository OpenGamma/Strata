/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit.type;

import static com.opengamma.strata.basics.index.IborIndices.EUR_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.deposit.IborFixingDeposit;
import com.opengamma.strata.product.deposit.IborFixingDepositTrade;

/**
 * Test {@link IborFixingDepositTemplate}.
 */
public class IborFixingDepositTemplateTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final IborFixingDepositConvention CONVENTION = IborFixingDepositConvention.of(EUR_LIBOR_3M);

  @Test
  public void test_builder() {
    IborFixingDepositTemplate test = IborFixingDepositTemplate.builder()
        .convention(CONVENTION)
        .depositPeriod(Period.ofMonths(1))
        .build();
    assertThat(test.getConvention()).isEqualTo(CONVENTION);
    assertThat(test.getDepositPeriod()).isEqualTo(Period.ofMonths(1));
  }

  @Test
  public void test_builder_noPeriod() {
    IborFixingDepositTemplate test = IborFixingDepositTemplate.builder()
        .convention(CONVENTION)
        .build();
    assertThat(test.getConvention()).isEqualTo(CONVENTION);
    assertThat(test.getDepositPeriod()).isEqualTo(EUR_LIBOR_3M.getTenor().getPeriod());
  }

  @Test
  public void test_build_negativePeriod() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborFixingDepositTemplate.builder()
            .convention(CONVENTION)
            .depositPeriod(Period.ofMonths(-3))
            .build());
  }

  @Test
  public void test_of_index() {
    IborFixingDepositTemplate test = IborFixingDepositTemplate.of(EUR_LIBOR_3M);
    assertThat(test.getConvention()).isEqualTo(CONVENTION);
    assertThat(test.getDepositPeriod()).isEqualTo(EUR_LIBOR_3M.getTenor().getPeriod());
  }

  @Test
  public void test_of_periodAndIndex() {
    IborFixingDepositTemplate test = IborFixingDepositTemplate.of(Period.ofMonths(1), EUR_LIBOR_3M);
    assertThat(test.getConvention()).isEqualTo(CONVENTION);
    assertThat(test.getDepositPeriod()).isEqualTo(Period.ofMonths(1));
  }

  @Test
  public void test_createTrade() {
    IborFixingDepositTemplate template = IborFixingDepositTemplate.of(EUR_LIBOR_3M);
    double notional = 1d;
    double fixedRate = 0.045;
    LocalDate tradeDate = LocalDate.of(2015, 1, 22);
    IborFixingDepositTrade trade = template.createTrade(tradeDate, BUY, notional, fixedRate, REF_DATA);
    ImmutableIborFixingDepositConvention conv = (ImmutableIborFixingDepositConvention) template.getConvention();
    LocalDate startExpected = conv.getSpotDateOffset().adjust(tradeDate, REF_DATA);
    LocalDate endExpected = startExpected.plus(template.getDepositPeriod());
    IborFixingDeposit productExpected = IborFixingDeposit.builder()
        .businessDayAdjustment(conv.getBusinessDayAdjustment())
        .buySell(BUY)
        .startDate(startExpected)
        .endDate(endExpected)
        .fixedRate(fixedRate)
        .index(EUR_LIBOR_3M)
        .notional(notional)
        .build();
    TradeInfo tradeInfoExpected = TradeInfo.builder()
        .tradeDate(tradeDate)
        .build();
    assertThat(trade.getInfo()).isEqualTo(tradeInfoExpected);
    assertThat(trade.getProduct()).isEqualTo(productExpected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    IborFixingDepositTemplate test1 = IborFixingDepositTemplate.of(EUR_LIBOR_3M);
    coverImmutableBean(test1);
    IborFixingDepositTemplate test2 = IborFixingDepositTemplate.of(GBP_LIBOR_6M);
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    IborFixingDepositTemplate test = IborFixingDepositTemplate.of(EUR_LIBOR_3M);
    assertSerialization(test);
  }

}
