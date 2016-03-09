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
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Set;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.market.ImmutableMarketData;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.basics.market.StandardId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveParameterMetadata;
import com.opengamma.strata.market.curve.DatedCurveParameterMetadata;
import com.opengamma.strata.market.curve.meta.TenorDateCurveNodeMetadata;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.product.swap.type.FixedIborSwapTemplate;

/**
 * Test {@link FixedIborSwapCurveNode}.
 */
@Test
public class FixedIborSwapCurveNodeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2015, 6, 30);

  private static final FixedIborSwapTemplate TEMPLATE =
      FixedIborSwapTemplate.of(TENOR_10Y, FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M);
  private static final QuoteKey QUOTE_KEY = QuoteKey.of(StandardId.of("OG-Ticker", "Deposit1"));
  private static final double SPREAD = 0.0015;
  private static final String LABEL = "Label";
  private static final String LABEL_AUTO = "10Y";

  private static final double TOLERANCE_DF = 1.0E-10;

  public void test_builder() {
    FixedIborSwapCurveNode test = FixedIborSwapCurveNode.builder()
        .label(LABEL)
        .template(TEMPLATE)
        .rateKey(QUOTE_KEY)
        .additionalSpread(SPREAD)
        .build();
    assertEquals(test.getLabel(), LABEL);
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
    assertEquals(test.getDate(), CurveNodeDate.END);
  }

  public void test_of_noSpread() {
    FixedIborSwapCurveNode test = FixedIborSwapCurveNode.of(TEMPLATE, QUOTE_KEY);
    assertEquals(test.getLabel(), LABEL_AUTO);
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), 0.0d);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_withSpread() {
    FixedIborSwapCurveNode test = FixedIborSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    assertEquals(test.getLabel(), LABEL_AUTO);
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_withSpreadAndLabel() {
    FixedIborSwapCurveNode test = FixedIborSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD, LABEL);
    assertEquals(test.getLabel(), LABEL);
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_requirements() {
    FixedIborSwapCurveNode test = FixedIborSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    Set<ObservableKey> set = test.requirements();
    Iterator<ObservableKey> itr = set.iterator();
    assertEquals(itr.next(), QUOTE_KEY);
    assertFalse(itr.hasNext());
  }

  public void test_trade() {
    FixedIborSwapCurveNode node = FixedIborSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate tradeDate = LocalDate.of(2015, 1, 22);
    double rate = 0.125;
    MarketData marketData = ImmutableMarketData.builder(tradeDate).addValue(QUOTE_KEY, rate).build();
    SwapTrade trade = node.trade(tradeDate, marketData, REF_DATA);
    SwapTrade expected = TEMPLATE.createTrade(tradeDate, BUY, 1, rate + SPREAD, REF_DATA);
    assertEquals(trade, expected);
  }

  public void test_trade_differentKey() {
    FixedIborSwapCurveNode node = FixedIborSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    double rate = 0.035;
    QuoteKey key = QuoteKey.of(StandardId.of("OG-Ticker", "Deposit2"));
    MarketData marketData = ImmutableMarketData.builder(valuationDate).addValue(key, rate).build();
    assertThrowsIllegalArg(() -> node.trade(valuationDate, marketData, REF_DATA));
  }

  public void test_initialGuess() {
    FixedIborSwapCurveNode node = FixedIborSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    double rate = 0.035;
    MarketData marketData = ImmutableMarketData.builder(valuationDate).addValue(QUOTE_KEY, rate).build();
    assertEquals(node.initialGuess(valuationDate, marketData, ValueType.ZERO_RATE), rate);
    assertEquals(node.initialGuess(valuationDate, marketData, ValueType.FORWARD_RATE), rate);
    double df = Math.exp(-TENOR_10Y.get(ChronoUnit.YEARS) * rate);
    assertEquals(node.initialGuess(valuationDate, marketData, ValueType.DISCOUNT_FACTOR), df, TOLERANCE_DF);
    assertEquals(node.initialGuess(valuationDate, marketData, ValueType.PRICE_INDEX), 0d);
  }

  public void test_metadata_end() {
    FixedIborSwapCurveNode node = FixedIborSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    CurveParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    // 2015-01-22 is Thursday, start is 2015-01-26, but 2025-01-26 is Sunday, so end is 2025-01-27
    assertEquals(((TenorDateCurveNodeMetadata) metadata).getDate(), LocalDate.of(2025, 1, 27));
    assertEquals(((TenorDateCurveNodeMetadata) metadata).getTenor(), Tenor.TENOR_10Y);
  }

  public void test_metadata_fixed() {
    FixedIborSwapCurveNode node =
        FixedIborSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD, LABEL).withDate(CurveNodeDate.of(VAL_DATE));
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    DatedCurveParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    assertEquals(metadata.getDate(), VAL_DATE);
    assertEquals(metadata.getLabel(), node.getLabel());
  }

  public void test_metadata_last_fixing() {
    FixedIborSwapCurveNode node =
        FixedIborSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD, LABEL).withDate(CurveNodeDate.LAST_FIXING);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    LocalDate fixingExpected = LocalDate.of(2024, 10, 24);
    DatedCurveParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    assertEquals(metadata.getDate(), fixingExpected);
    assertEquals(metadata.getLabel(), node.getLabel());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FixedIborSwapCurveNode test = FixedIborSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    coverImmutableBean(test);
    FixedIborSwapCurveNode test2 = FixedIborSwapCurveNode.of(
        FixedIborSwapTemplate.of(TENOR_10Y, FixedIborSwapConventions.USD_FIXED_1Y_LIBOR_3M),
        QuoteKey.of(StandardId.of("OG-Ticker", "Deposit2")));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FixedIborSwapCurveNode test = FixedIborSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    assertSerialization(test);
  }

}
