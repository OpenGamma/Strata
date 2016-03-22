/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.node;

import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;

import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.assertThrowsWithCause;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.Period;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.market.FxRateKey;
import com.opengamma.strata.basics.market.ImmutableMarketData;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.basics.market.SimpleMarketDataKey;
import com.opengamma.strata.basics.market.StandardId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveParameterMetadata;
import com.opengamma.strata.market.curve.DatedCurveParameterMetadata;
import com.opengamma.strata.market.curve.meta.TenorDateCurveNodeMetadata;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.product.fx.FxSwapTrade;
import com.opengamma.strata.product.fx.type.FxSwapTemplate;
import com.opengamma.strata.product.fx.type.ImmutableFxSwapConvention;

/**
 * Test {@link FxSwapCurveNode}.
 */
@Test
public class FxSwapCurveNodeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final CurrencyPair EUR_USD = CurrencyPair.of(Currency.EUR, Currency.USD);
  private static final HolidayCalendarId EUTA_USNY = EUTA.combinedWith(USNY);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, EUTA_USNY);
  private static final ImmutableFxSwapConvention CONVENTION = ImmutableFxSwapConvention.of(EUR_USD, PLUS_TWO_DAYS);
  private static final Period NEAR_PERIOD = Period.ofMonths(3);
  private static final Period FAR_PERIOD = Period.ofMonths(6);
  private static final FxSwapTemplate TEMPLATE = FxSwapTemplate.of(NEAR_PERIOD, FAR_PERIOD, CONVENTION);

  private static final FxRateKey RATE_KEY_NEAR = FxRateKey.of(EUR_USD);
  private static final QuoteKey QUOTE_KEY_PTS = QuoteKey.of(StandardId.of("OG-Ticker", "EUR_USD_3M_6M"));
  private static final FxRate FX_RATE_NEAR = FxRate.of(EUR_USD, 1.30d);
  private static final double FX_RATE_PTS = 0.0050d;
  private static final String LABEL = "Label";
  private static final String LABEL_AUTO = "6M";
  private static final MarketData OV = ImmutableMarketData.builder(VAL_DATE)
      .addValue(RATE_KEY_NEAR, FX_RATE_NEAR)
      .addValue(QUOTE_KEY_PTS, FX_RATE_PTS)
      .build();

  public void test_builder() {
    FxSwapCurveNode test = FxSwapCurveNode.builder()
        .label(LABEL)
        .template(TEMPLATE)
        .farForwardPointsKey(QUOTE_KEY_PTS)
        .build();
    assertEquals(test.getLabel(), LABEL);
    assertEquals(test.getFarForwardPointsKey(), QUOTE_KEY_PTS);
    assertEquals(test.getTemplate(), TEMPLATE);
    assertEquals(test.getDate(), CurveNodeDate.END);
  }

  public void test_of() {
    FxSwapCurveNode test = FxSwapCurveNode.of(TEMPLATE, QUOTE_KEY_PTS);
    assertEquals(test.getLabel(), LABEL_AUTO);
    assertEquals(test.getFarForwardPointsKey(), QUOTE_KEY_PTS);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_withLabel() {
    FxSwapCurveNode test = FxSwapCurveNode.of(TEMPLATE, QUOTE_KEY_PTS, LABEL);
    assertEquals(test.getLabel(), LABEL);
    assertEquals(test.getFarForwardPointsKey(), QUOTE_KEY_PTS);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_requirements() {
    FxSwapCurveNode test = FxSwapCurveNode.of(TEMPLATE, QUOTE_KEY_PTS);
    Set<? extends MarketDataKey<?>> setExpected = ImmutableSet.of(RATE_KEY_NEAR, QUOTE_KEY_PTS);
    Set<? extends SimpleMarketDataKey<?>> set = test.requirements();
    assertTrue(set.equals(setExpected));
  }

  public void test_trade() {
    FxSwapCurveNode node = FxSwapCurveNode.of(TEMPLATE, QUOTE_KEY_PTS);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    FxSwapTrade trade = node.trade(valuationDate, OV, REF_DATA);
    double rate = FX_RATE_NEAR.fxRate(EUR_USD);
    FxSwapTrade expected = TEMPLATE.createTrade(valuationDate, BuySell.BUY, 1.0, rate, FX_RATE_PTS, REF_DATA);
    assertEquals(trade, expected);
  }

  public void test_trade_differentKey() {
    FxSwapCurveNode node = FxSwapCurveNode.of(TEMPLATE, QUOTE_KEY_PTS);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    double rate = 0.035;
    QuoteKey quoteKey = QuoteKey.of(StandardId.of("OG-Ticker", "Deposit2"));
    ImmutableMarketData md = ImmutableMarketData.builder(valuationDate).addValue(quoteKey, rate).build();
    assertThrowsIllegalArg(() -> node.trade(valuationDate, md, REF_DATA));
  }

  public void test_initialGuess() {
    FxSwapCurveNode node = FxSwapCurveNode.of(TEMPLATE, QUOTE_KEY_PTS);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    assertEquals(node.initialGuess(valuationDate, OV, ValueType.ZERO_RATE), 0.0d);
    assertEquals(node.initialGuess(valuationDate, OV, ValueType.DISCOUNT_FACTOR), 1.0d);
  }

  public void test_metadata_end() {
    FxSwapCurveNode node = FxSwapCurveNode.of(TEMPLATE, QUOTE_KEY_PTS);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    LocalDate endDate = CONVENTION.getBusinessDayAdjustment()
        .adjust(CONVENTION.getSpotDateOffset().adjust(valuationDate, REF_DATA).plus(FAR_PERIOD), REF_DATA);
    CurveParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    assertEquals(((TenorDateCurveNodeMetadata) metadata).getDate(), endDate);
    assertEquals(((TenorDateCurveNodeMetadata) metadata).getTenor(), Tenor.of(FAR_PERIOD));
  }

  public void test_metadata_fixed() {
    LocalDate nodeDate = VAL_DATE.plusMonths(1);
    FxSwapCurveNode node = FxSwapCurveNode.of(TEMPLATE, QUOTE_KEY_PTS).withDate(CurveNodeDate.of(nodeDate));
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    DatedCurveParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    assertEquals(metadata.getDate(), nodeDate);
    assertEquals(metadata.getLabel(), node.getLabel());
  }

  public void test_metadata_last_fixing() {
    FxSwapCurveNode node = FxSwapCurveNode.of(TEMPLATE, QUOTE_KEY_PTS).withDate(CurveNodeDate.LAST_FIXING);
    assertThrowsWithCause(() -> node.metadata(VAL_DATE, REF_DATA), UnsupportedOperationException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxSwapCurveNode test = FxSwapCurveNode.of(TEMPLATE, QUOTE_KEY_PTS);
    coverImmutableBean(test);
    FxSwapCurveNode test2 =
        FxSwapCurveNode.of(FxSwapTemplate.of(Period.ZERO, FAR_PERIOD, CONVENTION), QUOTE_KEY_PTS);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FxSwapCurveNode test = FxSwapCurveNode.of(TEMPLATE, QUOTE_KEY_PTS);
    assertSerialization(test);
  }

}
