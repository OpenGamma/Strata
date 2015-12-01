/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.node;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.Iterator;
import java.util.Set;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveParameterMetadata;
import com.opengamma.strata.market.curve.meta.YearMonthCurveNodeMetadata;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.product.index.IborFutureTrade;
import com.opengamma.strata.product.index.type.IborFutureConvention;
import com.opengamma.strata.product.index.type.IborFutureConventions;
import com.opengamma.strata.product.index.type.IborFutureTemplate;

/**
 * Tests {@link IborFutureCurveNode}.
 */
@Test
public class IborFutureCurveNodeTest {

  private static final IborFutureConvention CONVENTION = IborFutureConventions.USD_LIBOR_3M_QUARTERLY_IMM;
  private static final Period PERIOD_TO_START = Period.ofMonths(2);
  private static final int NUMBER = 2;
  private static final IborFutureTemplate TEMPLATE = IborFutureTemplate.of(PERIOD_TO_START, NUMBER, CONVENTION);
  private static final QuoteKey QUOTE_KEY = QuoteKey.of(StandardId.of("OG-Ticker", "OG-EDH6"));
  private static final double SPREAD = 0.0001;
  private static final String LABEL = "Label";

  private static final double TOLERANCE_RATE = 1.0E-8;

  public void test_builder() {
    IborFutureCurveNode test = IborFutureCurveNode.builder()
        .label(LABEL)
        .template(TEMPLATE)
        .rateKey(QUOTE_KEY)
        .additionalSpread(SPREAD)
        .build();
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_no_spread() {
    IborFutureCurveNode test = IborFutureCurveNode.of(TEMPLATE, QUOTE_KEY);
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), 0.0d);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_withSpread() {
    IborFutureCurveNode test = IborFutureCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_withSpreadAndLabel() {
    IborFutureCurveNode test = IborFutureCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD, LABEL);
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_requirements() {
    IborFutureCurveNode test = IborFutureCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    Set<ObservableKey> set = test.requirements();
    Iterator<ObservableKey> itr = set.iterator();
    assertEquals(itr.next(), QUOTE_KEY);
    assertFalse(itr.hasNext());
  }

  public void test_trade() {
    IborFutureCurveNode node = IborFutureCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate date = LocalDate.of(2015, 10, 20);
    double price = 0.99;
    MarketData marketData = MarketData.builder().addValue(QUOTE_KEY, price).build();
    IborFutureTrade trade = node.trade(date, marketData);
    IborFutureTrade expected = TEMPLATE.toTrade(date, 1L, 1.0, price + SPREAD);
    assertEquals(trade, expected);
  }

  public void test_trade_differentKey() {
    IborFutureCurveNode node = IborFutureCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate date = LocalDate.of(2015, 10, 20);
    double price = 0.99;
    QuoteKey key = QuoteKey.of(StandardId.of("OG-Ticker", "Unknown"));
    MarketData marketData = MarketData.builder().addValue(key, price).build();
    assertThrowsIllegalArg(() -> node.trade(date, marketData));
  }

  public void test_initialGuess() {
    IborFutureCurveNode node = IborFutureCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate date = LocalDate.of(2015, 10, 20);
    double price = 0.99;
    MarketData marketData = MarketData.builder().addValue(QUOTE_KEY, price).build();
    assertEquals(node.initialGuess(date, marketData, ValueType.ZERO_RATE), 1.0 - price, TOLERANCE_RATE);
    double approximateMaturity =
        TEMPLATE.getMinimumPeriod().plus(TEMPLATE.getConvention().getIndex().getTenor()).toTotalMonths() / 12.0d;
    double df = Math.exp(-approximateMaturity * (1.0 - price));
    assertEquals(node.initialGuess(date, marketData, ValueType.DISCOUNT_FACTOR), df, TOLERANCE_RATE);
    assertEquals(node.initialGuess(date, marketData, ValueType.UNKNOWN), 0.0d, TOLERANCE_RATE);
  }

  public void test_metadata() {
    IborFutureCurveNode node = IborFutureCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD, LABEL);
    LocalDate date = LocalDate.of(2015, 10, 20);
    LocalDate referenceDate = TEMPLATE.referenceDate(date);
    LocalDate maturityDate = TEMPLATE.getConvention().getIndex().calculateMaturityFromEffective(referenceDate);
    CurveParameterMetadata metadata = node.metadata(date);
    assertEquals(metadata.getLabel(), LABEL);
    assertTrue(metadata instanceof YearMonthCurveNodeMetadata);
    assertEquals(((YearMonthCurveNodeMetadata) metadata).getDate(), maturityDate);
    assertEquals(((YearMonthCurveNodeMetadata) metadata).getYearMonth(), YearMonth.from(referenceDate));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborFutureCurveNode test = IborFutureCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    coverImmutableBean(test);
    IborFutureCurveNode test2 = IborFutureCurveNode.of(
        IborFutureTemplate.of(PERIOD_TO_START, NUMBER, CONVENTION), QuoteKey.of(StandardId.of("OG-Ticker", "Unknown")));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborFutureCurveNode test = IborFutureCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    assertSerialization(test);
  }

}
