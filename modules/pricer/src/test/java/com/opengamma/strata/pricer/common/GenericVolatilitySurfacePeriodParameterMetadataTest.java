/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.common;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Period;

import org.joda.beans.BeanBuilder;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.option.LogMoneynessStrike;
import com.opengamma.strata.market.option.MoneynessStrike;
import com.opengamma.strata.market.option.SimpleStrike;
import com.opengamma.strata.market.option.Strike;

/**
 * Test {@link GenericVolatilitySurfacePeriodParameterMetadata}.
 */
public class GenericVolatilitySurfacePeriodParameterMetadataTest {

  private static final Period TIME_TO_EXPIRY = Period.ofYears(2);
  private static final LogMoneynessStrike STRIKE1 = LogMoneynessStrike.of(0.98d);
  private static final SimpleStrike STRIKE2 = SimpleStrike.of(1.05);

  @Test
  public void test_of_withStrikeType() {
    GenericVolatilitySurfacePeriodParameterMetadata test =
        GenericVolatilitySurfacePeriodParameterMetadata.of(TIME_TO_EXPIRY, STRIKE1);
    assertThat(test.getIdentifier()).isEqualTo(Pair.of(TIME_TO_EXPIRY, STRIKE1));
    assertThat(test.getLabel()).isEqualTo(Pair.of(TIME_TO_EXPIRY, STRIKE1.getLabel()).toString());
    assertThat(test.getStrike()).isEqualTo(STRIKE1);
    assertThat(test.getPeriod()).isEqualTo(TIME_TO_EXPIRY);
  }

  @Test
  public void test_of_withLabel() {
    Pair<Period, Strike> pair = Pair.of(TIME_TO_EXPIRY, STRIKE2);
    String label = "(2, 1.35)";
    GenericVolatilitySurfacePeriodParameterMetadata test =
        GenericVolatilitySurfacePeriodParameterMetadata.of(TIME_TO_EXPIRY, STRIKE2, label);
    assertThat(test.getIdentifier()).isEqualTo(pair);
    assertThat(test.getLabel()).isEqualTo(label);
    assertThat(test.getStrike()).isEqualTo(STRIKE2);
    assertThat(test.getPeriod()).isEqualTo(TIME_TO_EXPIRY);
  }

  @Test
  public void test_builder_noLabel() {
    BeanBuilder<? extends GenericVolatilitySurfacePeriodParameterMetadata> builder =
        GenericVolatilitySurfacePeriodParameterMetadata.meta().builder();
    Pair<Period, Strike> pair = Pair.of(TIME_TO_EXPIRY, STRIKE1);
    builder.set(GenericVolatilitySurfacePeriodParameterMetadata.meta().period(), TIME_TO_EXPIRY);
    builder.set(GenericVolatilitySurfacePeriodParameterMetadata.meta().strike(), STRIKE1);
    GenericVolatilitySurfacePeriodParameterMetadata test = builder.build();
    assertThat(test.getIdentifier()).isEqualTo(pair);
    assertThat(test.getLabel()).isEqualTo(Pair.of(TIME_TO_EXPIRY, STRIKE1.getLabel()).toString());
    assertThat(test.getStrike()).isEqualTo(STRIKE1);
    assertThat(test.getPeriod()).isEqualTo(TIME_TO_EXPIRY);
  }

  @Test
  public void test_builder_withLabel() {
    BeanBuilder<? extends GenericVolatilitySurfacePeriodParameterMetadata> builder =
        GenericVolatilitySurfacePeriodParameterMetadata.meta().builder();
    Pair<Period, Strike> pair = Pair.of(TIME_TO_EXPIRY, STRIKE1);
    String label = "(2, 0.75)";
    builder.set(GenericVolatilitySurfacePeriodParameterMetadata.meta().period(), TIME_TO_EXPIRY);
    builder.set(GenericVolatilitySurfacePeriodParameterMetadata.meta().strike(), STRIKE1);
    builder.set(GenericVolatilitySurfacePeriodParameterMetadata.meta().label(), label);
    GenericVolatilitySurfacePeriodParameterMetadata test = builder.build();
    assertThat(test.getIdentifier()).isEqualTo(pair);
    assertThat(test.getLabel()).isEqualTo(label);
    assertThat(test.getStrike()).isEqualTo(STRIKE1);
    assertThat(test.getPeriod()).isEqualTo(TIME_TO_EXPIRY);
  }

  @Test
  public void test_builder_incomplete() {
    BeanBuilder<? extends GenericVolatilitySurfacePeriodParameterMetadata> builder1 =
        GenericVolatilitySurfacePeriodParameterMetadata.meta().builder();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> builder1.build());
    BeanBuilder<? extends GenericVolatilitySurfacePeriodParameterMetadata> builder2 =
        GenericVolatilitySurfacePeriodParameterMetadata.meta().builder();
    builder2.set(GenericVolatilitySurfacePeriodParameterMetadata.meta().period(), TIME_TO_EXPIRY);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> builder2.build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    GenericVolatilitySurfacePeriodParameterMetadata test1 =
        GenericVolatilitySurfacePeriodParameterMetadata.of(TIME_TO_EXPIRY, STRIKE1);
    coverImmutableBean(test1);
    GenericVolatilitySurfacePeriodParameterMetadata test2 =
        GenericVolatilitySurfacePeriodParameterMetadata.of(Period.ofMonths(3), MoneynessStrike.of(1.1d));
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    GenericVolatilitySurfacePeriodParameterMetadata test =
        GenericVolatilitySurfacePeriodParameterMetadata.of(TIME_TO_EXPIRY, STRIKE1);
    assertSerialization(test);
  }

}
