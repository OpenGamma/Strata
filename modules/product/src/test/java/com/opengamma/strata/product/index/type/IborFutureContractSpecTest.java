/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index.type;

import static com.opengamma.strata.basics.date.DateSequences.MONTHLY_IMM;
import static com.opengamma.strata.basics.date.DateSequences.QUARTERLY_IMM;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.SequenceDate;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.index.IborFuturePosition;
import com.opengamma.strata.product.index.IborFutureTrade;

/**
 * Tests {@link IborFutureContractSpec}.
 */
public class IborFutureContractSpecTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL_1M = 1_000_000d;
  private static final BusinessDayAdjustment BDA = BusinessDayAdjustment
      .of(BusinessDayConventions.FOLLOWING, USD_LIBOR_3M.getEffectiveDateOffset().getCalendar());

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    ImmutableIborFutureContractSpec test = ImmutableIborFutureContractSpec.builder()
        .name("USD-IMM")
        .index(USD_LIBOR_3M)
        .dateSequence(QUARTERLY_IMM)
        .notional(1_000_000d)
        .build();
    assertThat(test.getName()).isEqualTo("USD-IMM");
    assertThat(test.getIndex()).isEqualTo(USD_LIBOR_3M);
    assertThat(test.getDateSequence()).isEqualTo(QUARTERLY_IMM);
    assertThat(test.getBusinessDayAdjustment()).isEqualTo(BDA);
  }

  @Test
  public void test_builder_incomplete() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ImmutableIborFutureContractSpec.builder()
            .index(USD_LIBOR_3M)
            .build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ImmutableIborFutureContractSpec.builder()
            .dateSequence(QUARTERLY_IMM)
            .build());
  }

  @Test
  public void test_toTrade() {
    LocalDate date = LocalDate.of(2015, 10, 20);
    // Future should be 20 Dec 15 + 2 IMM = effective 15-Jun-2016, fixing 13-Jun-2016    
    SequenceDate seqDate = SequenceDate.base(Period.ofMonths(2), 2);
    IborFutureContractSpec convention = IborFutureContractSpecs.USD_LIBOR_3M_IMM_CME;
    double quantity = 3;
    double price = 0.99;
    SecurityId secId = SecurityId.of("OG-Future", "GBP-LIBOR-3M-Jun16");
    IborFutureTrade trade = convention.createTrade(date, secId, seqDate, quantity, price, REF_DATA);
    assertThat(trade.getProduct().getFixingDate()).isEqualTo(LocalDate.of(2016, 6, 13));
    assertThat(trade.getProduct().getIndex()).isEqualTo(USD_LIBOR_3M);
    assertThat(trade.getProduct().getNotional()).isEqualTo(NOTIONAL_1M);
    assertThat(trade.getProduct().getAccrualFactor()).isEqualTo(0.25);
    assertThat(trade.getQuantity()).isEqualTo(quantity);
    assertThat(trade.getPrice()).isEqualTo(price);
  }

  @Test
  public void test_toPosition() {
    YearMonth expiry = YearMonth.of(2016, 6);
    IborFutureContractSpec convention = IborFutureContractSpecs.USD_LIBOR_3M_IMM_CME;
    double quantity = 3;
    SecurityId secId = SecurityId.of("OG-Future", "GBP-LIBOR-3M-Jun16");
    IborFuturePosition trade = convention.createPosition(secId, expiry, quantity, REF_DATA);
    assertThat(trade.getProduct().getFixingDate()).isEqualTo(LocalDate.of(2016, 6, 13));
    assertThat(trade.getProduct().getIndex()).isEqualTo(USD_LIBOR_3M);
    assertThat(trade.getProduct().getNotional()).isEqualTo(NOTIONAL_1M);
    assertThat(trade.getProduct().getAccrualFactor()).isEqualTo(0.25);
    assertThat(trade.getQuantity()).isEqualTo(quantity);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {IborFutureContractSpecs.USD_LIBOR_3M_IMM_CME, "USD-LIBOR-3M-IMM-CME"},
        {IborFutureContractSpecs.GBP_LIBOR_3M_IMM_ICE, "GBP-LIBOR-3M-IMM-ICE"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_name(IborFutureContractSpec convention, String name) {
    assertThat(convention.getName()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(IborFutureContractSpec convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(IborFutureContractSpec convention, String name) {
    assertThat(IborFutureContractSpec.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_extendedEnum(IborFutureContractSpec convention, String name) {
    IborFutureContractSpec.of(name);  // ensures map is populated
    ImmutableMap<String, IborFutureContractSpec> map = IborFutureContractSpec.extendedEnum().lookupAll();
    assertThat(map.get(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborFutureContractSpec.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborFutureContractSpec.of((String) null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ImmutableIborFutureContractSpec test = (ImmutableIborFutureContractSpec) IborFutureContractSpecs.USD_LIBOR_3M_IMM_CME;
    coverImmutableBean(test);
    ImmutableIborFutureContractSpec test2 = ImmutableIborFutureContractSpec.builder()
        .name("GBP-TEST")
        .index(GBP_LIBOR_3M)
        .dateSequence(MONTHLY_IMM)
        .businessDayAdjustment(BDA)
        .notional(1000d)
        .build();
    coverBeanEquals(test, test2);

    coverPrivateConstructor(IborFutureContractSpecs.class);
    coverPrivateConstructor(StandardIborFutureContractSpecs.class);
  }

  @Test
  public void test_serialization() {
    IborFutureContractSpec test = IborFutureContractSpecs.USD_LIBOR_3M_IMM_CME;
    assertSerialization(test);
  }

}
