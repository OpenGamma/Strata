/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface.meta;

import static com.opengamma.strata.basics.currency.Currency.AUD;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.joda.beans.BeanBuilder;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.option.DeltaStrike;
import com.opengamma.strata.market.option.MoneynessStrike;
import com.opengamma.strata.market.option.SimpleStrike;
import com.opengamma.strata.market.option.Strike;

/**
 * Test {@link FxVolatilitySurfaceYearFractionNodeMetadata}.
 */
@Test
public class FxVolatilitySurfaceYearFractionNodeMetadataTest {

  private static final double TIME_TO_EXPIRY = 1.5d;
  private static final DeltaStrike STRIKE = DeltaStrike.of(0.75d);
  private static final SimpleStrike STRIKE1 = SimpleStrike.of(1.35);
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(GBP, USD);

  public void test_of_withStrikeType() {
    FxVolatilitySurfaceYearFractionNodeMetadata test =
        FxVolatilitySurfaceYearFractionNodeMetadata.of(TIME_TO_EXPIRY, STRIKE, CURRENCY_PAIR);
    assertEquals(test.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(test.getIdentifier(), Pair.of(TIME_TO_EXPIRY, STRIKE));
    assertEquals(test.getLabel(), Pair.of(TIME_TO_EXPIRY, STRIKE.getLabel()).toString());
    assertEquals(test.getStrike(), STRIKE);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_of_withLabel() {
    Pair<Double, Strike> pair = Pair.of(TIME_TO_EXPIRY, STRIKE1);
    String label = "(1.5, 1.35)";
    FxVolatilitySurfaceYearFractionNodeMetadata test =
        FxVolatilitySurfaceYearFractionNodeMetadata.of(TIME_TO_EXPIRY, STRIKE1, label, CURRENCY_PAIR);
    assertEquals(test.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), label);
    assertEquals(test.getStrike(), STRIKE1);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_builder_noLabel() {
    BeanBuilder<? extends FxVolatilitySurfaceYearFractionNodeMetadata> builder =
        FxVolatilitySurfaceYearFractionNodeMetadata.meta().builder();
    Pair<Double, Strike> pair = Pair.of(TIME_TO_EXPIRY, STRIKE);
    builder.set(FxVolatilitySurfaceYearFractionNodeMetadata.meta().currencyPair(), CURRENCY_PAIR);
    builder.set(FxVolatilitySurfaceYearFractionNodeMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(FxVolatilitySurfaceYearFractionNodeMetadata.meta().strike(), STRIKE);
    FxVolatilitySurfaceYearFractionNodeMetadata test = builder.build();
    assertEquals(test.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), Pair.of(TIME_TO_EXPIRY, STRIKE.getLabel()).toString());
    assertEquals(test.getStrike(), STRIKE);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_builder_withLabel() {
    BeanBuilder<? extends FxVolatilitySurfaceYearFractionNodeMetadata> builder =
        FxVolatilitySurfaceYearFractionNodeMetadata.meta().builder();
    Pair<Double, Strike> pair = Pair.of(TIME_TO_EXPIRY, STRIKE);
    String label = "(1.5, 0.75)";
    builder.set(FxVolatilitySurfaceYearFractionNodeMetadata.meta().currencyPair(), CURRENCY_PAIR);
    builder.set(FxVolatilitySurfaceYearFractionNodeMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(FxVolatilitySurfaceYearFractionNodeMetadata.meta().strike(), STRIKE);
    builder.set(FxVolatilitySurfaceYearFractionNodeMetadata.meta().label(), label);
    FxVolatilitySurfaceYearFractionNodeMetadata test = builder.build();
    assertEquals(test.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), label);
    assertEquals(test.getStrike(), STRIKE);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_builder_incomplete() {
    BeanBuilder<? extends FxVolatilitySurfaceYearFractionNodeMetadata> builder1 =
        FxVolatilitySurfaceYearFractionNodeMetadata.meta().builder();
    assertThrowsIllegalArg(() -> builder1.build());
    BeanBuilder<? extends FxVolatilitySurfaceYearFractionNodeMetadata> builder2 =
        FxVolatilitySurfaceYearFractionNodeMetadata.meta().builder();
    builder2.set(FxVolatilitySurfaceYearFractionNodeMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    assertThrowsIllegalArg(() -> builder2.build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxVolatilitySurfaceYearFractionNodeMetadata test1 =
        FxVolatilitySurfaceYearFractionNodeMetadata.of(TIME_TO_EXPIRY, STRIKE, CURRENCY_PAIR);
    coverImmutableBean(test1);
    FxVolatilitySurfaceYearFractionNodeMetadata test2 =
        FxVolatilitySurfaceYearFractionNodeMetadata.of(3d, MoneynessStrike.of(1.1d), CurrencyPair.of(EUR, AUD));
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    FxVolatilitySurfaceYearFractionNodeMetadata test =
        FxVolatilitySurfaceYearFractionNodeMetadata.of(TIME_TO_EXPIRY, STRIKE, CURRENCY_PAIR);
    assertSerialization(test);
  }

}
