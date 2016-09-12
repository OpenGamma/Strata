/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index.type;

import static com.opengamma.strata.basics.date.DateSequences.MONTHLY_IMM;
import static com.opengamma.strata.basics.date.DateSequences.QUARTERLY_IMM;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.index.IborFutureTrade;

/**
 * Tests {@link IborFutureConvention}.
 */
@Test
public class IborFutureConventionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL_1M = 1_000_000d;
  private static final BusinessDayAdjustment BDA = BusinessDayAdjustment
      .of(BusinessDayConventions.FOLLOWING, USD_LIBOR_3M.getEffectiveDateOffset().getCalendar());

  //-------------------------------------------------------------------------
  public void test_of() {
    ImmutableIborFutureConvention test = ImmutableIborFutureConvention.of(USD_LIBOR_3M, QUARTERLY_IMM);
    assertEquals(test.getName(), "USD-LIBOR-3M-Quarterly-IMM");
    assertEquals(test.getIndex(), USD_LIBOR_3M);
    assertEquals(test.getDateSequence(), QUARTERLY_IMM);
    assertEquals(test.getBusinessDayAdjustment(), BDA);
  }

  public void test_builder() {
    ImmutableIborFutureConvention test = ImmutableIborFutureConvention.builder()
        .name("USD-IMM")
        .index(USD_LIBOR_3M)
        .dateSequence(QUARTERLY_IMM)
        .build();
    assertEquals(test.getName(), "USD-IMM");
    assertEquals(test.getIndex(), USD_LIBOR_3M);
    assertEquals(test.getDateSequence(), QUARTERLY_IMM);
    assertEquals(test.getBusinessDayAdjustment(), BDA);
  }

  public void test_builder_incomplete() {
    assertThrowsIllegalArg(() -> ImmutableIborFutureConvention.builder()
        .index(USD_LIBOR_3M)
        .build());
    assertThrowsIllegalArg(() -> ImmutableIborFutureConvention.builder()
        .dateSequence(QUARTERLY_IMM)
        .build());
  }

  public void test_toTrade() {
    LocalDate date = LocalDate.of(2015, 10, 20);
    Period start = Period.ofMonths(2);
    int number = 2; // Future should be 20 Dec 15 + 2 IMM = effective 15-Jun-2016, fixing 13-Jun-2016    
    IborFutureConvention convention = ImmutableIborFutureConvention.of(USD_LIBOR_3M, QUARTERLY_IMM);
    double quantity = 3;
    double price = 0.99;
    SecurityId secId = SecurityId.of("OG-Future", "GBP-LIBOR-3M-Jun16");
    IborFutureTrade trade = convention.createTrade(date, secId, start, number, quantity, NOTIONAL_1M, price, REF_DATA);
    assertEquals(trade.getProduct().getFixingDate(), LocalDate.of(2016, 6, 13));
    assertEquals(trade.getProduct().getIndex(), USD_LIBOR_3M);
    assertEquals(trade.getProduct().getNotional(), NOTIONAL_1M);
    assertEquals(trade.getProduct().getAccrualFactor(), 0.25);
    assertEquals(trade.getQuantity(), quantity);
    assertEquals(trade.getPrice(), price);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {IborFutureConventions.USD_LIBOR_3M_QUARTERLY_IMM, "USD-LIBOR-3M-Quarterly-IMM"},
        {IborFutureConventions.USD_LIBOR_3M_MONTHLY_IMM, "USD-LIBOR-3M-Monthly-IMM"},
    };
  }

  @Test(dataProvider = "name")
  public void test_name(IborFutureConvention convention, String name) {
    assertEquals(convention.getName(), name);
  }

  @Test(dataProvider = "name")
  public void test_toString(IborFutureConvention convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(IborFutureConvention convention, String name) {
    assertEquals(IborFutureConvention.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_extendedEnum(IborFutureConvention convention, String name) {
    IborFutureConvention.of(name);  // ensures map is populated
    ImmutableMap<String, IborFutureConvention> map = IborFutureConvention.extendedEnum().lookupAll();
    assertEquals(map.get(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> IborFutureConvention.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> IborFutureConvention.of((String) null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableIborFutureConvention test = ImmutableIborFutureConvention.of(USD_LIBOR_3M, QUARTERLY_IMM);
    coverImmutableBean(test);
    ImmutableIborFutureConvention test2 = ImmutableIborFutureConvention.builder()
        .index(USD_LIBOR_3M)
        .dateSequence(MONTHLY_IMM)
        .businessDayAdjustment(BDA)
        .build();
    coverBeanEquals(test, test2);

    coverPrivateConstructor(IborFutureConventions.class);
    coverPrivateConstructor(StandardIborFutureConventions.class);
  }

  public void test_serialization() {
    IborFutureConvention test = ImmutableIborFutureConvention.of(USD_LIBOR_3M, QUARTERLY_IMM);
    assertSerialization(test);
  }

}
