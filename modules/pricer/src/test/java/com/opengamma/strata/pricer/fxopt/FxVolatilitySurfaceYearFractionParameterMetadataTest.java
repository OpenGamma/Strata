/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static com.opengamma.strata.basics.currency.Currency.AUD;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.joda.beans.BeanBuilder;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.option.DeltaStrike;
import com.opengamma.strata.market.option.MoneynessStrike;
import com.opengamma.strata.market.option.SimpleStrike;
import com.opengamma.strata.market.option.Strike;

/**
 * Test {@link FxVolatilitySurfaceYearFractionParameterMetadata}.
 */
public class FxVolatilitySurfaceYearFractionParameterMetadataTest {

  private static final double TIME_TO_EXPIRY = 1.5d;
  private static final DeltaStrike STRIKE = DeltaStrike.of(0.75d);
  private static final SimpleStrike STRIKE1 = SimpleStrike.of(1.35);
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(GBP, USD);

  @Test
  public void test_of_withStrikeType() {
    FxVolatilitySurfaceYearFractionParameterMetadata test =
        FxVolatilitySurfaceYearFractionParameterMetadata.of(TIME_TO_EXPIRY, STRIKE, CURRENCY_PAIR);
    assertThat(test.getCurrencyPair()).isEqualTo(CURRENCY_PAIR);
    assertThat(test.getIdentifier()).isEqualTo(Pair.of(TIME_TO_EXPIRY, STRIKE));
    assertThat(test.getLabel()).isEqualTo(Pair.of(TIME_TO_EXPIRY, STRIKE.getLabel()).toString());
    assertThat(test.getStrike()).isEqualTo(STRIKE);
    assertThat(test.getYearFraction()).isEqualTo(TIME_TO_EXPIRY);
  }

  @Test
  public void test_of_withTenor() {
    FxVolatilitySurfaceYearFractionParameterMetadata test =
        FxVolatilitySurfaceYearFractionParameterMetadata.of(TIME_TO_EXPIRY, Tenor.TENOR_18M, STRIKE, CURRENCY_PAIR);
    assertThat(test.getCurrencyPair()).isEqualTo(CURRENCY_PAIR);
    assertThat(test.getIdentifier()).isEqualTo(Pair.of(TIME_TO_EXPIRY, STRIKE));
    assertThat(test.getLabel()).isEqualTo("[18M, Delta=0.75]");
    assertThat(test.getStrike()).isEqualTo(STRIKE);
    assertThat(test.getYearFraction()).isEqualTo(TIME_TO_EXPIRY);
    assertThat(test.getYearFractionTenor()).hasValue(Tenor.TENOR_18M);
  }

  @Test
  public void test_of_withLabel() {
    Pair<Double, Strike> pair = Pair.of(TIME_TO_EXPIRY, STRIKE1);
    String label = "(1.5, 1.35)";
    FxVolatilitySurfaceYearFractionParameterMetadata test =
        FxVolatilitySurfaceYearFractionParameterMetadata.of(TIME_TO_EXPIRY, STRIKE1, label, CURRENCY_PAIR);
    assertThat(test.getCurrencyPair()).isEqualTo(CURRENCY_PAIR);
    assertThat(test.getIdentifier()).isEqualTo(pair);
    assertThat(test.getLabel()).isEqualTo(label);
    assertThat(test.getStrike()).isEqualTo(STRIKE1);
    assertThat(test.getYearFraction()).isEqualTo(TIME_TO_EXPIRY);
  }

  @Test
  public void test_builder_noLabel() {
    BeanBuilder<? extends FxVolatilitySurfaceYearFractionParameterMetadata> builder =
        FxVolatilitySurfaceYearFractionParameterMetadata.meta().builder();
    Pair<Double, Strike> pair = Pair.of(TIME_TO_EXPIRY, STRIKE);
    builder.set(FxVolatilitySurfaceYearFractionParameterMetadata.meta().currencyPair(), CURRENCY_PAIR);
    builder.set(FxVolatilitySurfaceYearFractionParameterMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(FxVolatilitySurfaceYearFractionParameterMetadata.meta().strike(), STRIKE);
    FxVolatilitySurfaceYearFractionParameterMetadata test = builder.build();
    assertThat(test.getCurrencyPair()).isEqualTo(CURRENCY_PAIR);
    assertThat(test.getIdentifier()).isEqualTo(pair);
    assertThat(test.getLabel()).isEqualTo(Pair.of(TIME_TO_EXPIRY, STRIKE.getLabel()).toString());
    assertThat(test.getStrike()).isEqualTo(STRIKE);
    assertThat(test.getYearFraction()).isEqualTo(TIME_TO_EXPIRY);
  }

  @Test
  public void test_builder_withLabel() {
    BeanBuilder<? extends FxVolatilitySurfaceYearFractionParameterMetadata> builder =
        FxVolatilitySurfaceYearFractionParameterMetadata.meta().builder();
    Pair<Double, Strike> pair = Pair.of(TIME_TO_EXPIRY, STRIKE);
    String label = "(1.5, 0.75)";
    builder.set(FxVolatilitySurfaceYearFractionParameterMetadata.meta().currencyPair(), CURRENCY_PAIR);
    builder.set(FxVolatilitySurfaceYearFractionParameterMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(FxVolatilitySurfaceYearFractionParameterMetadata.meta().strike(), STRIKE);
    builder.set(FxVolatilitySurfaceYearFractionParameterMetadata.meta().label(), label);
    FxVolatilitySurfaceYearFractionParameterMetadata test = builder.build();
    assertThat(test.getCurrencyPair()).isEqualTo(CURRENCY_PAIR);
    assertThat(test.getIdentifier()).isEqualTo(pair);
    assertThat(test.getLabel()).isEqualTo(label);
    assertThat(test.getStrike()).isEqualTo(STRIKE);
    assertThat(test.getYearFraction()).isEqualTo(TIME_TO_EXPIRY);
  }

  @Test
  public void test_builder_incomplete() {
    BeanBuilder<? extends FxVolatilitySurfaceYearFractionParameterMetadata> builder1 =
        FxVolatilitySurfaceYearFractionParameterMetadata.meta().builder();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> builder1.build());
    BeanBuilder<? extends FxVolatilitySurfaceYearFractionParameterMetadata> builder2 =
        FxVolatilitySurfaceYearFractionParameterMetadata.meta().builder();
    builder2.set(FxVolatilitySurfaceYearFractionParameterMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> builder2.build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FxVolatilitySurfaceYearFractionParameterMetadata test1 =
        FxVolatilitySurfaceYearFractionParameterMetadata.of(TIME_TO_EXPIRY, STRIKE, CURRENCY_PAIR);
    coverImmutableBean(test1);
    FxVolatilitySurfaceYearFractionParameterMetadata test2 =
        FxVolatilitySurfaceYearFractionParameterMetadata.of(3d, MoneynessStrike.of(1.1d), CurrencyPair.of(EUR, AUD));
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    FxVolatilitySurfaceYearFractionParameterMetadata test =
        FxVolatilitySurfaceYearFractionParameterMetadata.of(TIME_TO_EXPIRY, STRIKE, CURRENCY_PAIR);
    assertSerialization(test);
  }

}
