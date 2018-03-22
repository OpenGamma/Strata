/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.market.ValueType.BLACK_VOLATILITY;
import static com.opengamma.strata.market.ValueType.RISK_REVERSAL;
import static com.opengamma.strata.market.ValueType.STRANGLE;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
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
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.option.DeltaStrike;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilitiesName;

/**
 * Test {@link FxOptionVolatilitiesDefinition}.
 */
@Test
public class FxOptionVolatilitiesDefinitionTest {

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
  @SuppressWarnings("unused")
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
  private static final BlackFxOptionSmileVolatilitiesSpecification SPEC = BlackFxOptionSmileVolatilitiesSpecification.builder()
      .name(VOL_NAME)
      .currencyPair(EUR_GBP)
      .currencyPair(EUR_GBP)
      .dayCount(ACT_365F)
      .nodes(NODES)
      .timeInterpolator(LINEAR)
      .strikeInterpolator(LINEAR)
      .build();

  public void test_of() {
    FxOptionVolatilitiesDefinition test = FxOptionVolatilitiesDefinition.of(SPEC);
    assertEquals(test.getSpecification(), SPEC);
    assertEquals(test.getParameterCount(), SPEC.getParameterCount());
    assertEquals(test.volatilitiesInputs(), SPEC.volatilitiesInputs());
    ZonedDateTime dateTime = LocalDate.of(2017, 9, 25).atStartOfDay().atZone(ZoneId.of("Europe/London"));
    DoubleArray parameters = DoubleArray.of(0.05, -0.05, 0.15, 0.25, 0.1, -0.1);
    assertEquals(test.volatilities(dateTime, parameters, REF_DATA), SPEC.volatilities(dateTime, parameters, REF_DATA));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxOptionVolatilitiesDefinition test1 = FxOptionVolatilitiesDefinition.of(SPEC);
    coverImmutableBean(test1);
    BlackFxOptionSmileVolatilitiesSpecification spec2 = BlackFxOptionSmileVolatilitiesSpecification.builder()
        .name(VOL_NAME)
        .currencyPair(EUR_GBP)
        .dayCount(ACT_360)
        .nodes(NODES)
        .timeInterpolator(LINEAR)
        .strikeInterpolator(LINEAR)
        .build();
    FxOptionVolatilitiesDefinition test2 = FxOptionVolatilitiesDefinition.of(spec2);
    coverBeanEquals(test1, test2);
  }

  public void serialization() {
    FxOptionVolatilitiesDefinition test = FxOptionVolatilitiesDefinition.of(SPEC);
    assertSerialization(test);
  }

}
