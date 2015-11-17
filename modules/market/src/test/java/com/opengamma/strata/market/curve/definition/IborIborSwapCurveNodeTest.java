/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.definition;

import static com.opengamma.strata.basics.BuySell.BUY;
import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.time.LocalDate;
import java.time.Period;
import java.util.Iterator;
import java.util.Set;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.market.curve.CurveParameterMetadata;
import com.opengamma.strata.market.curve.TenorCurveNodeMetadata;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.market.value.ValueType;
import com.opengamma.strata.product.rate.swap.SwapTrade;
import com.opengamma.strata.product.rate.swap.type.IborIborSwapConventions;
import com.opengamma.strata.product.rate.swap.type.IborIborSwapTemplate;

/**
 * Test {@link IborIborSwapCurveNode}.
 */
@Test
public class IborIborSwapCurveNodeTest {

  private static final IborIborSwapTemplate TEMPLATE =
      IborIborSwapTemplate.of(Period.ZERO, TENOR_10Y, IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M);
  private static final IborIborSwapTemplate TEMPLATE2 =
      IborIborSwapTemplate.of(Period.ofMonths(1), TENOR_10Y, IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M);
  private static final QuoteKey QUOTE_KEY = QuoteKey.of(StandardId.of("OG-Ticker", "USD-BS36-10Y"));
  private static final QuoteKey QUOTE_KEY2 = QuoteKey.of(StandardId.of("OG-Ticker", "Test"));
  private static final double SPREAD = 0.0015;

  public void test_builder() {
    IborIborSwapCurveNode test = IborIborSwapCurveNode.builder()
        .template(TEMPLATE)
        .rateKey(QUOTE_KEY)
        .additionalSpread(SPREAD)
        .build();
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_noSpread() {
    IborIborSwapCurveNode test = IborIborSwapCurveNode.of(TEMPLATE, QUOTE_KEY);
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), 0.0d);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_withSpread() {
    IborIborSwapCurveNode test = IborIborSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_requirements() {
    IborIborSwapCurveNode test = IborIborSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    Set<ObservableKey> set = test.requirements();
    Iterator<ObservableKey> itr = set.iterator();
    assertEquals(itr.next(), QUOTE_KEY);
    assertFalse(itr.hasNext());
  }

  public void test_trade() {
    IborIborSwapCurveNode node = IborIborSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate tradeDate = LocalDate.of(2015, 1, 22);
    double rate = 0.125;
    MarketData marketData = MarketData.builder().addValue(QUOTE_KEY, rate).build();
    SwapTrade trade = node.trade(tradeDate, marketData);
    SwapTrade expected = TEMPLATE.toTrade(tradeDate, BUY, 1, rate + SPREAD);
    assertEquals(trade, expected);
  }

  public void test_trade_differentKey() {
    IborIborSwapCurveNode node = IborIborSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    double rate = 0.035;
    QuoteKey key = QuoteKey.of(StandardId.of("OG-Ticker", "Deposit2"));
    MarketData marketData = MarketData.builder().addValue(key, rate).build();
    assertThrowsIllegalArg(() -> node.trade(valuationDate, marketData));
  }

  public void test_initialGuess() {
    IborIborSwapCurveNode node = IborIborSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    double rate = 0.035;
    MarketData marketData = MarketData.builder().addValue(QUOTE_KEY, rate).build();
    assertEquals(node.initialGuess(valuationDate, marketData, ValueType.ZERO_RATE), 0d);
    assertEquals(node.initialGuess(valuationDate, marketData, ValueType.DISCOUNT_FACTOR), 1.0d);
  }

  public void test_metadata() {
    IborIborSwapCurveNode node = IborIborSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    CurveParameterMetadata metadata = node.metadata(valuationDate);
    // 2015-01-22 is Thursday, start is 2015-01-26, but 2025-01-26 is Sunday, so end is 2025-01-27
    assertEquals(((TenorCurveNodeMetadata) metadata).getDate(), LocalDate.of(2025, 1, 27));
    assertEquals(((TenorCurveNodeMetadata) metadata).getTenor(), Tenor.TENOR_10Y);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborIborSwapCurveNode test = IborIborSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    coverImmutableBean(test);
    IborIborSwapCurveNode test2 = IborIborSwapCurveNode.of(TEMPLATE2, QUOTE_KEY2, 0.1);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborIborSwapCurveNode test = IborIborSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    assertSerialization(test);
  }

}
