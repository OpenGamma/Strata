/*
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
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.fra.FraDiscountingMethod.ISDA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test {@link ResolvedFra}.
 */
public class ResolvedFraTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL_1M = 1_000_000d;
  private static final double NOTIONAL_2M = 2_000_000d;

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    ResolvedFra test = sut();
    assertThat(test.getPaymentDate()).isEqualTo(date(2015, 6, 16));
    assertThat(test.getStartDate()).isEqualTo(date(2015, 6, 15));
    assertThat(test.getEndDate()).isEqualTo(date(2015, 9, 15));
    assertThat(test.getYearFraction()).isCloseTo(0.25d, offset(0d));
    assertThat(test.getFixedRate()).isCloseTo(0.25d, offset(0d));
    assertThat(test.getFloatingRate()).isEqualTo(IborRateComputation.of(GBP_LIBOR_3M, date(2015, 6, 12), REF_DATA));
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getNotional()).isCloseTo(NOTIONAL_1M, offset(0d));
    assertThat(test.getDiscounting()).isEqualTo(ISDA);
    assertThat(test.allIndices()).containsOnly(GBP_LIBOR_3M);
  }

  @Test
  public void test_builder_datesInOrder() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedFra.builder()
            .notional(NOTIONAL_1M)
            .paymentDate(date(2015, 6, 15))
            .startDate(date(2015, 6, 15))
            .endDate(date(2015, 6, 14))
            .fixedRate(0.25d)
            .floatingRate(IborRateComputation.of(GBP_LIBOR_3M, date(2015, 6, 12), REF_DATA))
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  @Test
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
