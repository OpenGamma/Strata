/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.basics.index.OvernightIndices.EUR_EONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link OvernightIndexObservation}.
 */
@Test
public class OvernightIndexObservationTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate FIXING_DATE = date(2016, 2, 22);
  private static final LocalDate PUBLICATION_DATE = GBP_SONIA.calculatePublicationFromFixing(FIXING_DATE, REF_DATA);
  private static final LocalDate EFFECTIVE_DATE = GBP_SONIA.calculateEffectiveFromFixing(FIXING_DATE, REF_DATA);
  private static final LocalDate MATURITY_DATE = GBP_SONIA.calculateMaturityFromEffective(EFFECTIVE_DATE, REF_DATA);

  //-------------------------------------------------------------------------
  public void test_of() {
    OvernightIndexObservation test = OvernightIndexObservation.of(GBP_SONIA, FIXING_DATE, REF_DATA);
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getFixingDate(), FIXING_DATE);
    assertEquals(test.getPublicationDate(), PUBLICATION_DATE);
    assertEquals(test.getEffectiveDate(), EFFECTIVE_DATE);
    assertEquals(test.getMaturityDate(), MATURITY_DATE);
    assertEquals(test.getCurrency(), GBP_SONIA.getCurrency());
    assertEquals(test.toString(), "OvernightIndexObservation[GBP-SONIA on 2016-02-22]");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    OvernightIndexObservation test = OvernightIndexObservation.of(GBP_SONIA, FIXING_DATE, REF_DATA);
    coverImmutableBean(test);
    OvernightIndexObservation test2 = OvernightIndexObservation.of(EUR_EONIA, FIXING_DATE.plusDays(1), REF_DATA);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    OvernightIndexObservation test = OvernightIndexObservation.of(GBP_SONIA, FIXING_DATE, REF_DATA);
    assertSerialization(test);
  }

}
