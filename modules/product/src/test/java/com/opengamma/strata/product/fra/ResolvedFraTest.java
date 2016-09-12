/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fra;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_2M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.fra.FraDiscountingMethod.ISDA;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test {@link ResolvedFra}.
 */
@Test
public class ResolvedFraTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL_1M = 1_000_000d;
  private static final double NOTIONAL_2M = 2_000_000d;

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedFra test = sut();
    assertEquals(test.getPaymentDate(), date(2015, 6, 16));
    assertEquals(test.getStartDate(), date(2015, 6, 15));
    assertEquals(test.getEndDate(), date(2015, 9, 15));
    assertEquals(test.getYearFraction(), 0.25d, 0d);
    assertEquals(test.getFixedRate(), 0.25d, 0d);
    assertEquals(test.getFloatingRate(), IborRateComputation.of(GBP_LIBOR_3M, date(2015, 6, 12), REF_DATA));
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getNotional(), NOTIONAL_1M, 0d);
    assertEquals(test.getDiscounting(), ISDA);
    assertEquals(test.allIndices(), ImmutableSet.of(GBP_LIBOR_3M));
  }

  public void test_builder_datesInOrder() {
    assertThrowsIllegalArg(() -> ResolvedFra.builder()
        .notional(NOTIONAL_1M)
        .paymentDate(date(2015, 6, 15))
        .startDate(date(2015, 6, 15))
        .endDate(date(2015, 6, 14))
        .fixedRate(0.25d)
        .floatingRate(IborRateComputation.of(GBP_LIBOR_3M, date(2015, 6, 12), REF_DATA))
        .build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    ResolvedFra test = sut();
    assertSerialization(test);
  }

  //-------------------------------------------------------------------------
  static ResolvedFra sut() {
    return ResolvedFra.builder()
        .paymentDate(date(2015, 6, 16))
        .startDate(date(2015, 6, 15))
        .endDate(date(2015, 9, 15))
        .yearFraction(0.25d)
        .fixedRate(0.25d)
        .floatingRate(IborRateComputation.of(GBP_LIBOR_3M, date(2015, 6, 12), REF_DATA))
        .currency(GBP)
        .notional(NOTIONAL_1M)
        .discounting(ISDA)
        .build();
  }

  static ResolvedFra sut2() {
    return ResolvedFra.builder()
        .paymentDate(date(2015, 6, 17))
        .startDate(date(2015, 6, 16))
        .endDate(date(2015, 9, 16))
        .yearFraction(0.26d)
        .fixedRate(0.27d)
        .floatingRate(IborRateComputation.of(GBP_LIBOR_2M, date(2015, 6, 12), REF_DATA))
        .currency(USD)
        .notional(NOTIONAL_2M)
        .discounting(FraDiscountingMethod.NONE)
        .build();
  }

}
