/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

import static com.opengamma.strata.basics.index.OvernightIndices.BRL_CDI;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndexObservation;

/**
 * Test {@link OvernightCompoundedAnnualRateComputation}.
 */
public class OvernightCompoundedAnnualRateComputationTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    OvernightCompoundedAnnualRateComputation test = sut();
    assertThat(test.getStartDate()).isEqualTo(date(2016, 2, 24));
    assertThat(test.getEndDate()).isEqualTo(date(2016, 3, 24));
    assertThat(test.getIndex()).isEqualTo(BRL_CDI);
    assertThat(test.getFixingCalendar()).isEqualTo(BRL_CDI.getFixingCalendar().resolve(REF_DATA));
  }

  @Test
  public void test_of_badDateOrder() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightCompoundedAnnualRateComputation.of(
            BRL_CDI, date(2016, 2, 24), date(2016, 2, 24), REF_DATA));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightCompoundedAnnualRateComputation.of(
            BRL_CDI, date(2016, 2, 25), date(2016, 2, 24), REF_DATA));
  }

  @Test
  public void test_calculate() {
    OvernightCompoundedAnnualRateComputation test = sut();
    assertThat(test.calculateEffectiveFromFixing(date(2016, 2, 24)))
        .isEqualTo(BRL_CDI.calculateEffectiveFromFixing(date(2016, 2, 24), REF_DATA));
    assertThat(test.calculateFixingFromEffective(date(2016, 2, 24)))
        .isEqualTo(BRL_CDI.calculateFixingFromEffective(date(2016, 2, 24), REF_DATA));
    assertThat(test.calculatePublicationFromFixing(date(2016, 2, 24)))
        .isEqualTo(BRL_CDI.calculatePublicationFromFixing(date(2016, 2, 24), REF_DATA));
    assertThat(test.calculateMaturityFromFixing(date(2016, 2, 24)))
        .isEqualTo(BRL_CDI.calculateMaturityFromFixing(date(2016, 2, 24), REF_DATA));
    assertThat(test.calculateMaturityFromEffective(date(2016, 2, 24)))
        .isEqualTo(BRL_CDI.calculateMaturityFromEffective(date(2016, 2, 24), REF_DATA));
  }

  @Test
  public void test_observeOn() {
    OvernightCompoundedAnnualRateComputation test = sut();
    assertThat(test.observeOn(date(2016, 2, 24))).isEqualTo(OvernightIndexObservation.of(BRL_CDI, date(2016, 2, 24), REF_DATA));
  }

  @Test
  public void test_collectIndices() {
    OvernightCompoundedAnnualRateComputation test = sut();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertThat(builder.build()).containsOnly(BRL_CDI);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    OvernightCompoundedAnnualRateComputation test = sut();
    coverImmutableBean(test);
    OvernightCompoundedAnnualRateComputation test2 = sut2();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    OvernightCompoundedAnnualRateComputation test = sut();
    assertSerialization(test);
  }

  //-------------------------------------------------------------------------
  private OvernightCompoundedAnnualRateComputation sut() {
    return OvernightCompoundedAnnualRateComputation.of(BRL_CDI, date(2016, 2, 24), date(2016, 3, 24), REF_DATA);
  }

  private OvernightCompoundedAnnualRateComputation sut2() {
    return OvernightCompoundedAnnualRateComputation.of(USD_FED_FUND, date(2014, 6, 3), date(2014, 7, 3), REF_DATA);
  }

}
