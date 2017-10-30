/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.FLAT;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.DOUBLE_QUADRATIC;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.PCHIP;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.option.SimpleStrike;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.pricer.fxopt.BlackFxOptionSurfaceVolatilities;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilitiesName;
import com.opengamma.strata.pricer.fxopt.FxVolatilitySurfaceYearFractionParameterMetadata;

/**
 * Test {@link BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification}.
 */
@Test
public class BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecificationTest {

  private static final FxOptionVolatilitiesName VOL_NAME = FxOptionVolatilitiesName.of("test");
  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final CurrencyPair GBP_USD = CurrencyPair.of(GBP, USD);
  private static final HolidayCalendarId NY_LO = USNY.combinedWith(GBLO);
  private static final DaysAdjustment SPOT_OFFSET = DaysAdjustment.ofBusinessDays(2, NY_LO);
  private static final BusinessDayAdjustment BDA = BusinessDayAdjustment.of(FOLLOWING, NY_LO);
  private static final List<Tenor> TENORS = ImmutableList.of(Tenor.TENOR_3M, Tenor.TENOR_6M, Tenor.TENOR_1Y);
  private static final List<Double> STRIKES = ImmutableList.of(1.35, 1.5, 1.65, 1.7);
  private static final ImmutableList<FxOptionVolatilitiesNode> NODES;
  private static final ImmutableList<QuoteId> QUOTE_IDS;
  static {
    ImmutableList.Builder<FxOptionVolatilitiesNode> nodeBuilder = ImmutableList.builder();
    ImmutableList.Builder<QuoteId> quoteIdBuilder = ImmutableList.builder();
    for (int i = 0; i < TENORS.size(); ++i) {
      for (int j = 0; j < STRIKES.size(); ++j) {
        QuoteId quoteId = QuoteId.of(StandardId.of(
            "OG", GBP_USD.toString() + "_" + TENORS.get(i).toString() + "_" + STRIKES.get(j)));
        nodeBuilder.add(FxOptionVolatilitiesNode.of(
            GBP_USD, SPOT_OFFSET, BDA, ValueType.BLACK_VOLATILITY, quoteId, TENORS.get(i), SimpleStrike.of(STRIKES.get(j))));
        quoteIdBuilder.add(quoteId);
      }
    }
    NODES = nodeBuilder.build();
    QUOTE_IDS = quoteIdBuilder.build();
  }

