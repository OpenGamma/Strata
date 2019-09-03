/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.FxIndices.EUR_GBP_ECB;
import static com.opengamma.strata.basics.index.FxIndices.EUR_USD_ECB;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.index.FxIndexObservation;

/**
 * Test.
 */
public class FxResetTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate DATE_2014_06_30 = date(2014, 6, 30);

  @Test
  public void test_of() {
    FxReset test = FxReset.of(FxIndexObservation.of(EUR_GBP_ECB, DATE_2014_06_30, REF_DATA), GBP);
    assertThat(test.getIndex()).isEqualTo(EUR_GBP_ECB);
    assertThat(test.getReferenceCurrency()).isEqualTo(GBP);
  }

  @Test
  public void test_invalidCurrency() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxReset.meta().builder()
            .set(FxReset.meta().observation(), FxIndexObservation.of(EUR_USD_ECB, DATE_2014_06_30, REF_DATA))
            .set(FxReset.meta().referenceCurrency(), GBP)
            .build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxReset.of(FxIndexObservation.of(EUR_USD_ECB, DATE_2014_06_30, REF_DATA), GBP));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FxReset test = FxReset.of(FxIndexObservation.of(EUR_GBP_ECB, DATE_2014_06_30, REF_DATA), GBP);
    coverImmutableBean(test);
    FxReset test2 = FxReset.of(FxIndexObservation.of(EUR_USD_ECB, date(2014, 1, 15), REF_DATA), USD);
    coverBeanEquals(test, test2);
    FxReset test3 = FxReset.of(FxIndexObservation.of(EUR_USD_ECB, date(2014, 1, 15), REF_DATA), EUR);
    coverBeanEquals(test2, test3);
  }

  @Test
  public void test_serialization() {
    FxReset test = FxReset.of(FxIndexObservation.of(EUR_GBP_ECB, DATE_2014_06_30, REF_DATA), GBP);
    assertSerialization(test);
  }

}
