/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static com.opengamma.strata.basics.currency.Currency.AUD;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.Tenor.TENOR_12M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_18M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.joda.beans.BeanBuilder;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.option.DeltaStrike;
import com.opengamma.strata.market.option.MoneynessStrike;
import com.opengamma.strata.market.option.SimpleStrike;
import com.opengamma.strata.market.option.Strike;

/**
 * Test {@link FxVolatilitySurfaceTenorParameterMetadata}.
 */
public class FxVolatilitySurfaceTenorParameterMetadataTest {

  private static final double TIME_TO_EXPIRY = 1.5d;
  private static final DeltaStrike STRIKE = DeltaStrike.of(0.75d);
  private static final SimpleStrike STRIKE1 = SimpleStrike.of(1.35);
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(GBP, USD);

  @Test
  public void test_of_withStrikeType() {
    FxVolatilitySurfaceTenorParameterMetadata test =
        FxVolatilitySurfaceTenorParameterMetadata.of(TENOR_18M, TIME_TO_EXPIRY, STRIKE, CURRENCY_PAIR);
    assertThat(test.getCurrencyPair()).isEqualTo(CURRENCY_PAIR);
    assertThat(test.getIdentifier()).isEqualTo(Pair.of(TIME_TO_EXPIRY, STRIKE));
    assertThat(test.getLabel()).isEqualTo(Pair.of(TENOR_18M, STRIKE.getLabel()).toString());
    assertThat(test.getStrike()).isEqualTo(STRIKE);
    assertThat(test.getYearFraction()).isEqualTo(TIME_TO_EXPIRY);
  }

  @Test
  public void test_of_withLabel() {
    Pair<Double, Strike> pair = Pair.of(TIME_TO_EXPIRY, STRIKE1);
    String label = "(1.5, 1.35)";
    FxVolatilitySurfaceTenorParameterMetadata test =
        FxVolatilitySurfaceTenorParameterMetadata.of(TENOR_18M, TIME_TO_EXPIRY, STRIKE1, CURRENCY_PAIR, label);
    assertThat(test.getCurrencyPair()).isEqualTo(CURRENCY_PAIR);
    assertThat(test.getIdentifier()).isEqualTo(pair);
    assertThat(test.getLabel()).isEqualTo(label);
    assertThat(test.getStrike()).isEqualTo(STRIKE1);
    assertThat(test.getYearFraction()).isEqualTo(TIME_TO_EXPIRY);
  }

  @Test
  public void test_builder_noLabel() {
    BeanBuilder<? extends FxVolatilitySurfaceTenorParameterMetadata> builder =
        FxVolatilitySurfaceTenorParameterMetadata.meta().builder();
    Pair<Double, Strike> pair = Pair.of(TIME_TO_EXPIRY, STRIKE);
    builder.set(FxVolatilitySurfaceTenorParameterMetadata.meta().tenor(), TENOR_18M);
    builder.set(FxVolatilitySurfaceTenorParameterMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(FxVolatilitySurfaceTenorParameterMetadata.meta().strike(), STRIKE);
    builder.set(FxVolatilitySurfaceTenorParameterMetadata.meta().currencyPair(), CURRENCY_PAIR);
    FxVolatilitySurfaceTenorParameterMetadata test = builder.build();
    assertThat(test.getCurrencyPair()).isEqualTo(CURRENCY_PAIR);
    assertThat(test.getIdentifier()).isEqualTo(pair);
    assertThat(test.getLabel()).isEqualTo(Pair.of(TIME_TO_EXPIRY, STRIKE.getLabel()).toString());
    assertThat(test.getStrike()).isEqualTo(STRIKE);
    assertThat(test.getYearFraction()).isEqualTo(TIME_TO_EXPIRY);
  }

  @Test
  public void test_builder_withLabel() {
    BeanBuilder<? extends FxVolatilitySurfaceTenorParameterMetadata> builder =
        FxVolatilitySurfaceTenorParameterMetadata.meta().builder();
    Pair<Double, Strike> pair = Pair.of(TIME_TO_EXPIRY, STRIKE);
    String label = "(1.5, 0.75)";
    builder.set(FxVolatilitySurfaceTenorParameterMetadata.meta().tenor(), TENOR_18M);
    builder.set(FxVolatilitySurfaceTenorParameterMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(FxVolatilitySurfaceTenorParameterMetadata.meta().strike(), STRIKE);
    builder.set(FxVolatilitySurfaceTenorParameterMetadata.meta().currencyPair(), CURRENCY_PAIR);
    builder.set(FxVolatilitySurfaceTenorParameterMetadata.meta().label(), label);
    FxVolatilitySurfaceTenorParameterMetadata test = builder.build();
    assertThat(test.getCurrencyPair()).isEqualTo(CURRENCY_PAIR);
    assertThat(test.getIdentifier()).isEqualTo(pair);
    assertThat(test.getLabel()).isEqualTo(label);
    assertThat(test.getStrike()).isEqualTo(STRIKE);
    assertThat(test.getYearFraction()).isEqualTo(TIME_TO_EXPIRY);
  }

  @Test
  public void test_builder_incomplete() {
    BeanBuilder<? extends FxVolatilitySurfaceTenorParameterMetadata> builder1 =
        FxVolatilitySurfaceTenorParameterMetadata.meta().builder();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> builder1.build());
    BeanBuilder<? extends FxVolatilitySurfaceTenorParameterMetadata> builder2 =
        FxVolatilitySurfaceTenorParameterMetadata.meta().builder();
    builder2.set(FxVolatilitySurfaceTenorParameterMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> builder2.build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FxVolatilitySurfaceTenorParameterMetadata test1 =
        FxVolatilitySurfaceTenorParameterMetadata.of(TENOR_18M, TIME_TO_EXPIRY, STRIKE, CURRENCY_PAIR);
    coverImmutableBean(test1);
    FxVolatilitySurfaceTenorParameterMetadata test2 =
        FxVolatilitySurfaceTenorParameterMetadata.of(TENOR_12M, 3d, MoneynessStrike.of(1.1d), CurrencyPair.of(EUR, AUD));
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    FxVolatilitySurfaceTenorParameterMetadata test =
        FxVolatilitySurfaceTenorParameterMetadata.of(TENOR_18M, TIME_TO_EXPIRY, STRIKE, CURRENCY_PAIR);
    assertSerialization(test);
  }

}
