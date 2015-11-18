/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.node;

import static com.opengamma.strata.basics.BuySell.BUY;
import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.market.FxRateKey;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.SimpleMarketDataKey;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveParameterMetadata;
import com.opengamma.strata.market.curve.meta.TenorCurveNodeMetadata;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swap.type.XCcyIborIborSwapConventions;
import com.opengamma.strata.product.swap.type.XCcyIborIborSwapTemplate;

/**
 * Test {@link XCcyIborIborSwapCurveNode}.
 */
@Test
public class XCcyIborIborSwapCurveNodeTest {

  private static final XCcyIborIborSwapTemplate TEMPLATE =
      XCcyIborIborSwapTemplate.of(Period.ZERO, TENOR_10Y, XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M);
  private static final XCcyIborIborSwapTemplate TEMPLATE2 =
      XCcyIborIborSwapTemplate.of(Period.ofMonths(1), TENOR_10Y, XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M);
  private static final QuoteKey SPREAD_KEY = QuoteKey.of(StandardId.of("OG-Ticker", "USD-EUR-XCS-10Y"));
  private static final QuoteKey SPREAD_KEY2 = QuoteKey.of(StandardId.of("OG-Ticker", "Test"));
  private static final FxRateKey FX_KEY = FxRateKey.of(Currency.EUR, Currency.USD);
  private static final double SPREAD_XCS = 0.00125;
  private static final FxRate FX_EUR_USD = FxRate.of(Currency.EUR, Currency.USD, 1.25);
  private static final double SPREAD_ADJ = 0.0015;
  private static final Map<MarketDataKey<?>, Object> MAP_OV = new HashMap<>();

  static {
    MAP_OV.put(SPREAD_KEY, SPREAD_XCS);
    MAP_OV.put(FX_KEY, FX_EUR_USD);
  }
  private static final MarketData OV = MarketData.of(MAP_OV);

  public void test_builder() {
    XCcyIborIborSwapCurveNode test = XCcyIborIborSwapCurveNode.builder()
        .template(TEMPLATE)
        .spreadKey(SPREAD_KEY)
        .fxKey(FX_KEY)
        .additionalSpread(SPREAD_ADJ)
        .build();
    assertEquals(test.getSpreadKey(), SPREAD_KEY);
    assertEquals(test.getFxKey(), FX_KEY);
    assertEquals(test.getAdditionalSpread(), SPREAD_ADJ);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_noSpread() {
    XCcyIborIborSwapCurveNode test = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_KEY);
    assertEquals(test.getSpreadKey(), SPREAD_KEY);
    assertEquals(test.getFxKey(), FX_KEY);
    assertEquals(test.getAdditionalSpread(), 0.0d);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_withSpread() {
    XCcyIborIborSwapCurveNode test = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_KEY, SPREAD_ADJ);
    assertEquals(test.getSpreadKey(), SPREAD_KEY);
    assertEquals(test.getFxKey(), FX_KEY);
    assertEquals(test.getAdditionalSpread(), SPREAD_ADJ);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_requirements() {
    XCcyIborIborSwapCurveNode test = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_KEY, SPREAD_ADJ);
    Set<? extends SimpleMarketDataKey<?>> setExpected = ImmutableSet.of(SPREAD_KEY, FX_KEY);
    Set<? extends SimpleMarketDataKey<?>> set = test.requirements();
    assertTrue(set.equals(setExpected));
  }

  public void test_trade() {
    XCcyIborIborSwapCurveNode node = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_KEY, SPREAD_ADJ);
    LocalDate tradeDate = LocalDate.of(2015, 1, 22);
    SwapTrade trade = node.trade(tradeDate, OV);
    double rate = FX_EUR_USD.fxRate(Currency.EUR, Currency.USD);
    SwapTrade expected = TEMPLATE.toTrade(tradeDate, BUY, 1, rate, SPREAD_XCS + SPREAD_ADJ);
    assertEquals(trade, expected);
  }

  public void test_trade_differentKey() {
    XCcyIborIborSwapCurveNode node = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_KEY, SPREAD_ADJ);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    double rate = 0.035;
    QuoteKey key = QuoteKey.of(StandardId.of("OG-Ticker", "Deposit2"));
    MarketData marketData = MarketData.builder().addValue(key, rate).build();
    assertThrowsIllegalArg(() -> node.trade(valuationDate, marketData));
  }

  public void test_initialGuess() {
    XCcyIborIborSwapCurveNode node = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_KEY, SPREAD_ADJ);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    assertEquals(node.initialGuess(valuationDate, OV, ValueType.ZERO_RATE), 0d);
    assertEquals(node.initialGuess(valuationDate, OV, ValueType.DISCOUNT_FACTOR), 1.0d);
  }

  public void test_metadata() {
    XCcyIborIborSwapCurveNode node = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_KEY, SPREAD_ADJ);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    CurveParameterMetadata metadata = node.metadata(valuationDate);
    // 2015-01-22 is Thursday, start is 2015-01-26, but 2025-01-26 is Sunday, so end is 2025-01-27
    assertEquals(((TenorCurveNodeMetadata) metadata).getDate(), LocalDate.of(2025, 1, 27));
    assertEquals(((TenorCurveNodeMetadata) metadata).getTenor(), Tenor.TENOR_10Y);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    XCcyIborIborSwapCurveNode test = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_KEY, SPREAD_ADJ);
    coverImmutableBean(test);
    XCcyIborIborSwapCurveNode test2 =
        XCcyIborIborSwapCurveNode.of(TEMPLATE2, SPREAD_KEY2, 0.1);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    XCcyIborIborSwapCurveNode test = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_KEY, SPREAD_ADJ);
    assertSerialization(test);
  }

}
