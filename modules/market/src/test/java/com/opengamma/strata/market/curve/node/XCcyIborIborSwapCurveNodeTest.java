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
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.Period;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.market.FxRateKey;
import com.opengamma.strata.basics.market.ImmutableMarketData;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.basics.market.SimpleMarketDataKey;
import com.opengamma.strata.basics.market.StandardId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveParameterMetadata;
import com.opengamma.strata.market.curve.DatedCurveParameterMetadata;
import com.opengamma.strata.market.curve.meta.TenorDateCurveNodeMetadata;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swap.type.XCcyIborIborSwapConventions;
import com.opengamma.strata.product.swap.type.XCcyIborIborSwapTemplate;

/**
 * Test {@link XCcyIborIborSwapCurveNode}.
 */
@Test
public class XCcyIborIborSwapCurveNodeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2015, 6, 30);
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
  private static final String LABEL = "Label";
  private static final String LABEL_AUTO = "10Y";
  private static final MarketData OV = ImmutableMarketData.builder(VAL_DATE)
      .addValue(SPREAD_KEY, SPREAD_XCS)
      .addValue(FX_KEY, FX_EUR_USD)
      .build();

  public void test_builder() {
    XCcyIborIborSwapCurveNode test = XCcyIborIborSwapCurveNode.builder()
        .label(LABEL)
        .template(TEMPLATE)
        .spreadKey(SPREAD_KEY)
        .additionalSpread(SPREAD_ADJ)
        .build();
    assertEquals(test.getLabel(), LABEL);
    assertEquals(test.getSpreadKey(), SPREAD_KEY);
    assertEquals(test.getAdditionalSpread(), SPREAD_ADJ);
    assertEquals(test.getTemplate(), TEMPLATE);
    assertEquals(test.getDate(), CurveNodeDate.END);
  }

  public void test_of_noSpread() {
    XCcyIborIborSwapCurveNode test = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_KEY);
    assertEquals(test.getLabel(), LABEL_AUTO);
    assertEquals(test.getSpreadKey(), SPREAD_KEY);
    assertEquals(test.getAdditionalSpread(), 0.0d);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_withSpread() {
    XCcyIborIborSwapCurveNode test = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_KEY, SPREAD_ADJ);
    assertEquals(test.getLabel(), LABEL_AUTO);
    assertEquals(test.getSpreadKey(), SPREAD_KEY);
    assertEquals(test.getAdditionalSpread(), SPREAD_ADJ);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_withSpreadAndLabel() {
    XCcyIborIborSwapCurveNode test = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_KEY, SPREAD_ADJ, LABEL);
    assertEquals(test.getLabel(), LABEL);
    assertEquals(test.getSpreadKey(), SPREAD_KEY);
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
    SwapTrade trade = node.trade(tradeDate, OV, REF_DATA);
    double rate = FX_EUR_USD.fxRate(Currency.EUR, Currency.USD);
    SwapTrade expected = TEMPLATE.createTrade(tradeDate, BUY, 1, rate, SPREAD_XCS + SPREAD_ADJ, REF_DATA);
    assertEquals(trade, expected);
  }

  public void test_trade_differentKey() {
    XCcyIborIborSwapCurveNode node = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_KEY, SPREAD_ADJ);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    double rate = 0.035;
    QuoteKey key = QuoteKey.of(StandardId.of("OG-Ticker", "Deposit2"));
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(key, rate).build();
    assertThrowsIllegalArg(() -> node.trade(valuationDate, marketData, REF_DATA));
  }

  public void test_initialGuess() {
    XCcyIborIborSwapCurveNode node = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_KEY, SPREAD_ADJ);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    assertEquals(node.initialGuess(valuationDate, OV, ValueType.ZERO_RATE), 0d);
    assertEquals(node.initialGuess(valuationDate, OV, ValueType.DISCOUNT_FACTOR), 1.0d);
  }

  public void test_metadata_end() {
    XCcyIborIborSwapCurveNode node = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_KEY, SPREAD_ADJ);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    CurveParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    // 2015-01-22 is Thursday, start is 2015-01-26, but 2025-01-26 is Sunday, so end is 2025-01-27
    assertEquals(((TenorDateCurveNodeMetadata) metadata).getDate(), LocalDate.of(2025, 1, 27));
    assertEquals(((TenorDateCurveNodeMetadata) metadata).getTenor(), Tenor.TENOR_10Y);
  }

  public void test_metadata_fixed() {
    LocalDate nodeDate = VAL_DATE.plusMonths(1);
    XCcyIborIborSwapCurveNode node =
        XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_KEY, SPREAD_ADJ, LABEL).withDate(CurveNodeDate.of(nodeDate));
    DatedCurveParameterMetadata metadata = node.metadata(VAL_DATE, REF_DATA);
    assertEquals(metadata.getDate(), nodeDate);
    assertEquals(metadata.getLabel(), node.getLabel());
  }

  public void test_metadata_last_fixing() {
    XCcyIborIborSwapCurveNode node =
        XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_KEY, SPREAD_ADJ, LABEL).withDate(CurveNodeDate.LAST_FIXING);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    DatedCurveParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    LocalDate fixingExpected = LocalDate.of(2024, 10, 24);
    assertEquals(metadata.getDate(), fixingExpected);
    assertEquals(((TenorDateCurveNodeMetadata) metadata).getTenor(), TENOR_10Y);
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