  public void test_builder() {
    BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification test =
        BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.builder()
            .name(VOL_NAME)
            .currencyPair(GBP_USD)
            .dayCount(ACT_365F)
            .nodes(NODES)
            .timeInterpolator(PCHIP)
            .timeExtrapolatorLeft(LINEAR)
            .timeExtrapolatorRight(FLAT)
            .strikeInterpolator(DOUBLE_QUADRATIC)
            .strikeExtrapolatorLeft(FLAT)
            .strikeExtrapolatorRight(LINEAR)
            .build();
    assertEquals(test.getCurrencyPair(), GBP_USD);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getName(), VOL_NAME);
    assertEquals(test.getNodes(), NODES);
    assertEquals(test.getParameterCount(), NODES.size());
    assertEquals(test.getStrikeInterpolator(), DOUBLE_QUADRATIC);
    assertEquals(test.getStrikeExtrapolatorLeft(), FLAT);
    assertEquals(test.getStrikeExtrapolatorRight(), LINEAR);
    assertEquals(test.getTimeInterpolator(), PCHIP);
    assertEquals(test.getTimeExtrapolatorLeft(), LINEAR);
    assertEquals(test.getTimeExtrapolatorRight(), FLAT);
    assertEquals(test.volatilitiesInputs(), QUOTE_IDS);
  }

  public void test_volatilities() {
    BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification base =
        BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.builder()
            .name(VOL_NAME)
            .currencyPair(GBP_USD)
            .dayCount(ACT_365F)
            .nodes(NODES)
            .timeInterpolator(PCHIP)
            .strikeInterpolator(DOUBLE_QUADRATIC)
            .build();
    LocalDate date = LocalDate.of(2017, 9, 25);
    ZonedDateTime dateTime = date.atStartOfDay().atZone(ZoneId.of("Europe/London"));
    DoubleArray parameters = DoubleArray.of(0.19, 0.15, 0.13, 0.14, 0.14, 0.11, 0.09, 0.09, 0.11, 0.09, 0.07, 0.07);
    BlackFxOptionSurfaceVolatilities computed = base.volatilities(dateTime, parameters, REF_DATA);
    DaysAdjustment expOffset = DaysAdjustment.ofBusinessDays(-2, NY_LO);
    double[] expiries = new double[STRIKES.size() * TENORS.size()];
    double[] strikes = new double[STRIKES.size() * TENORS.size()];
    ImmutableList.Builder<ParameterMetadata> paramMetadata = ImmutableList.builder();
    for (int i = 0; i < TENORS.size(); ++i) {
      double expiry = ACT_365F.relativeYearFraction(
          date, expOffset.adjust(BDA.adjust(SPOT_OFFSET.adjust(date, REF_DATA).plus(TENORS.get(i)), REF_DATA), REF_DATA));
      for (int j = 0; j < STRIKES.size(); ++j) {
        paramMetadata.add(FxVolatilitySurfaceYearFractionParameterMetadata.of(expiry, SimpleStrike.of(STRIKES.get(j)), GBP_USD));
        expiries[STRIKES.size() * i + j] = expiry;
        strikes[STRIKES.size() * i + j] = STRIKES.get(j);
      }
    }
    InterpolatedNodalSurface surface = InterpolatedNodalSurface.ofUnsorted(
        Surfaces.blackVolatilityByExpiryStrike(VOL_NAME.getName(), ACT_365F).withParameterMetadata(paramMetadata.build()),
        DoubleArray.ofUnsafe(expiries), DoubleArray.ofUnsafe(strikes), parameters,
        GridSurfaceInterpolator.of(PCHIP, DOUBLE_QUADRATIC));
    BlackFxOptionSurfaceVolatilities expected = BlackFxOptionSurfaceVolatilities.of(VOL_NAME, GBP_USD, dateTime, surface);
    assertEquals(computed, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification test1 =
        BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.builder()
            .name(VOL_NAME)
            .currencyPair(GBP_USD)
            .dayCount(ACT_365F)
            .nodes(NODES)
            .timeInterpolator(PCHIP)
            .timeExtrapolatorLeft(LINEAR)
            .timeExtrapolatorRight(LINEAR)
            .strikeInterpolator(PCHIP)
            .strikeExtrapolatorLeft(LINEAR)
            .strikeExtrapolatorRight(LINEAR)
            .build();
    coverImmutableBean(test1);
    CurrencyPair eurUsd = CurrencyPair.of(EUR, USD);
    ImmutableList.Builder<FxOptionVolatilitiesNode> nodeBuilder = ImmutableList.builder();
    for (int i = 0; i < TENORS.size(); ++i) {
      for (int j = 0; j < STRIKES.size(); ++j) {
        QuoteId quoteId = QuoteId.of(StandardId.of(
            "OG", eurUsd.toString() + "_" + TENORS.get(i).toString() + "_" + STRIKES.get(j)));
        nodeBuilder.add(FxOptionVolatilitiesNode.of(
            eurUsd, SPOT_OFFSET, BDA, ValueType.BLACK_VOLATILITY, quoteId, TENORS.get(i), SimpleStrike.of(STRIKES.get(j))));
      }
    }
    BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification test2 =
        BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.builder()
            .name(FxOptionVolatilitiesName.of("other"))
            .currencyPair(eurUsd)
            .dayCount(ACT_360)
            .nodes(nodeBuilder.build())
            .timeInterpolator(DOUBLE_QUADRATIC)
            .strikeInterpolator(DOUBLE_QUADRATIC)
            .build();
    coverBeanEquals(test1, test2);
  }

  public void serialization() {
    BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification test =
        BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.builder()
            .name(VOL_NAME)
            .currencyPair(GBP_USD)
            .dayCount(ACT_365F)
            .nodes(NODES)
            .timeInterpolator(PCHIP)
            .timeExtrapolatorLeft(LINEAR)
            .timeExtrapolatorRight(FLAT)
            .strikeInterpolator(DOUBLE_QUADRATIC)
            .strikeExtrapolatorLeft(FLAT)
            .strikeExtrapolatorRight(LINEAR)
            .build();
    assertSerialization(test);
  }

}
