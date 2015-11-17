/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.definition;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendars.EUTA;
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

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.market.curve.CurveParameterMetadata;
import com.opengamma.strata.market.curve.TenorCurveNodeMetadata;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.market.value.ValueType;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.rate.deposit.TermDeposit;
import com.opengamma.strata.product.rate.deposit.TermDepositTrade;
import com.opengamma.strata.product.rate.deposit.type.TermDepositConvention;
import com.opengamma.strata.product.rate.deposit.type.TermDepositConventions;
import com.opengamma.strata.product.rate.deposit.type.TermDepositTemplate;

/**
 * Test {@link TermDepositCurveNode}.
 */
@Test
public class TermDepositCurveNodeTest {

  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, EUTA);
  private static final TermDepositConvention CONVENTION = TermDepositConventions.EUR_DEPOSIT;
  private static final Period DEPOSIT_PERIOD = Period.ofMonths(3);
  private static final TermDepositTemplate TEMPLATE = TermDepositTemplate.of(DEPOSIT_PERIOD, CONVENTION);
  private static final QuoteKey QUOTE_KEY = QuoteKey.of(StandardId.of("OG-Ticker", "Deposit1"));
  private static final double SPREAD = 0.0015;

  public void test_builder() {
    TermDepositCurveNode test = TermDepositCurveNode.builder()
        .template(TEMPLATE)
        .rateKey(QUOTE_KEY)
        .additionalSpread(SPREAD)
        .build();
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_noSpread() {
    TermDepositCurveNode test = TermDepositCurveNode.of(TEMPLATE, QUOTE_KEY);
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), 0.0d);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_withSpread() {
    TermDepositCurveNode test = TermDepositCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    assertEquals(test.getRateKey(), QUOTE_KEY);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);

  }

  public void test_requirements() {
    TermDepositCurveNode test = TermDepositCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    Set<ObservableKey> set = test.requirements();
    Iterator<ObservableKey> itr = set.iterator();
    assertEquals(itr.next(), QUOTE_KEY);
    assertFalse(itr.hasNext());
  }

  public void test_trade() {
    TermDepositCurveNode node = TermDepositCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    double rate = 0.035;
    MarketData marketData = MarketData.builder().addValue(QUOTE_KEY, rate).build();
    TermDepositTrade trade = node.trade(valuationDate, marketData);
    LocalDate startDateExpected = PLUS_TWO_DAYS.adjust(valuationDate);
    LocalDate endDateExpected = startDateExpected.plus(DEPOSIT_PERIOD);
    TermDeposit depositExpected = TermDeposit.builder()
        .buySell(BuySell.BUY)
        .currency(EUR)
        .dayCount(ACT_360)
        .startDate(startDateExpected)
        .endDate(endDateExpected)
        .notional(1.0d)
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .rate(rate + SPREAD)
        .build();
    TradeInfo tradeInfoExpected = TradeInfo.builder()
        .tradeDate(valuationDate)
        .build();
    assertEquals(trade.getProduct(), depositExpected);
    assertEquals(trade.getTradeInfo(), tradeInfoExpected);
  }

  public void test_trade_differentKey() {
    TermDepositCurveNode node = TermDepositCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    double rate = 0.035;
    QuoteKey key = QuoteKey.of(StandardId.of("OG-Ticker", "Deposit2"));
    MarketData marketData = MarketData.builder().addValue(key, rate).build();
    assertThrowsIllegalArg(() -> node.trade(valuationDate, marketData));
  }

  public void test_initialGuess() {
    TermDepositCurveNode node = TermDepositCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    double rate = 0.035;
    MarketData marketData = MarketData.builder().addValue(QUOTE_KEY, rate).build();
    assertEquals(node.initialGuess(valuationDate, marketData, ValueType.ZERO_RATE), rate);
    assertEquals(node.initialGuess(valuationDate, marketData, ValueType.DISCOUNT_FACTOR), 0d);
  }

  public void test_metadata() {
    TermDepositCurveNode node = TermDepositCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    CurveParameterMetadata metadata = node.metadata(valuationDate);
    assertEquals(((TenorCurveNodeMetadata) metadata).getDate(), LocalDate.of(2015, 4, 27));
    assertEquals(((TenorCurveNodeMetadata) metadata).getTenor(), Tenor.TENOR_3M);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    TermDepositCurveNode test = TermDepositCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    coverImmutableBean(test);
    TermDepositCurveNode test2 = TermDepositCurveNode.of(
        TermDepositTemplate.of(Period.ofMonths(1), CONVENTION), QuoteKey.of(StandardId.of("OG-Ticker", "Deposit2")));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    TermDepositCurveNode test = TermDepositCurveNode.of(TEMPLATE, QUOTE_KEY, SPREAD);
    assertSerialization(test);
  }

}
