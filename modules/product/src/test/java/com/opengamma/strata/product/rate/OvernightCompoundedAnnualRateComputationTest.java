/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

import static com.opengamma.strata.basics.index.OvernightIndices.BRL_CDI;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndexObservation;

/**
 * Test.
 */
@Test
public class OvernightCompoundedAnnualRateComputationTest {
  
  private static final ReferenceData REF_DATA = ReferenceData.standard();
  
  public void test_of() {
    OvernightCompoundedAnnualRateComputation test = OvernightCompoundedAnnualRateComputation.of(
        BRL_CDI, date(2016, 2, 24), date(2016, 3, 24), REF_DATA);
    assertEquals(test.getStartDate(), date(2016, 2, 24));
    assertEquals(test.getEndDate(), date(2016, 3, 24));
    assertEquals(test.getIndex(), BRL_CDI);
    assertEquals(test.getFixingCalendar(), BRL_CDI.getFixingCalendar().resolve(REF_DATA));
  }
  
  public void test_of_badDateOrder() {
    assertThrowsIllegalArg(() -> OvernightCompoundedAnnualRateComputation.of(
        BRL_CDI, date(2016, 2, 24), date(2016, 2, 24), REF_DATA));
    assertThrowsIllegalArg(() -> OvernightCompoundedAnnualRateComputation.of(
        BRL_CDI, date(2016, 2, 25), date(2016, 2, 24), REF_DATA));
  }
  
  public void test_calculate() {
    OvernightCompoundedAnnualRateComputation test =
        OvernightCompoundedAnnualRateComputation.of(BRL_CDI, date(2016, 2, 24), date(2016, 3, 24), REF_DATA);
    assertEquals(
        test.calculateEffectiveFromFixing(date(2016, 2, 24)),
        BRL_CDI.calculateEffectiveFromFixing(date(2016, 2, 24), REF_DATA));
    assertEquals(
        test.calculateFixingFromEffective(date(2016, 2, 24)),
        BRL_CDI.calculateFixingFromEffective(date(2016, 2, 24), REF_DATA));
    assertEquals(
        test.calculatePublicationFromFixing(date(2016, 2, 24)),
        BRL_CDI.calculatePublicationFromFixing(date(2016, 2, 24), REF_DATA));
    assertEquals(
        test.calculateMaturityFromFixing(date(2016, 2, 24)),
        BRL_CDI.calculateMaturityFromFixing(date(2016, 2, 24), REF_DATA));
    assertEquals(
        test.calculateMaturityFromEffective(date(2016, 2, 24)),
        BRL_CDI.calculateMaturityFromEffective(date(2016, 2, 24), REF_DATA));
  }
  
  public void test_observeOn() {
    OvernightCompoundedAnnualRateComputation test =
        OvernightCompoundedAnnualRateComputation.of(USD_FED_FUND, date(2016, 2, 24), date(2016, 3, 24), REF_DATA);
    assertEquals(test.observeOn(date(2016, 2, 24)), OvernightIndexObservation.of(USD_FED_FUND, date(2016, 2, 24), REF_DATA));
  }
  
  public void test_collectIndices() {
    OvernightCompoundedAnnualRateComputation test =
        OvernightCompoundedAnnualRateComputation.of(BRL_CDI, date(2016, 2, 24), date(2016, 3, 24), REF_DATA);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(BRL_CDI));
  }
  
  public void coverage() {
    OvernightCompoundedAnnualRateComputation test =
        OvernightCompoundedAnnualRateComputation.of(USD_FED_FUND, date(2016, 2, 24), date(2016, 3, 24), REF_DATA);
    coverImmutableBean(test);
    OvernightCompoundedAnnualRateComputation test2 =
        OvernightCompoundedAnnualRateComputation.of(BRL_CDI, date(2014, 6, 3), date(2014, 7, 3), REF_DATA);
    coverBeanEquals(test, test2);
  }
  
  public void test_serialization() {
    OvernightCompoundedAnnualRateComputation test =
        OvernightCompoundedAnnualRateComputation.of(BRL_CDI, date(2016, 2, 24), date(2016, 3, 24), REF_DATA);
    assertSerialization(test);
  }
}
