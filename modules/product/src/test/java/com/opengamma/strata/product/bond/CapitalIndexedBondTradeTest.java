/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ICMA;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;
import static com.opengamma.strata.basics.index.PriceIndices.US_CPI_U;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.bond.YieldConvention.US_IL_REAL;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

import com.google.common.reflect.TypeToken;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.id.IdentifiableBean;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityLink;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.UnitSecurity;
import com.opengamma.strata.product.swap.InflationRateCalculation;

/**
 * Test {@link CapitalIndexedBondTrade}. 
 */
@Test
public class CapitalIndexedBondTradeTest {

  private static final long QUANTITY = 10;
  private static final double NOTIONAL = 10_000_000d;
  private static final double START_INDEX = 198.475;
  private static final LocalDate START = LocalDate.of(2006, 1, 15);
  private static final LocalDate END = LocalDate.of(2016, 1, 15);
  private static final ValueSchedule COUPON = ValueSchedule.of(0.02);
  private static final InflationRateCalculation RATE_CALC = InflationRateCalculation.builder()
      .gearing(COUPON)
      .index(US_CPI_U)
      .lag(Period.ofMonths(3))
      .interpolated(true)
      .build();
  private static final DaysAdjustment SETTLE_OFFSET = DaysAdjustment.ofBusinessDays(2, USNY);
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG-Ticker", "US-Govt");
  private static final Frequency FREQUENCY = Frequency.P6M;
  private static final BusinessDayAdjustment SCHEDULE_ADJ =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, USNY);
  private static final PeriodicSchedule SCHEDULE =
      PeriodicSchedule.of(START, END, FREQUENCY, SCHEDULE_ADJ, StubConvention.NONE, RollConventions.NONE);
  private static final CapitalIndexedBond PRODUCT = CapitalIndexedBond.builder()
      .notional(NOTIONAL)
      .currency(USD)
      .dayCount(ACT_ACT_ICMA)
      .rateCalculation(RATE_CALC)
      .legalEntityId(LEGAL_ENTITY)
      .yieldConvention(US_IL_REAL)
      .settlementDateOffset(SETTLE_OFFSET)
      .periodicSchedule(SCHEDULE)
      .startIndexValue(START_INDEX)
      .build();
  private static final StandardId SECURITY_ID = StandardId.of("OG-Ticker", "BOND1");
  private static final Security<CapitalIndexedBond> SECURITY = UnitSecurity.builder(PRODUCT).standardId(SECURITY_ID).build();
  private static final SecurityLink<CapitalIndexedBond> LINK_RESOLVABLE = 
      SecurityLink.resolvable(SECURITY_ID, CapitalIndexedBond.class);
  private static final SecurityLink<CapitalIndexedBond> LINK_RESOLVED = SecurityLink.resolved(SECURITY);
  private static final LocalDate TRADE = LocalDate.of(2007, 1, 8);
  private static final LocalDate SETTLEMENT_DATE = SCHEDULE_ADJ.adjust(TRADE);
  private static final CapitalIndexedBondPaymentPeriod SETTLEMENT = CapitalIndexedBondPaymentPeriod.builder()
      .startDate(SCHEDULE_ADJ.adjust(START))
      .unadjustedStartDate(START)
      .endDate(SETTLEMENT_DATE)
      .currency(USD)
      .rateObservation(RATE_CALC.createRateObservation(SETTLEMENT_DATE, START_INDEX))
      .notional(-NOTIONAL * QUANTITY)
      .realCoupon(1d)
      .build();
  private static final TradeInfo TRADE_INFO =
      TradeInfo.builder().tradeDate(TRADE).settlementDate(SETTLEMENT_DATE).build();

  private static final LinkResolver RESOLVER = new LinkResolver() {
    @SuppressWarnings("unchecked")
    @Override
    public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
      assertEquals(identifier, SECURITY_ID);
      return (T) SECURITY;
    }
  };

  //-------------------------------------------------------------------------
  public void test_builder_resolved() {
    CapitalIndexedBondTrade test = CapitalIndexedBondTrade.builder()
        .securityLink(LINK_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .settlement(SETTLEMENT)
        .build();
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getSecurityLink(), LINK_RESOLVED);
    assertEquals(test.getSettlement(), SETTLEMENT);
  }

  public void test_builder_resolvable() {
    CapitalIndexedBondTrade test = CapitalIndexedBondTrade.builder()
        .securityLink(LINK_RESOLVABLE)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .settlement(SETTLEMENT)
        .build();
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getSecurityLink(), LINK_RESOLVABLE);
    assertThrows(() -> test.getSecurity(), IllegalStateException.class);
    assertThrows(() -> test.getProduct(), IllegalStateException.class);
  }

  public void test_of() {
    CapitalIndexedBondTrade test = CapitalIndexedBondTrade.of(LINK_RESOLVED, TRADE_INFO, QUANTITY);
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getSecurityLink(), LINK_RESOLVED);
    assertEquals(test.getSettlement(), SETTLEMENT);
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinks_resolved() {
    CapitalIndexedBondTrade base = CapitalIndexedBondTrade.builder()
        .securityLink(LINK_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .settlement(SETTLEMENT)
        .build();
    assertEquals(base.resolveLinks(RESOLVER), base);
  }

  public void test_resolveLinks_resolvable() {
    CapitalIndexedBondTrade base = CapitalIndexedBondTrade.builder()
        .securityLink(LINK_RESOLVABLE)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .settlement(SETTLEMENT)
        .build();
    CapitalIndexedBondTrade expected = CapitalIndexedBondTrade.builder()
        .securityLink(LINK_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .settlement(SETTLEMENT)
        .build();
    assertEquals(base.resolveLinks(RESOLVER), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CapitalIndexedBondTrade test1 = CapitalIndexedBondTrade.builder()
        .securityLink(LINK_RESOLVABLE)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .settlement(SETTLEMENT)
        .build();
    coverImmutableBean(test1);
    CapitalIndexedBondTrade test2 = CapitalIndexedBondTrade.builder()
        .securityLink(SecurityLink.resolved(UnitSecurity.builder(PRODUCT)
            .standardId(StandardId.of("Ticker", "GOV1-BND1"))
            .build()))
        .quantity(100L)
        .settlement(
            CapitalIndexedBondPaymentPeriod.builder()
                .startDate(START)
                .endDate(START.plusDays(7))
                .currency(USD)
                .rateObservation(RATE_CALC.createRateObservation(START.plusDays(7), START_INDEX))
                .notional(-1.e10)
                .build())
        .tradeInfo(TradeInfo.builder().settlementDate(START.plusDays(7)).build())
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    CapitalIndexedBondTrade test = CapitalIndexedBondTrade.builder()
        .securityLink(LINK_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .settlement(SETTLEMENT)
        .build();
    assertSerialization(test);
  }

}
