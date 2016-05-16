/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.node;

import static com.opengamma.strata.basics.BuySell.BUY;
import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.time.LocalDate;
import java.time.Period;
import java.util.Iterator;
import java.util.Set;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.market.ImmutableMarketData;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.MarketDataNotFoundException;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.basics.market.StandardId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveParameterMetadata;
import com.opengamma.strata.market.curve.DatedCurveParameterMetadata;
import com.opengamma.strata.market.curve.meta.TenorDateCurveNodeMetadata;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swap.type.ThreeLegBasisSwapConventions;
import com.opengamma.strata.product.swap.type.ThreeLegBasisSwapTemplate;

/**
 * Test {@link ThreeLegBasisSwapCurveNode}.
 */
@Test
public class ThreeLegBasisSwapCurveNodeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final ThreeLegBasisSwapTemplate TEMPLATE = ThreeLegBasisSwapTemplate.of(
      Period.ZERO, TENOR_10Y, ThreeLegBasisSwapConventions.EUR_FIXED_1Y_EURIBOR_3M_EURIBOR_6M);
  private static final ThreeLegBasisSwapTemplate TEMPLATE2 = ThreeLegBasisSwapTemplate.of(
      Period.ofMonths(1), TENOR_10Y, ThreeLegBasisSwapConventions.EUR_FIXED_1Y_EURIBOR_3M_EURIBOR_6M);
  private static final QuoteKey QUOTE_KEY = QuoteKey.of(StandardId.of("OG-Ticker", "EUR-BS36-10Y"));
  private static final QuoteKey QUOTE_KEY2 = QuoteKey.of(StandardId.of("OG-Ticker", "Test"));
  private static final double SPREAD = 0.0015;
  private static final String LABEL = "Label";
  private static final String LABEL_AUTO = "10Y";

  public void test_builder() {
    ThreeLegBasisSwapCurveNode test = ThreeLegBasisSwapCurveNode.builder()
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
    ThreeLegBasisSwapCurveNode test = ThreeLegBasisSwapCurveNode.of(TEMPLATE, QUOTE_KEY);
    assertEquals(test.getLabel(), LABEL_AUTO);
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), 0.0d);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_withSpread() {
    ThreeLegBasisSwapCurveNode test = ThreeLegBasisSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    assertEquals(test.getLabel(), LABEL_AUTO);
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_withSpreadAndLabel() {
    ThreeLegBasisSwapCurveNode test = ThreeLegBasisSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD, LABEL);
    assertEquals(test.getLabel(), LABEL);
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_requirements() {
    ThreeLegBasisSwapCurveNode test = ThreeLegBasisSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    Set<ObservableKey> set = test.requirements();
    Iterator<ObservableKey> itr = set.iterator();
    assertEquals(itr.next(), QUOTE_KEY);
    assertFalse(itr.hasNext());
  }

  public void test_trade() {
    ThreeLegBasisSwapCurveNode node = ThreeLegBasisSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate tradeDate = LocalDate.of(2015, 1, 22);
    double rate = 0.125;
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_KEY, rate).build();
    SwapTrade trade = node.trade(tradeDate, marketData, REF_DATA);
    SwapTrade expected = TEMPLATE.createTrade(tradeDate, BUY, 1, rate + SPREAD, REF_DATA);
    assertEquals(trade, expected);
  }

  public void test_trade_noMarketData() {
    ThreeLegBasisSwapCurveNode node = ThreeLegBasisSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    MarketData marketData = MarketData.empty(valuationDate);
    assertThrows(() -> node.trade(valuationDate, marketData, REF_DATA), MarketDataNotFoundException.class);
  }

  public void test_initialGuess() {
    ThreeLegBasisSwapCurveNode node = ThreeLegBasisSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    double rate = 0.035;
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_KEY, rate).build();
    assertEquals(node.initialGuess(valuationDate, marketData, ValueType.ZERO_RATE), 0d);
    assertEquals(node.initialGuess(valuationDate, marketData, ValueType.DISCOUNT_FACTOR), 1.0d);
  }

  public void test_metadata_end() {
    ThreeLegBasisSwapCurveNode node = ThreeLegBasisSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    CurveParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    assertEquals(((TenorDateCurveNodeMetadata) metadata).getDate(), LocalDate.of(2025, 1, 27));
    assertEquals(((TenorDateCurveNodeMetadata) metadata).getTenor(), Tenor.TENOR_10Y);
  }

  public void test_metadata_fixed() {
    LocalDate nodeDate = VAL_DATE.plusMonths(1);
    ThreeLegBasisSwapCurveNode node =
        ThreeLegBasisSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD, LABEL).withDate(CurveNodeDate.of(nodeDate));
    DatedCurveParameterMetadata metadata = node.metadata(VAL_DATE, REF_DATA);
    assertEquals(metadata.getDate(), nodeDate);
    assertEquals(metadata.getLabel(), node.getLabel());
  }

  public void test_metadata_last_fixing() {
    ThreeLegBasisSwapCurveNode node =
        ThreeLegBasisSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD, LABEL).withDate(CurveNodeDate.LAST_FIXING);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    LocalDate fixingExpected = LocalDate.of(2024, 7, 24);
    DatedCurveParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    assertEquals(metadata.getDate(), fixingExpected);
    assertEquals(metadata.getLabel(), node.getLabel());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ThreeLegBasisSwapCurveNode test = ThreeLegBasisSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    coverImmutableBean(test);
    ThreeLegBasisSwapCurveNode test2 = ThreeLegBasisSwapCurveNode.of(TEMPLATE2, QUOTE_KEY2, 0.1);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ThreeLegBasisSwapCurveNode test = ThreeLegBasisSwapCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    assertSerialization(test);
  }

}
