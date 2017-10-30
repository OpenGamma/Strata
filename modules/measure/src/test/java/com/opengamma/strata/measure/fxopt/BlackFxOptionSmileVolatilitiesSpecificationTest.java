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
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.market.ValueType.BLACK_VOLATILITY;
import static com.opengamma.strata.market.ValueType.RISK_REVERSAL;
import static com.opengamma.strata.market.ValueType.STRANGLE;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.FLAT;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.DOUBLE_QUADRATIC;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.PCHIP;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.option.DeltaStrike;
import com.opengamma.strata.pricer.fxopt.BlackFxOptionSmileVolatilities;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilitiesName;
import com.opengamma.strata.pricer.fxopt.InterpolatedStrikeSmileDeltaTermStructure;
import com.opengamma.strata.pricer.fxopt.SmileDeltaTermStructure;

/**
 * Test {@link BlackFxOptionSmileVolatilitiesSpecification}.
 */
@Test
public class BlackFxOptionSmileVolatilitiesSpecificationTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final FxOptionVolatilitiesName VOL_NAME = FxOptionVolatilitiesName.of("test");
  private static final CurrencyPair EUR_GBP = CurrencyPair.of(EUR, GBP);
  private static final HolidayCalendarId TA_LO = HolidayCalendarIds.EUTA.combinedWith(HolidayCalendarIds.GBLO);
  private static final DaysAdjustment SPOT_OFFSET = DaysAdjustment.ofBusinessDays(2, TA_LO);
  private static final BusinessDayAdjustment BUS_ADJ = BusinessDayAdjustment.of(FOLLOWING, TA_LO);
  
  private static final ImmutableList<Tenor> TENORS = ImmutableList.of(
      Tenor.TENOR_1Y, Tenor.TENOR_1Y, Tenor.TENOR_1Y, Tenor.TENOR_3M, Tenor.TENOR_3M, Tenor.TENOR_3M);
  private static final ImmutableList<Double> DELTAS = ImmutableList.of(0.1, 0.1, 0.5, 0.5, 0.1, 0.1);
  private static final ImmutableList<ValueType> QUOTE_TYPE = ImmutableList.of(
      STRANGLE, RISK_REVERSAL, BLACK_VOLATILITY, BLACK_VOLATILITY, STRANGLE, RISK_REVERSAL);
  private static final ImmutableList<FxOptionVolatilitiesNode> NODES;
  private static final ImmutableList<QuoteId> QUOTE_IDS;
  static {
    ImmutableList.Builder<FxOptionVolatilitiesNode> builder = ImmutableList.builder();
    ImmutableList.Builder<QuoteId> quoteBuilder = ImmutableList.builder();
    for (int i = 0; i < TENORS.size(); ++i) {
      QuoteId id = QuoteId.of(StandardId.of(
          "OG", TENORS.get(i).toString() + "_" + DELTAS.get(i).toString() + "_" + QUOTE_TYPE.get(i).toString()));
      builder.add(FxOptionVolatilitiesNode.of(
          EUR_GBP, SPOT_OFFSET, BUS_ADJ, QUOTE_TYPE.get(i), id, TENORS.get(i), DeltaStrike.of(DELTAS.get(i))));
      quoteBuilder.add(id);
    }
    NODES = builder.build();
    QUOTE_IDS = quoteBuilder.build();
  }

  public void test_builder() {
    BlackFxOptionSmileVolatilitiesSpecification test = BlackFxOptionSmileVolatilitiesSpecification.builder()
        .name(VOL_NAME)
        .currencyPair(EUR_GBP)
        .dayCount(ACT_360)
        .nodes(NODES)
        .timeInterpolator(PCHIP)
        .timeExtrapolatorLeft(LINEAR)
        .timeExtrapolatorRight(FLAT)
        .strikeInterpolator(DOUBLE_QUADRATIC)
        .strikeExtrapolatorLeft(FLAT)
        .strikeExtrapolatorRight(LINEAR)
        .build();
    assertEquals(test.getCurrencyPair(), EUR_GBP);
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.getName(), VOL_NAME);
    assertEquals(test.getNodes(), NODES);
    assertEquals(test.getParameterCount(), TENORS.size());
    assertEquals(test.getStrikeInterpolator(), DOUBLE_QUADRATIC);
    assertEquals(test.getStrikeExtrapolatorLeft(), FLAT);
    assertEquals(test.getStrikeExtrapolatorRight(), LINEAR);
    assertEquals(test.getTimeInterpolator(), PCHIP);
    assertEquals(test.getTimeExtrapolatorLeft(), LINEAR);
    assertEquals(test.getTimeExtrapolatorRight(), FLAT);
    assertEquals(test.volatilitiesInputs(), QUOTE_IDS);
  }

  public void test_volatilities() {
    BlackFxOptionSmileVolatilitiesSpecification base = BlackFxOptionSmileVolatilitiesSpecification.builder()
        .name(VOL_NAME)
        .currencyPair(EUR_GBP)
        .dayCount(ACT_360)
        .nodes(NODES)
        .timeInterpolator(PCHIP)
        .strikeInterpolator(PCHIP)
        .build();
    LocalDate date = LocalDate.of(2017, 9, 25);
    ZonedDateTime dateTime = date.atStartOfDay().atZone(ZoneId.of("Europe/London"));
    DoubleArray parameters = DoubleArray.of(0.05, -0.05, 0.15, 0.25, 0.1, -0.1);
    BlackFxOptionSmileVolatilities computed = base.volatilities(dateTime, parameters, REF_DATA);
    LocalDate spotDate = SPOT_OFFSET.adjust(dateTime.toLocalDate(), REF_DATA);
    DaysAdjustment expOffset = DaysAdjustment.ofBusinessDays(-2, TA_LO);
    DoubleArray expiries = DoubleArray.of(
        ACT_360.relativeYearFraction(date, expOffset.adjust(BUS_ADJ.adjust(spotDate.plus(Tenor.TENOR_3M), REF_DATA), REF_DATA)),
        ACT_360.relativeYearFraction(date, expOffset.adjust(BUS_ADJ.adjust(spotDate.plus(Tenor.TENOR_1Y), REF_DATA), REF_DATA)));
    SmileDeltaTermStructure smiles = InterpolatedStrikeSmileDeltaTermStructure.of(
        expiries, DoubleArray.of(0.1), DoubleArray.of(0.25, 0.15), DoubleMatrix.ofUnsafe(new double[][] {{-0.1}, {-0.05}}),
        DoubleMatrix.ofUnsafe(new double[][] {{0.1}, {0.05}}), ACT_360, PCHIP, FLAT, FLAT, PCHIP, FLAT, FLAT);
    BlackFxOptionSmileVolatilities expected = BlackFxOptionSmileVolatilities.of(VOL_NAME, EUR_GBP, dateTime, smiles);
    assertEquals(computed, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    BlackFxOptionSmileVolatilitiesSpecification test1 = BlackFxOptionSmileVolatilitiesSpecification.builder()
        .name(VOL_NAME)
        .currencyPair(EUR_GBP)
        .dayCount(ACT_360)
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
    ImmutableList.Builder<FxOptionVolatilitiesNode> builder = ImmutableList.builder();
    for (int i = 0; i < TENORS.size(); ++i) {
      QuoteId id = QuoteId.of(StandardId.of(
          "OG", TENORS.get(i).toString() + "_" + DELTAS.get(i).toString() + "_" + QUOTE_TYPE.get(i).toString()));
      builder.add(FxOptionVolatilitiesNode.of(eurUsd, DaysAdjustment.NONE, BusinessDayAdjustment.NONE, QUOTE_TYPE.get(i), id,
          TENORS.get(i), DeltaStrike.of(DELTAS.get(i))));
    }
    BlackFxOptionSmileVolatilitiesSpecification test2 = BlackFxOptionSmileVolatilitiesSpecification.builder()
        .name(FxOptionVolatilitiesName.of("other"))
        .currencyPair(eurUsd)
        .dayCount(ACT_365F)
        .nodes(builder.build())
        .timeInterpolator(DOUBLE_QUADRATIC)
        .strikeInterpolator(DOUBLE_QUADRATIC)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void serialization() {
    BlackFxOptionSmileVolatilitiesSpecification test = BlackFxOptionSmileVolatilitiesSpecification.builder()
        .name(VOL_NAME)
        .currencyPair(EUR_GBP)
        .dayCount(ACT_360)
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
