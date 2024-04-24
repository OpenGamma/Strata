/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_20Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_30Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_40Y;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.joda.beans.BeanBuilder;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.tuple.Triple;

/**
 * Test {@link TenorTenorStrikeParameterMetadata}.
 */
public class TenorTenorStrikeParameterMetadataTest {

  @Test
  public void test_of_noLabel() {
    TenorTenorStrikeParameterMetadata test = TenorTenorStrikeParameterMetadata.of(TENOR_10Y, TENOR_20Y, 0.015);
    assertThat(test.getExpiryTenor()).isEqualTo(TENOR_10Y);
    assertThat(test.getUnderlyingTenor()).isEqualTo(TENOR_20Y);
    assertThat(test.getStrike()).isEqualTo(0.015);

    assertThat(test.getIdentifier()).isEqualTo(Triple.of(TENOR_10Y, TENOR_20Y, 0.015));
    assertThat(test.getLabel()).isEqualTo("[10Y, 20Y, 0.015]");
  }

  @Test
  public void test_of_label() {
    TenorTenorStrikeParameterMetadata test = TenorTenorStrikeParameterMetadata.of(
        TENOR_10Y,
        TENOR_20Y,
        0.011,
        "10Y to 20Y on 0.011");
    assertThat(test.getExpiryTenor()).isEqualTo(TENOR_10Y);
    assertThat(test.getUnderlyingTenor()).isEqualTo(TENOR_20Y);
    assertThat(test.getStrike()).isEqualTo(0.011);

    assertThat(test.getIdentifier()).isEqualTo(Triple.of(TENOR_10Y, TENOR_20Y, 0.011));
    assertThat(test.getLabel()).isEqualTo("10Y to 20Y on 0.011");
  }

  @Test
  public void test_builder_defaultLabel() {
    BeanBuilder<? extends TenorTenorStrikeParameterMetadata> builder = TenorTenorStrikeParameterMetadata.meta().builder();
    builder.set(TenorTenorStrikeParameterMetadata.meta().expiryTenor(), TENOR_10Y);
    builder.set(TenorTenorStrikeParameterMetadata.meta().underlyingTenor(), TENOR_20Y);
    builder.set(TenorTenorStrikeParameterMetadata.meta().strike(), -0.01);

    TenorTenorStrikeParameterMetadata test = builder.build();
    assertThat(test.getExpiryTenor()).isEqualTo(TENOR_10Y);
    assertThat(test.getUnderlyingTenor()).isEqualTo(TENOR_20Y);
    assertThat(test.getStrike()).isEqualTo(-0.01);

    assertThat(test.getIdentifier()).isEqualTo(Triple.of(TENOR_10Y, TENOR_20Y, -0.01));
    assertThat(test.getLabel()).isEqualTo("[10Y, 20Y, -0.01]");
  }

  @Test
  public void test_builder_specifyLabel() {
    BeanBuilder<? extends TenorTenorStrikeParameterMetadata> builder = TenorTenorStrikeParameterMetadata.meta().builder();
    builder.set(TenorTenorStrikeParameterMetadata.meta().expiryTenor(), TENOR_10Y);
    builder.set(TenorTenorStrikeParameterMetadata.meta().underlyingTenor(), TENOR_20Y);
    builder.set(TenorTenorStrikeParameterMetadata.meta().strike(), 0.01);
    builder.set(TenorTenorStrikeParameterMetadata.meta().label(), "10Y to 20Y, ATM");

    TenorTenorStrikeParameterMetadata test = builder.build();
    assertThat(test.getExpiryTenor()).isEqualTo(TENOR_10Y);
    assertThat(test.getUnderlyingTenor()).isEqualTo(TENOR_20Y);
    assertThat(test.getStrike()).isEqualTo(0.01);

    assertThat(test.getIdentifier()).isEqualTo(Triple.of(TENOR_10Y, TENOR_20Y, 0.01));
    assertThat(test.getLabel()).isEqualTo("10Y to 20Y, ATM");
  }

  @Test
  public void test_builder_incomplete() {
    BeanBuilder<? extends TenorTenorStrikeParameterMetadata> builder = TenorTenorStrikeParameterMetadata.meta().builder();
    assertThatIllegalArgumentException().isThrownBy(builder::build);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    TenorTenorStrikeParameterMetadata test = TenorTenorStrikeParameterMetadata.of(TENOR_10Y, TENOR_20Y, 0.015);
    coverImmutableBean(test);
    TenorTenorStrikeParameterMetadata test2 = TenorTenorStrikeParameterMetadata.of(TENOR_30Y, TENOR_40Y, 0.011);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    TenorTenorStrikeParameterMetadata test = TenorTenorStrikeParameterMetadata.of(TENOR_10Y, TENOR_20Y, 0.014);
    assertSerialization(test);
  }

}
