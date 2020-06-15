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
import static com.opengamma.strata.collect.TestHelper.list;
import static com.opengamma.strata.market.ValueType.BLACK_VOLATILITY;
import static com.opengamma.strata.market.ValueType.RISK_REVERSAL;
import static com.opengamma.strata.market.ValueType.STRANGLE;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.FLAT;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.DOUBLE_QUADRATIC;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.PCHIP;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

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
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.option.DeltaStrike;
import com.opengamma.strata.pricer.fxopt.BlackFxOptionSmileVolatilities;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilitiesName;
import com.opengamma.strata.pricer.fxopt.InterpolatedStrikeSmileDeltaTermStructure;
import com.opengamma.strata.pricer.fxopt.SmileDeltaParameters;
import com.opengamma.strata.pricer.fxopt.SmileDeltaTermStructure;

/**
 * Test {@link BlackFxOptionSmileVolatilitiesSpecification}.
 */
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

  @Test
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
    assertThat(test.getCurrencyPair()).isEqualTo(EUR_GBP);
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.getName()).isEqualTo(VOL_NAME);
    assertThat(test.getNodes()).isEqualTo(NODES);
    assertThat(test.getParameterCount()).isEqualTo(TENORS.size());
    assertThat(test.getStrikeInterpolator()).isEqualTo(DOUBLE_QUADRATIC);
    assertThat(test.getStrikeExtrapolatorLeft()).isEqualTo(FLAT);
    assertThat(test.getStrikeExtrapolatorRight()).isEqualTo(LINEAR);
    assertThat(test.getTimeInterpolator()).isEqualTo(PCHIP);
    assertThat(test.getTimeExtrapolatorLeft()).isEqualTo(LINEAR);
    assertThat(test.getTimeExtrapolatorRight()).isEqualTo(FLAT);
    assertThat(test.volatilitiesInputs()).isEqualTo(QUOTE_IDS);
  }

  @Test
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
    double expiry3m = ACT_360.relativeYearFraction(
        date, expOffset.adjust(BUS_ADJ.adjust(spotDate.plus(Tenor.TENOR_3M), REF_DATA), REF_DATA));
    double expiry1y = ACT_360.relativeYearFraction(
        date, expOffset.adjust(BUS_ADJ.adjust(spotDate.plus(Tenor.TENOR_1Y), REF_DATA), REF_DATA));
    DoubleArray deltas = DoubleArray.of(0.1);
    SmileDeltaParameters params3m = SmileDeltaParameters.of(
        expiry3m, Tenor.TENOR_3M, 0.25, deltas, DoubleArray.of(-0.1), DoubleArray.of(0.1));
    SmileDeltaParameters params1y = SmileDeltaParameters.of(
        expiry1y, Tenor.TENOR_1Y, 0.15, deltas, DoubleArray.of(-0.05), DoubleArray.of(0.05));
    SmileDeltaTermStructure smiles = InterpolatedStrikeSmileDeltaTermStructure.of(
        list(params3m, params1y), ACT_360, PCHIP, FLAT, FLAT, PCHIP, FLAT, FLAT);
    BlackFxOptionSmileVolatilities expected = BlackFxOptionSmileVolatilities.of(VOL_NAME, EUR_GBP, dateTime, smiles);
    assertThat(computed).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
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

  @Test
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
