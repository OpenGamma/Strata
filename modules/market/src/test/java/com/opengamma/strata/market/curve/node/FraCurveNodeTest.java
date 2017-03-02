/*
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

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveNodeDate;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.DatedParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.TenorDateParameterMetadata;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.fra.ResolvedFra;
import com.opengamma.strata.product.fra.type.FraTemplate;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test {@link FraCurveNode}.
 */
@Test
public class FraCurveNodeTest {

  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  private static final DaysAdjustment OFFSET = DaysAdjustment.ofBusinessDays(0, GBLO);
  private static final Period PERIOD_TO_START = Period.ofMonths(2);
  private static final Period PERIOD_TO_END = Period.ofMonths(5);
  private static final FraTemplate TEMPLATE = FraTemplate.of(PERIOD_TO_START, GBP_LIBOR_3M);
  private static final QuoteId QUOTE_ID = QuoteId.of(StandardId.of("OG-Ticker", "Deposit1"));
  private static final double SPREAD = 0.0015;
  private static final String LABEL = "Label";
  private static final String LABEL_AUTO = "5M";
  private static final ReferenceData REF_DATA = ReferenceData.standard();

  public void test_builder() {
    FraCurveNode test = FraCurveNode.builder()
        .label(LABEL)
        .template(TEMPLATE)
        .rateId(QUOTE_ID)
        .additionalSpread(SPREAD)
        .date(CurveNodeDate.LAST_FIXING)
        .build();
    assertEquals(test.getLabel(), LABEL);
    assertEquals(test.getRateId(), QUOTE_ID);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
    assertEquals(test.getDate(), CurveNodeDate.LAST_FIXING);
  }

  public void test_builder_defaults() {
    FraCurveNode test = FraCurveNode.builder()
        .label(LABEL)
        .template(TEMPLATE)
        .rateId(QUOTE_ID)
        .additionalSpread(SPREAD)
        .build();
    assertEquals(test.getLabel(), LABEL);
    assertEquals(test.getRateId(), QUOTE_ID);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
    assertEquals(test.getDate(), CurveNodeDate.END);
  }

  public void test_of_noSpread() {
    FraCurveNode test = FraCurveNode.of(TEMPLATE, QUOTE_ID);
    assertEquals(test.getLabel(), LABEL_AUTO);
    assertEquals(test.getRateId(), QUOTE_ID);
    assertEquals(test.getAdditionalSpread(), 0.0d);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_withSpread() {
    FraCurveNode test = FraCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    assertEquals(test.getLabel(), LABEL_AUTO);
    assertEquals(test.getRateId(), QUOTE_ID);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_withSpreadAndLabel() {
    FraCurveNode test = FraCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD, LABEL);
    assertEquals(test.getLabel(), LABEL);
    assertEquals(test.getRateId(), QUOTE_ID);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_requirements() {
    FraCurveNode test = FraCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    Set<ObservableId> set = test.requirements();
    Iterator<ObservableId> itr = set.iterator();
    assertEquals(itr.next(), QUOTE_ID);
    assertFalse(itr.hasNext());
  }

  public void test_trade() {
    FraCurveNode node = FraCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    double rate = 0.035;
    ImmutableMarketData marketData = ImmutableMarketData.builder(valuationDate).addValue(QUOTE_ID, rate).build();
    FraTrade trade = node.trade(1d, marketData, REF_DATA);
    LocalDate startDateExpected =
        BDA_MOD_FOLLOW.adjust(OFFSET.adjust(valuationDate, REF_DATA).plus(PERIOD_TO_START), REF_DATA);
    LocalDate endDateExpected =
        BDA_MOD_FOLLOW.adjust(OFFSET.adjust(valuationDate, REF_DATA).plus(PERIOD_TO_END), REF_DATA);
    Fra productExpected = Fra.builder()
        .buySell(BuySell.SELL)
        .currency(GBP)
        .dayCount(ACT_365F)
        .startDate(startDateExpected)
        .endDate(endDateExpected)
        .paymentDate(AdjustableDate.of(startDateExpected))
        .notional(1.0d)
        .index(GBP_LIBOR_3M)
        .fixedRate(rate + SPREAD)
        .build();
    TradeInfo tradeInfoExpected = TradeInfo.builder()
        .tradeDate(valuationDate)
        .build();
    assertEquals(trade.getProduct(), productExpected);
    assertEquals(trade.getInfo(), tradeInfoExpected);
  }

  public void test_trade_noMarketData() {
    FraCurveNode node = FraCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    MarketData marketData = MarketData.empty(valuationDate);
    assertThrows(() -> node.trade(1d, marketData, REF_DATA), MarketDataNotFoundException.class);
  }

  public void test_initialGuess() {
    FraCurveNode node = FraCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    double rate = 0.035;
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_ID, rate).build();
    assertEquals(node.initialGuess(marketData, ValueType.ZERO_RATE), rate);
    assertEquals(node.initialGuess(marketData, ValueType.FORWARD_RATE), rate);
    double approximateMaturity = TEMPLATE.getPeriodToEnd().toTotalMonths() / 12.0d;
    double df = Math.exp(-approximateMaturity * rate);
    assertEquals(node.initialGuess(marketData, ValueType.DISCOUNT_FACTOR), df);
    assertEquals(node.initialGuess(marketData, ValueType.PRICE_INDEX), 0d);
  }

  public void test_metadata_end() {
    FraCurveNode node = FraCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    LocalDate endDate = OFFSET.adjust(valuationDate, REF_DATA).plus(PERIOD_TO_START).plusMonths(3);
    ParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    assertEquals(((TenorDateParameterMetadata) metadata).getDate(), endDate);
    assertEquals(((TenorDateParameterMetadata) metadata).getTenor(), TENOR_5M);
  }

  public void test_metadata_fixed() {
    LocalDate nodeDate = VAL_DATE.plusMonths(1);
    FraCurveNode node = FraCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD).withDate(CurveNodeDate.of(nodeDate));
    DatedParameterMetadata metadata = node.metadata(VAL_DATE, REF_DATA);
    assertEquals(metadata.getDate(), nodeDate);
    assertEquals(metadata.getLabel(), node.getLabel());
  }

  public void test_metadata_last_fixing() {
    FraCurveNode node = FraCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD).withDate(CurveNodeDate.LAST_FIXING);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    ImmutableMarketData marketData = ImmutableMarketData.builder(valuationDate).addValue(QUOTE_ID, 0.0d).build();
    FraTrade trade = node.trade(1d, marketData, REF_DATA);
    ResolvedFra resolved = trade.getProduct().resolve(REF_DATA);
    LocalDate fixingDate = ((IborRateComputation) (resolved.getFloatingRate())).getFixingDate();
    DatedParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    assertEquals(((TenorDateParameterMetadata) metadata).getDate(), fixingDate);
    assertEquals(((TenorDateParameterMetadata) metadata).getTenor(), TENOR_5M);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FraCurveNode test = FraCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    coverImmutableBean(test);
    FraCurveNode test2 = FraCurveNode.of(
        FraTemplate.of(Period.ofMonths(1), GBP_LIBOR_6M), QuoteId.of(StandardId.of("OG-Ticker", "Deposit2")));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FraCurveNode test = FraCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    assertSerialization(test);
  }

}
