/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit.cds;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.StandardId;

/**
 * Test {@link ConstantRecoveryRates}.
 */
@Test
public class ConstantRecoveryRatesTest {

  private static final LocalDate VALUATION = LocalDate.of(2016, 5, 6);
  private static final double RECOVERY_RATE = 0.35;
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG", "ABC");
  private static final LocalDate DATE_AFTER = LocalDate.of(2017, 2, 24);

  public void test_of() {
    ConstantRecoveryRates test = ConstantRecoveryRates.of(LEGAL_ENTITY, VALUATION, RECOVERY_RATE);
    assertEquals(test.getLegalEntityId(), LEGAL_ENTITY);
    assertEquals(test.getRecoveryRate(), RECOVERY_RATE);
    assertEquals(test.getValuationDate(), VALUATION);
    assertEquals(test.recoveryRate(DATE_AFTER), RECOVERY_RATE);
  }

  public void test_of_rateOutOfRange() {
    assertThrowsIllegalArg(() -> ConstantRecoveryRates.of(LEGAL_ENTITY, VALUATION, -0.5));
    assertThrowsIllegalArg(() -> ConstantRecoveryRates.of(LEGAL_ENTITY, VALUATION, 1.5));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ConstantRecoveryRates test1 = ConstantRecoveryRates.of(LEGAL_ENTITY, VALUATION, RECOVERY_RATE);
    coverImmutableBean(test1);
    ConstantRecoveryRates test2 = ConstantRecoveryRates.of(StandardId.of("OG", "DEF"), DATE_AFTER, 0.2d);
    coverBeanEquals(test1, test2);
  }
}
