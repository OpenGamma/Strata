/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.node;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;

import static com.opengamma.strata.basics.index.IborIndices.EUR_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.Set;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
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
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.deposit.IborFixingDeposit;
import com.opengamma.strata.product.deposit.IborFixingDepositTrade;
import com.opengamma.strata.product.deposit.ResolvedIborFixingDeposit;
import com.opengamma.strata.product.deposit.type.IborFixingDepositTemplate;
import com.opengamma.strata.product.deposit.type.ImmutableIborFixingDepositConvention;
import com.opengamma.strata.product.rate.IborRateObservation;

/**
 * Test {@link IborFixingDepositCurveNode}.
 */
@Test
public class IborFixingDepositCurveNodeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final QuoteKey QUOTE_KEY = QuoteKey.of(StandardId.of("OG-Ticker", "Deposit1"));
  private static final double SPREAD = 0.0015;
  private static final String LABEL = "Label";
  private static final String LABEL_AUTO = "3M";
  private static final IborFixingDepositTemplate TEMPLATE = IborFixingDepositTemplate.of(EUR_LIBOR_3M);

  public void test_builder() {
    IborFixingDepositCurveNode test = IborFixingDepositCurveNode.builder()
        .label(LABEL)
        .rateKey(QUOTE_KEY)
        .template(TEMPLATE)
        .additionalSpread(SPREAD)
        .build();
    assertEquals(test.getLabel(), LABEL);
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
    assertEquals(test.getDate(), CurveNodeDate.END);
  }

  public void test_of_noSpread() {
    IborFixingDepositCurveNode test = IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_KEY);
    assertEquals(test.getLabel(), LABEL_AUTO);
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), 0.0);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_withSpread() {
    IborFixingDepositCurveNode test = IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    assertEquals(test.getLabel(), LABEL_AUTO);
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_withSpreadAndLabel() {
    IborFixingDepositCurveNode test = IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD, LABEL);
    assertEquals(test.getLabel(), LABEL);
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_requirements() {
    IborFixingDepositCurveNode test = IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    Set<ObservableKey> set = test.requirements();
    Iterator<ObservableKey> itr = set.iterator();
    assertEquals(itr.next(), QUOTE_KEY);
    assertFalse(itr.hasNext());
  }

  public void test_trade() {
    IborFixingDepositCurveNode node = IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    double rate = 0.035;
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_KEY, rate).build();
    IborFixingDepositTrade trade = node.trade(valuationDate, marketData, REF_DATA);
    ImmutableIborFixingDepositConvention conv = (ImmutableIborFixingDepositConvention) TEMPLATE.getConvention();
    LocalDate startDateExpected = conv.getSpotDateOffset().adjust(valuationDate, REF_DATA);
    LocalDate endDateExpected = startDateExpected.plus(TEMPLATE.getDepositPeriod());
    IborFixingDeposit depositExpected = IborFixingDeposit.builder()
        .buySell(BuySell.BUY)
        .index(EUR_LIBOR_3M)
        .startDate(startDateExpected)
        .endDate(endDateExpected)
        .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUR_LIBOR_3M.getFixingCalendar()))
        .notional(1.0d)
        .fixedRate(rate + SPREAD)
        .build();
    TradeInfo tradeInfoExpected = TradeInfo.builder()
        .tradeDate(valuationDate)
        .build();
    assertEquals(trade.getProduct(), depositExpected);
    assertEquals(trade.getInfo(), tradeInfoExpected);
  }

  public void test_trade_differentKey() {
    IborFixingDepositCurveNode node = IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    double rate = 0.035;
    QuoteKey key = QuoteKey.of(StandardId.of("OG-Ticker", "Deposit2"));
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(key, rate).build();
    assertThrowsIllegalArg(() -> node.trade(valuationDate, marketData, REF_DATA));
  }

  public void test_initialGuess() {
    IborFixingDepositCurveNode node = IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    double rate = 0.035;
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_KEY, rate).build();
    assertEquals(node.initialGuess(valuationDate, marketData, ValueType.ZERO_RATE), rate);
    assertEquals(node.initialGuess(valuationDate, marketData, ValueType.FORWARD_RATE), rate);
    assertEquals(node.initialGuess(valuationDate, marketData, ValueType.DISCOUNT_FACTOR),
        Math.exp(-rate * 0.25d), 1.0E-12);
  }

  public void test_metadata_end() {
    IborFixingDepositCurveNode node = IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    CurveParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    assertEquals(((TenorDateCurveNodeMetadata) metadata).getDate(), LocalDate.of(2015, 4, 27));
    assertEquals(((TenorDateCurveNodeMetadata) metadata).getTenor(), Tenor.TENOR_3M);
  }

  public void test_metadata_fixed() {
    LocalDate nodeDate = VAL_DATE.plusMonths(1);
    IborFixingDepositCurveNode node =
        IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD).withDate(CurveNodeDate.of(nodeDate));
    DatedCurveParameterMetadata metadata = node.metadata(VAL_DATE, REF_DATA);
    assertEquals(metadata.getDate(), nodeDate);
    assertEquals(metadata.getLabel(), node.getLabel());
  }

  public void test_metadata_last_fixing() {
    IborFixingDepositCurveNode node =
        IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD).withDate(CurveNodeDate.LAST_FIXING);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    ImmutableMarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_KEY, 0.0d).build();
    IborFixingDepositTrade trade = node.trade(valuationDate, marketData, REF_DATA);
    ResolvedIborFixingDeposit product = trade.getProduct().resolve(REF_DATA);
    LocalDate fixingDate = ((IborRateObservation) product.getFloatingRate()).getFixingDate();
    DatedCurveParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    assertEquals(((TenorDateCurveNodeMetadata) metadata).getDate(), fixingDate);
    assertEquals(((TenorDateCurveNodeMetadata) metadata).getTenor().getPeriod(), TEMPLATE.getDepositPeriod());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborFixingDepositCurveNode test = IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    coverImmutableBean(test);
    IborFixingDepositCurveNode test2 = IborFixingDepositCurveNode.of(
        IborFixingDepositTemplate.of(GBP_LIBOR_6M), QuoteKey.of(StandardId.of("OG-Ticker", "Deposit2")));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborFixingDepositCurveNode test = IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    assertSerialization(test);
  }

}
