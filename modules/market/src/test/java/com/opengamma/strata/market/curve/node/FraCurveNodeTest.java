/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.node;

import static com.opengamma.strata.basics.currency.Currency.GBP;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.Tenor.TENOR_5M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
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

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
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
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.fra.ResolvedFra;
import com.opengamma.strata.product.fra.type.FraTemplate;
import com.opengamma.strata.product.rate.IborRateObservation;

/**
 * Test {@link FraCurveNode}.
 */
@Test
public class FraCurveNodeTest {

  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  private static final DaysAdjustment OFFSET = DaysAdjustment.ofBusinessDays(0, GBLO);
  private static final Period PERIOD_TO_START = Period.ofMonths(2);
  private static final FraTemplate TEMPLATE = FraTemplate.of(PERIOD_TO_START, GBP_LIBOR_3M);
  private static final QuoteKey QUOTE_KEY = QuoteKey.of(StandardId.of("OG-Ticker", "Deposit1"));
  private static final double SPREAD = 0.0015;
  private static final String LABEL = "Label";
  private static final String LABEL_AUTO = "5M";
  private static final ReferenceData REF_DATA = ReferenceData.standard();

  public void test_builder() {
    FraCurveNode test = FraCurveNode.builder()
        .label(LABEL)
        .template(TEMPLATE)
        .rateKey(QUOTE_KEY)
        .additionalSpread(SPREAD)
        .date(CurveNodeDate.LAST_FIXING)
        .build();
    assertEquals(test.getLabel(), LABEL);
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
    assertEquals(test.getDate(), CurveNodeDate.LAST_FIXING);
  }

  public void test_builder_defaults() {
    FraCurveNode test = FraCurveNode.builder()
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
    FraCurveNode test = FraCurveNode.of(TEMPLATE, QUOTE_KEY);
    assertEquals(test.getLabel(), LABEL_AUTO);
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), 0.0d);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_withSpread() {
    FraCurveNode test = FraCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    assertEquals(test.getLabel(), LABEL_AUTO);
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_withSpreadAndLabel() {
    FraCurveNode test = FraCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD, LABEL);
    assertEquals(test.getLabel(), LABEL);
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_requirements() {
    FraCurveNode test = FraCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    Set<ObservableKey> set = test.requirements();
    Iterator<ObservableKey> itr = set.iterator();
    assertEquals(itr.next(), QUOTE_KEY);
    assertFalse(itr.hasNext());
  }

  public void test_trade() {
    FraCurveNode node = FraCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    double rate = 0.035;
    ImmutableMarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_KEY, rate).build();
    FraTrade trade = node.trade(valuationDate, marketData, REF_DATA);
    LocalDate startDateExpected = OFFSET.adjust(valuationDate, REF_DATA).plus(PERIOD_TO_START);
    LocalDate endDateExpected = startDateExpected.plusMonths(3);
    Fra productExpected = Fra.builder()
        .buySell(BuySell.BUY)
        .currency(GBP)
        .dayCount(ACT_365F)
        .startDate(startDateExpected)
        .endDate(endDateExpected)
        .notional(1.0d)
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .index(GBP_LIBOR_3M)
        .fixedRate(rate + SPREAD)
        .build();
    TradeInfo tradeInfoExpected = TradeInfo.builder()
        .tradeDate(valuationDate)
        .build();
    assertEquals(trade.getProduct(), productExpected);
    assertEquals(trade.getInfo(), tradeInfoExpected);
  }

  public void test_trade_differentKey() {
    FraCurveNode node = FraCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    double rate = 0.035;
    QuoteKey key = QuoteKey.of(StandardId.of("OG-Ticker", "Deposit2"));
    ImmutableMarketData md = ImmutableMarketData.builder(VAL_DATE).addValue(key, rate).build();
    assertThrowsIllegalArg(() -> node.trade(valuationDate, md, REF_DATA));
  }

  public void test_initialGuess() {
    FraCurveNode node = FraCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    double rate = 0.035;
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_KEY, rate).build();
    assertEquals(node.initialGuess(valuationDate, marketData, ValueType.ZERO_RATE), rate);
    assertEquals(node.initialGuess(valuationDate, marketData, ValueType.FORWARD_RATE), rate);
    double approximateMaturity = TEMPLATE.getPeriodToEnd().toTotalMonths() / 12.0d;
    double df = Math.exp(-approximateMaturity * rate);
    assertEquals(node.initialGuess(valuationDate, marketData, ValueType.DISCOUNT_FACTOR), df);
    assertEquals(node.initialGuess(valuationDate, marketData, ValueType.PRICE_INDEX), 0d);
  }

  public void test_metadata_end() {
    FraCurveNode node = FraCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    LocalDate endDate = OFFSET.adjust(valuationDate, REF_DATA).plus(PERIOD_TO_START).plusMonths(3);
    CurveParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    assertEquals(((TenorDateCurveNodeMetadata) metadata).getDate(), endDate);
    assertEquals(((TenorDateCurveNodeMetadata) metadata).getTenor(), TENOR_5M);
  }

  public void test_metadata_fixed() {
    LocalDate nodeDate = VAL_DATE.plusMonths(1);
    FraCurveNode node = FraCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD).withDate(CurveNodeDate.of(nodeDate));
    DatedCurveParameterMetadata metadata = node.metadata(VAL_DATE, REF_DATA);
    assertEquals(metadata.getDate(), nodeDate);
    assertEquals(metadata.getLabel(), node.getLabel());
  }

  public void test_metadata_last_fixing() {
    FraCurveNode node = FraCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD).withDate(CurveNodeDate.LAST_FIXING);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    ImmutableMarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_KEY, 0.0d).build();
    FraTrade trade = node.trade(valuationDate, marketData, REF_DATA);
    ResolvedFra resolved = trade.getProduct().resolve(REF_DATA);
    LocalDate fixingDate = ((IborRateObservation) (resolved.getFloatingRate())).getFixingDate();
    DatedCurveParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    assertEquals(((TenorDateCurveNodeMetadata) metadata).getDate(), fixingDate);
    assertEquals(((TenorDateCurveNodeMetadata) metadata).getTenor(), TENOR_5M);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FraCurveNode test = FraCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    coverImmutableBean(test);
    FraCurveNode test2 = FraCurveNode.of(
        FraTemplate.of(Period.ofMonths(1), GBP_LIBOR_6M), QuoteKey.of(StandardId.of("OG-Ticker", "Deposit2")));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FraCurveNode test = FraCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    assertSerialization(test);
  }

}
