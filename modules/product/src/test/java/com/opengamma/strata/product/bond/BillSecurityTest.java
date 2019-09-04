/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.SecurityPriceInfo;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link BillSecurity}.
 */
public class BillSecurityTest {

  private static final BusinessDayAdjustment BUSINESS_ADJUST =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, USNY);
  private static final BillYieldConvention YIELD_CONVENTION = BillYieldConvention.DISCOUNT;
  private static final LegalEntityId LEGAL_ENTITY = LegalEntityId.of("OG-Ticker", "US GOVT");
  private static final Currency CCY = Currency.USD;
  private static final double NOTIONAL_AMOUNT = 1_000_000;
  private static final LocalDate MATURITY_DATE = LocalDate.of(2019, 5, 23);
  private static final AdjustableDate MATURITY_DATE_ADJ = AdjustableDate.of(MATURITY_DATE, BUSINESS_ADJUST);
  private static final AdjustablePayment NOTIONAL =
      AdjustablePayment.of(CurrencyAmount.of(CCY, NOTIONAL_AMOUNT), MATURITY_DATE_ADJ);
  private static final DayCount DAY_COUNT = DayCounts.ACT_360;
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Test", "Bill2019-05-23");
  private static final DaysAdjustment SETTLE = DaysAdjustment.ofBusinessDays(1, USNY, BUSINESS_ADJUST);
  private static final SecurityPriceInfo PRICE_INFO = SecurityPriceInfo.of(0.1, CurrencyAmount.of(CCY, 25));
  private static final SecurityInfo INFO = SecurityInfo.of(SECURITY_ID, PRICE_INFO);

  @Test
  public void test_builder() {
    BillSecurity test = BillSecurity.builder()
        .dayCount(DAY_COUNT)
        .info(INFO)
        .legalEntityId(LEGAL_ENTITY)
        .notional(NOTIONAL)
        .settlementDateOffset(SETTLE)
        .yieldConvention(YIELD_CONVENTION)
        .build();
    assertThat(test.getCurrency()).isEqualTo(CCY);
    assertThat(test.getDayCount()).isEqualTo(DAY_COUNT);
    assertThat(test.getInfo()).isEqualTo(INFO);
    assertThat(test.getLegalEntityId()).isEqualTo(LEGAL_ENTITY);
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getSecurityId()).isEqualTo(SECURITY_ID);
    assertThat(test.getSettlementDateOffset()).isEqualTo(SETTLE);
    assertThat(test.getUnderlyingIds()).isEmpty();
    assertThat(test.getYieldConvention()).isEqualTo(YIELD_CONVENTION);
  }

  @Test
  public void test_builder_fail() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BillSecurity.builder()
            .dayCount(DAY_COUNT)
            .info(INFO)
            .legalEntityId(LEGAL_ENTITY)
            .notional(NOTIONAL)
            .settlementDateOffset(DaysAdjustment.ofBusinessDays(-1, USNY, BUSINESS_ADJUST))
            .yieldConvention(YIELD_CONVENTION)
            .build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BillSecurity.builder()
            .dayCount(DAY_COUNT)
            .info(INFO)
            .legalEntityId(LEGAL_ENTITY)
            .notional(AdjustablePayment.of(CurrencyAmount.of(CCY, -2_000_000), MATURITY_DATE_ADJ))
            .settlementDateOffset(SETTLE)
            .yieldConvention(YIELD_CONVENTION)
            .build());
  }

  @Test
  public void test_withInfo() {
    BillSecurity base = BillSecurity.builder()
        .dayCount(DAY_COUNT)
        .info(INFO)
        .legalEntityId(LEGAL_ENTITY)
        .notional(NOTIONAL)
        .settlementDateOffset(SETTLE)
        .yieldConvention(YIELD_CONVENTION)
        .build();
    SecurityInfo info = SecurityInfo.of(SECURITY_ID, SecurityPriceInfo.ofCurrencyMinorUnit(CCY));
    BillSecurity expected = BillSecurity.builder()
        .dayCount(DAY_COUNT)
        .info(info)
        .legalEntityId(LEGAL_ENTITY)
        .notional(NOTIONAL)
        .settlementDateOffset(SETTLE)
        .yieldConvention(YIELD_CONVENTION)
        .build();
    assertThat(base.withInfo(info)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createProduct() {
    BillSecurity base = BillSecurity.builder()
        .dayCount(DAY_COUNT)
        .info(INFO)
        .legalEntityId(LEGAL_ENTITY)
        .notional(NOTIONAL)
        .settlementDateOffset(SETTLE)
        .yieldConvention(YIELD_CONVENTION)
        .build();
    Bill expectedProduct = Bill.builder()
        .dayCount(DAY_COUNT)
        .securityId(SECURITY_ID)
        .dayCount(DAY_COUNT)
        .legalEntityId(LEGAL_ENTITY)
        .notional(NOTIONAL)
        .settlementDateOffset(SETTLE)
        .yieldConvention(YIELD_CONVENTION)
        .build();
    assertThat(base.createProduct(ReferenceData.empty())).isEqualTo(expectedProduct);
    TradeInfo tradeInfo = TradeInfo.of(date(2016, 6, 30));
    BillTrade expectedTrade = BillTrade.builder()
        .info(tradeInfo)
        .product(expectedProduct)
        .quantity(100)
        .price(1.235)
        .build();
    assertThat(base.createTrade(tradeInfo, 100, 1.235, ReferenceData.empty())).isEqualTo(expectedTrade);
  }

  @Test
  public void test_createPosition() {
    BillSecurity test = BillSecurity.builder()
        .dayCount(DAY_COUNT)
        .info(INFO)
        .legalEntityId(LEGAL_ENTITY)
        .notional(NOTIONAL)
        .settlementDateOffset(SETTLE)
        .yieldConvention(YIELD_CONVENTION)
        .build();
    Bill product = Bill.builder()
        .dayCount(DAY_COUNT)
        .securityId(SECURITY_ID)
        .dayCount(DAY_COUNT)
        .legalEntityId(LEGAL_ENTITY)
        .notional(NOTIONAL)
        .settlementDateOffset(SETTLE)
        .yieldConvention(YIELD_CONVENTION)
        .build();
    PositionInfo positionInfo = PositionInfo.empty();
    BillPosition expectedPosition1 = BillPosition.builder()
        .info(positionInfo)
        .product(product)
        .longQuantity(100)
        .build();
    assertThat(test.createPosition(positionInfo, 100, ReferenceData.empty())).isEqualTo(expectedPosition1);
    BillPosition expectedPosition2 = BillPosition.builder()
        .info(positionInfo)
        .product(product)
        .longQuantity(100)
        .shortQuantity(50)
        .build();
    assertThat(test.createPosition(positionInfo, 100, 50, ReferenceData.empty())).isEqualTo(expectedPosition2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    BillSecurity test1 = BillSecurity.builder()
        .dayCount(DAY_COUNT)
        .info(INFO)
        .legalEntityId(LEGAL_ENTITY)
        .notional(NOTIONAL)
        .settlementDateOffset(SETTLE)
        .yieldConvention(YIELD_CONVENTION)
        .build();
    coverImmutableBean(test1);
    BillSecurity test2 = BillSecurity.builder()
        .dayCount(DayCounts.ACT_365F)
        .info(SecurityInfo.of(SecurityId.of("OG-Test", "ID2"), PRICE_INFO))
        .legalEntityId(LegalEntityId.of("OG-Ticker", "LE2"))
        .notional(AdjustablePayment.of(CurrencyAmount.of(CCY, 10), MATURITY_DATE_ADJ))
        .settlementDateOffset(DaysAdjustment.ofBusinessDays(2, EUTA, BUSINESS_ADJUST))
        .yieldConvention(BillYieldConvention.INTEREST_AT_MATURITY)
        .build();
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    BillSecurity test = BillSecurity.builder()
        .dayCount(DAY_COUNT)
        .info(INFO)
        .legalEntityId(LEGAL_ENTITY)
        .notional(NOTIONAL)
        .settlementDateOffset(SETTLE)
        .yieldConvention(YIELD_CONVENTION)
        .build();
    assertSerialization(test);
  }

}
