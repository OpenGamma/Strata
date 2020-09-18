/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index.type;

import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DateSequences.MONTHLY_IMM;
import static com.opengamma.strata.basics.date.DateSequences.QUARTERLY_IMM;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_SOFR;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.swap.OvernightAccrualMethod.AVERAGED_DAILY;
import static com.opengamma.strata.product.swap.OvernightAccrualMethod.COMPOUNDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.SequenceDate;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.index.OvernightFuturePosition;
import com.opengamma.strata.product.index.OvernightFutureTrade;

/**
 * Tests {@link OvernightFutureContractSpec}.
 */
public class OvernightFutureContractSpecTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final BusinessDayAdjustment BDA_FOLLOW =
      BusinessDayAdjustment.of(FOLLOWING, USD_SOFR.getFixingCalendar());
  private static final BusinessDayAdjustment BDA_PRECEDE =
      BusinessDayAdjustment.of(BusinessDayConventions.PRECEDING, USD_SOFR.getFixingCalendar());

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    ImmutableOvernightFutureContractSpec test = ImmutableOvernightFutureContractSpec.builder()
        .name("USD-IMM")
        .index(USD_SOFR)
        .dateSequence(QUARTERLY_IMM)
        .accrualMethod(AVERAGED_DAILY)
        .notional(1_000_000d)
        .build();
    assertThat(test.getName()).isEqualTo("USD-IMM");
    assertThat(test.getIndex()).isEqualTo(USD_SOFR);
    assertThat(test.getDateSequence()).isEqualTo(QUARTERLY_IMM);
    assertThat(test.getAccrualMethod()).isEqualTo(AVERAGED_DAILY);
    assertThat(test.getStartDateAdjustment()).isEqualTo(BusinessDayAdjustment.NONE);
    assertThat(test.getEndDateAdjustment()).isEqualTo(DaysAdjustment.ofCalendarDays(-1));
    assertThat(test.getLastTradeDateAdjustment()).isEqualTo(DaysAdjustment.ofCalendarDays(-1, BDA_PRECEDE));
    assertThat(test.getNotional()).isEqualTo(1_000_000d);
  }

  @Test
  public void test_builder_incomplete() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ImmutableOvernightFutureContractSpec.builder()
            .index(USD_SOFR)
            .build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ImmutableOvernightFutureContractSpec.builder()
            .dateSequence(QUARTERLY_IMM)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createTrade_gbpSonia3mImmCme() {
    LocalDate tradeDate = date(2020, 1, 25);
    SequenceDate seqDate = SequenceDate.base(YearMonth.of(2020, 2));
    OvernightFutureContractSpec test = OvernightFutureContractSpecs.GBP_SONIA_3M_IMM_CME;
    OvernightFutureTrade trade = test.createTrade(
        tradeDate,
        SecurityId.of("OG", "1"),
        seqDate,
        20,
        0.999d,
        REF_DATA);
    assertThat(trade.getCurrency()).isEqualTo(Currency.GBP);
    assertThat(trade.getPrice()).isEqualTo(.999d);
    assertThat(trade.getQuantity()).isEqualTo(20);
    assertThat(trade.getProduct().getIndex()).isEqualTo(GBP_SONIA);
    assertThat(trade.getProduct().getAccrualMethod()).isEqualTo(COMPOUNDED);
    assertThat(trade.getProduct().getAccrualFactor()).isEqualTo(0.25);
    assertThat(trade.getProduct().getStartDate()).isEqualTo(date(2020, 3, 18));
    assertThat(trade.getProduct().getEndDate()).isEqualTo(date(2020, 6, 16));
    assertThat(trade.getProduct().getLastTradeDate()).isEqualTo(date(2020, 6, 17));
    assertThat(trade.getProduct().getNotional()).isEqualTo(1_000_000d);

    LocalDate startDate = test.calculateReferenceDate(tradeDate, seqDate, REF_DATA);
    LocalDate lastFixingDate = test.calculateLastFixingDate(startDate, REF_DATA);
    assertThat(startDate).isEqualTo(date(2020, 3, 18));
    assertThat(lastFixingDate).isEqualTo(date(2020, 6, 16));
  }

  @Test
  public void test_createTrade_gbpSonia3mImmIce() {
    OvernightFutureContractSpec test = OvernightFutureContractSpecs.GBP_SONIA_3M_IMM_ICE;
    OvernightFutureTrade trade = test.createTrade(
        date(2020, 1, 25),
        SecurityId.of("OG", "1"),
        SequenceDate.base(YearMonth.of(2020, 2)),
        20,
        0.999d,
        REF_DATA);
    assertThat(trade.getCurrency()).isEqualTo(Currency.GBP);
    assertThat(trade.getPrice()).isEqualTo(.999d);
    assertThat(trade.getQuantity()).isEqualTo(20);
    assertThat(trade.getProduct().getIndex()).isEqualTo(GBP_SONIA);
    assertThat(trade.getProduct().getAccrualMethod()).isEqualTo(COMPOUNDED);
    assertThat(trade.getProduct().getAccrualFactor()).isEqualTo(0.25);
    assertThat(trade.getProduct().getStartDate()).isEqualTo(date(2020, 3, 18));
    assertThat(trade.getProduct().getEndDate()).isEqualTo(date(2020, 6, 16));
    assertThat(trade.getProduct().getLastTradeDate()).isEqualTo(date(2020, 6, 16));
    assertThat(trade.getProduct().getNotional()).isEqualTo(1_000_000d);
  }

  @Test
  public void test_createTrade_gbpSonia3mImmLch() {
    OvernightFutureContractSpec test = OvernightFutureContractSpecs.GBP_SONIA_3M_IMM_LCH;
    OvernightFutureTrade trade = test.createTrade(
        date(2020, 1, 25),
        SecurityId.of("OG", "1"),
        SequenceDate.base(YearMonth.of(2020, 2)),
        20,
        0.999d,
        REF_DATA);
    assertThat(trade.getCurrency()).isEqualTo(Currency.GBP);
    assertThat(trade.getPrice()).isEqualTo(.999d);
    assertThat(trade.getQuantity()).isEqualTo(20);
    assertThat(trade.getProduct().getIndex()).isEqualTo(GBP_SONIA);
    assertThat(trade.getProduct().getAccrualMethod()).isEqualTo(COMPOUNDED);
    assertThat(trade.getProduct().getAccrualFactor()).isEqualTo(0.25);
    assertThat(trade.getProduct().getStartDate()).isEqualTo(date(2020, 3, 18));
    assertThat(trade.getProduct().getEndDate()).isEqualTo(date(2020, 6, 16));
    assertThat(trade.getProduct().getLastTradeDate()).isEqualTo(date(2020, 6, 17));
    assertThat(trade.getProduct().getNotional()).isEqualTo(500_000d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createTrade_gbpSonia1mIce() {
    OvernightFutureContractSpec test = OvernightFutureContractSpecs.GBP_SONIA_1M_ICE;
    OvernightFutureTrade trade = test.createTrade(
        date(2020, 1, 25),
        SecurityId.of("OG", "1"),
        SequenceDate.base(YearMonth.of(2020, 2)),
        20,
        0.999d,
        REF_DATA);
    assertThat(trade.getCurrency()).isEqualTo(Currency.GBP);
    assertThat(trade.getPrice()).isEqualTo(.999d);
    assertThat(trade.getQuantity()).isEqualTo(20);
    assertThat(trade.getProduct().getIndex()).isEqualTo(GBP_SONIA);
    assertThat(trade.getProduct().getAccrualMethod()).isEqualTo(AVERAGED_DAILY);
    assertThat(trade.getProduct().getAccrualFactor()).isEqualTo(1 / 12d);
    assertThat(trade.getProduct().getStartDate()).isEqualTo(date(2020, 2, 1));
    assertThat(trade.getProduct().getEndDate()).isEqualTo(date(2020, 2, 29));
    assertThat(trade.getProduct().getLastTradeDate()).isEqualTo(date(2020, 2, 28));
    assertThat(trade.getProduct().getNotional()).isEqualTo(3_000_000d);
  }

  @Test
  public void test_createTrade_gbpSonia1mImmLch() {
    OvernightFutureContractSpec test = OvernightFutureContractSpecs.GBP_SONIA_1M_IMM_LCH;
    OvernightFutureTrade trade = test.createTrade(
        date(2020, 1, 25),
        SecurityId.of("OG", "1"),
        SequenceDate.base(YearMonth.of(2020, 2)),
        20,
        0.999d,
        REF_DATA);
    assertThat(trade.getCurrency()).isEqualTo(Currency.GBP);
    assertThat(trade.getPrice()).isEqualTo(.999d);
    assertThat(trade.getQuantity()).isEqualTo(20);
    assertThat(trade.getProduct().getIndex()).isEqualTo(GBP_SONIA);
    assertThat(trade.getProduct().getAccrualMethod()).isEqualTo(COMPOUNDED);
    assertThat(trade.getProduct().getAccrualFactor()).isEqualTo(1 / 12d);
    assertThat(trade.getProduct().getStartDate()).isEqualTo(date(2020, 2, 19));
    assertThat(trade.getProduct().getEndDate()).isEqualTo(date(2020, 3, 17));
    assertThat(trade.getProduct().getLastTradeDate()).isEqualTo(date(2020, 3, 18));
    assertThat(trade.getProduct().getNotional()).isEqualTo(1_500_000d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createTrade_usdSofa3mImmCme() {
    OvernightFutureContractSpec test = OvernightFutureContractSpecs.USD_SOFR_3M_IMM_CME;
    OvernightFutureTrade trade = test.createTrade(
        date(2020, 1, 25),
        SecurityId.of("OG", "1"),
        SequenceDate.base(YearMonth.of(2020, 2)),
        20,
        0.999d,
        REF_DATA);
    assertThat(trade.getCurrency()).isEqualTo(Currency.USD);
    assertThat(trade.getPrice()).isEqualTo(.999d);
    assertThat(trade.getQuantity()).isEqualTo(20);
    assertThat(trade.getProduct().getIndex()).isEqualTo(USD_SOFR);
    assertThat(trade.getProduct().getAccrualMethod()).isEqualTo(COMPOUNDED);
    assertThat(trade.getProduct().getAccrualFactor()).isEqualTo(0.25);
    assertThat(trade.getProduct().getStartDate()).isEqualTo(date(2020, 3, 18));
    assertThat(trade.getProduct().getEndDate()).isEqualTo(date(2020, 6, 16));
    assertThat(trade.getProduct().getLastTradeDate()).isEqualTo(date(2020, 6, 16));
    assertThat(trade.getProduct().getNotional()).isEqualTo(1_000_000d);
  }

  @Test
  public void test_createTrade_usdSofa3mImmIce() {
    OvernightFutureContractSpec test = OvernightFutureContractSpecs.USD_SOFR_3M_IMM_ICE;
    OvernightFutureTrade trade = test.createTrade(
        date(2020, 1, 25),
        SecurityId.of("OG", "1"),
        SequenceDate.base(YearMonth.of(2020, 2)),
        20,
        0.999d,
        REF_DATA);
    assertThat(trade.getCurrency()).isEqualTo(Currency.USD);
    assertThat(trade.getPrice()).isEqualTo(.999d);
    assertThat(trade.getQuantity()).isEqualTo(20);
    assertThat(trade.getProduct().getIndex()).isEqualTo(USD_SOFR);
    assertThat(trade.getProduct().getAccrualMethod()).isEqualTo(COMPOUNDED);
    assertThat(trade.getProduct().getAccrualFactor()).isEqualTo(0.25);
    assertThat(trade.getProduct().getStartDate()).isEqualTo(date(2020, 3, 18));
    assertThat(trade.getProduct().getEndDate()).isEqualTo(date(2020, 6, 16));
    assertThat(trade.getProduct().getLastTradeDate()).isEqualTo(date(2020, 6, 16));
    assertThat(trade.getProduct().getNotional()).isEqualTo(4_000_000d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createTrade_usdSofa1mCme() {
    OvernightFutureContractSpec test = OvernightFutureContractSpecs.USD_SOFR_1M_CME;
    OvernightFutureTrade trade = test.createTrade(
        date(2020, 1, 25),
        SecurityId.of("OG", "1"),
        SequenceDate.base(YearMonth.of(2020, 2)),
        20,
        0.999d,
        REF_DATA);
    assertThat(trade.getCurrency()).isEqualTo(Currency.USD);
    assertThat(trade.getPrice()).isEqualTo(.999d);
    assertThat(trade.getQuantity()).isEqualTo(20);
    assertThat(trade.getProduct().getIndex()).isEqualTo(USD_SOFR);
    assertThat(trade.getProduct().getAccrualMethod()).isEqualTo(AVERAGED_DAILY);
    assertThat(trade.getProduct().getAccrualFactor()).isEqualTo(1 / 12d);
    assertThat(trade.getProduct().getStartDate()).isEqualTo(date(2020, 2, 1));
    assertThat(trade.getProduct().getEndDate()).isEqualTo(date(2020, 2, 29));
    assertThat(trade.getProduct().getLastTradeDate()).isEqualTo(date(2020, 2, 28));
    assertThat(trade.getProduct().getNotional()).isEqualTo(5_000_000d);
  }

  @Test
  public void test_createTrade_usdSofa1mIce() {
    OvernightFutureContractSpec test = OvernightFutureContractSpecs.USD_SOFR_1M_ICE;
    OvernightFutureTrade trade = test.createTrade(
        date(2020, 1, 25),
        SecurityId.of("OG", "1"),
        SequenceDate.base(YearMonth.of(2020, 2)),
        20,
        0.999d,
        REF_DATA);
    assertThat(trade.getCurrency()).isEqualTo(Currency.USD);
    assertThat(trade.getPrice()).isEqualTo(.999d);
    assertThat(trade.getQuantity()).isEqualTo(20);
    assertThat(trade.getProduct().getIndex()).isEqualTo(USD_SOFR);
    assertThat(trade.getProduct().getAccrualMethod()).isEqualTo(AVERAGED_DAILY);
    assertThat(trade.getProduct().getAccrualFactor()).isEqualTo(1 / 12d);
    assertThat(trade.getProduct().getStartDate()).isEqualTo(date(2020, 2, 1));
    assertThat(trade.getProduct().getEndDate()).isEqualTo(date(2020, 2, 29));
    assertThat(trade.getProduct().getLastTradeDate()).isEqualTo(date(2020, 2, 28));
    assertThat(trade.getProduct().getNotional()).isEqualTo(12_000_000d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createTrade_usdFedFund1mCme() {
    OvernightFutureContractSpec test = OvernightFutureContractSpecs.USD_FED_FUND_1M_CME;
    OvernightFutureTrade trade = test.createTrade(
        date(2020, 1, 25),
        SecurityId.of("OG", "1"),
        SequenceDate.base(YearMonth.of(2020, 2)),
        20,
        0.999d,
        REF_DATA);
    assertThat(trade.getCurrency()).isEqualTo(Currency.USD);
    assertThat(trade.getPrice()).isEqualTo(.999d);
    assertThat(trade.getQuantity()).isEqualTo(20);
    assertThat(trade.getProduct().getIndex()).isEqualTo(USD_FED_FUND);
    assertThat(trade.getProduct().getAccrualMethod()).isEqualTo(AVERAGED_DAILY);
    assertThat(trade.getProduct().getAccrualFactor()).isEqualTo(1 / 12d);
    assertThat(trade.getProduct().getStartDate()).isEqualTo(date(2020, 2, 1));
    assertThat(trade.getProduct().getEndDate()).isEqualTo(date(2020, 2, 29));
    assertThat(trade.getProduct().getLastTradeDate()).isEqualTo(date(2020, 2, 28));
    assertThat(trade.getProduct().getNotional()).isEqualTo(5_000_000d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createPosition_usdFedFund1mCme() {
    OvernightFutureContractSpec test = OvernightFutureContractSpecs.USD_FED_FUND_1M_CME;
    OvernightFuturePosition trade = test.createPosition(SecurityId.of("OG", "1"), YearMonth.of(2020, 2), 20, REF_DATA);
    assertThat(trade.getCurrency()).isEqualTo(Currency.USD);
    assertThat(trade.getQuantity()).isEqualTo(20);
    assertThat(trade.getProduct().getIndex()).isEqualTo(USD_FED_FUND);
    assertThat(trade.getProduct().getAccrualMethod()).isEqualTo(AVERAGED_DAILY);
    assertThat(trade.getProduct().getAccrualFactor()).isEqualTo(1 / 12d);
    assertThat(trade.getProduct().getStartDate()).isEqualTo(date(2020, 2, 1));
    assertThat(trade.getProduct().getEndDate()).isEqualTo(date(2020, 2, 29));
    assertThat(trade.getProduct().getLastTradeDate()).isEqualTo(date(2020, 2, 28));
    assertThat(trade.getProduct().getNotional()).isEqualTo(5_000_000d);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {OvernightFutureContractSpecs.GBP_SONIA_3M_IMM_CME, "GBP-SONIA-3M-IMM-CME"},
        {OvernightFutureContractSpecs.GBP_SONIA_3M_IMM_ICE, "GBP-SONIA-3M-IMM-ICE"},
        {OvernightFutureContractSpecs.GBP_SONIA_3M_IMM_LCH, "GBP-SONIA-3M-IMM-LCH"},
        {OvernightFutureContractSpecs.GBP_SONIA_1M_ICE, "GBP-SONIA-1M-ICE"},
        {OvernightFutureContractSpecs.GBP_SONIA_1M_IMM_LCH, "GBP-SONIA-1M-IMM-LCH"},
        {OvernightFutureContractSpecs.USD_SOFR_3M_IMM_CME, "USD-SOFR-3M-IMM-CME"},
        {OvernightFutureContractSpecs.USD_SOFR_3M_IMM_ICE, "USD-SOFR-3M-IMM-ICE"},
        {OvernightFutureContractSpecs.USD_SOFR_1M_CME, "USD-SOFR-1M-CME"},
        {OvernightFutureContractSpecs.USD_SOFR_1M_ICE, "USD-SOFR-1M-ICE"},
        {OvernightFutureContractSpecs.USD_FED_FUND_1M_CME, "USD-FED-FUND-1M-CME"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_name(OvernightFutureContractSpec convention, String name) {
    assertThat(convention.getName()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(OvernightFutureContractSpec convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(OvernightFutureContractSpec convention, String name) {
    assertThat(OvernightFutureContractSpec.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_extendedEnum(OvernightFutureContractSpec convention, String name) {
    OvernightFutureContractSpec.of(name);  // ensures map is populated
    ImmutableMap<String, OvernightFutureContractSpec> map = OvernightFutureContractSpec.extendedEnum().lookupAll();
    assertThat(map.get(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightFutureContractSpec.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightFutureContractSpec.of((String) null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ImmutableOvernightFutureContractSpec test = ImmutableOvernightFutureContractSpec.builder()
        .name("GBP-SONIA-3M")
        .index(GBP_SONIA)
        .accrualMethod(COMPOUNDED)
        .dateSequence(QUARTERLY_IMM)
        .startDateAdjustment(BDA_PRECEDE)
        .notional(1_000_000d)
        .build();
    coverImmutableBean(test);
    ImmutableOvernightFutureContractSpec test2 = ImmutableOvernightFutureContractSpec.builder()
        .name("USD-SOFR-3M")
        .index(USD_SOFR)
        .accrualMethod(AVERAGED_DAILY)
        .dateSequence(MONTHLY_IMM)
        .startDateAdjustment(BDA_FOLLOW)
        .endDateAdjustment(DaysAdjustment.ofCalendarDays(-2, BDA_FOLLOW))
        .lastTradeDateAdjustment(DaysAdjustment.ofCalendarDays(-2, BDA_FOLLOW))
        .notional(2_000_000d)
        .build();
    coverBeanEquals(test, test2);

    coverPrivateConstructor(OvernightFutureContractSpecs.class);
    coverPrivateConstructor(StandardOvernightFutureContractSpecs.class);
  }

  @Test
  public void test_serialization() {
    OvernightFutureContractSpec test = ImmutableOvernightFutureContractSpec.builder()
        .name("GBP-SONIA-3M")
        .index(GBP_SONIA)
        .accrualMethod(COMPOUNDED)
        .dateSequence(QUARTERLY_IMM)
        .startDateAdjustment(BDA_PRECEDE)
        .notional(1_000_000d)
        .build();
    assertSerialization(test);
  }

}
