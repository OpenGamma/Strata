/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.node;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.Period;
import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
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
import com.opengamma.strata.product.deposit.ResolvedTermDepositTrade;
import com.opengamma.strata.product.deposit.TermDeposit;
import com.opengamma.strata.product.deposit.TermDepositTrade;
import com.opengamma.strata.product.deposit.type.TermDepositConvention;
import com.opengamma.strata.product.deposit.type.TermDepositConventions;
import com.opengamma.strata.product.deposit.type.TermDepositTemplate;

/**
 * Test {@link TermDepositCurveNode}.
 */
public class TermDepositCurveNodeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, EUTA);
  private static final TermDepositConvention CONVENTION = TermDepositConventions.EUR_DEPOSIT_T2;
  private static final Period DEPOSIT_PERIOD = Period.ofMonths(3);
  private static final TermDepositTemplate TEMPLATE = TermDepositTemplate.of(DEPOSIT_PERIOD, CONVENTION);
  private static final QuoteId QUOTE_ID = QuoteId.of(StandardId.of("OG-Ticker", "Deposit1"));
  private static final double SPREAD = 0.0015;
  private static final String LABEL = "Label";
  private static final String LABEL_AUTO = "3M";

  @Test
  public void test_builder() {
    TermDepositCurveNode test = TermDepositCurveNode.builder()
        .label(LABEL)
        .template(TEMPLATE)
        .rateId(QUOTE_ID)
        .additionalSpread(SPREAD)
        .date(CurveNodeDate.LAST_FIXING)
        .build();
    assertThat(test.getLabel()).isEqualTo(LABEL);
    assertThat(test.getRateId()).isEqualTo(QUOTE_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(SPREAD);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
    assertThat(test.getDate()).isEqualTo(CurveNodeDate.LAST_FIXING);
  }

  @Test
  public void test_builder_defaults() {
    TermDepositCurveNode test = TermDepositCurveNode.builder()
        .label(LABEL)
        .template(TEMPLATE)
        .rateId(QUOTE_ID)
        .additionalSpread(SPREAD)
        .build();
    assertThat(test.getLabel()).isEqualTo(LABEL);
    assertThat(test.getRateId()).isEqualTo(QUOTE_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(SPREAD);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
    assertThat(test.getDate()).isEqualTo(CurveNodeDate.END);
  }

  @Test
  public void test_of_noSpread() {
    TermDepositCurveNode test = TermDepositCurveNode.of(TEMPLATE, QUOTE_ID);
    assertThat(test.getLabel()).isEqualTo(LABEL_AUTO);
    assertThat(test.getRateId()).isEqualTo(QUOTE_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(0.0d);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
  }

  @Test
  public void test_of_withSpread() {
    TermDepositCurveNode test = TermDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    assertThat(test.getLabel()).isEqualTo(LABEL_AUTO);
    assertThat(test.getRateId()).isEqualTo(QUOTE_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(SPREAD);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
  }

  @Test
  public void test_of_withSpreadAndLabel() {
    TermDepositCurveNode test = TermDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD, LABEL);
    assertThat(test.getLabel()).isEqualTo(LABEL);
    assertThat(test.getRateId()).isEqualTo(QUOTE_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(SPREAD);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
  }

  @Test
  public void test_requirements() {
    TermDepositCurveNode test = TermDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    Set<ObservableId> set = test.requirements();
    Iterator<ObservableId> itr = set.iterator();
    assertThat(itr.next()).isEqualTo(QUOTE_ID);
    assertThat(itr.hasNext()).isFalse();
  }

  @Test
  public void test_trade() {
    TermDepositCurveNode node = TermDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    double rate = 0.035;
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_ID, rate).build();
    TermDepositTrade trade = node.trade(1d, marketData, REF_DATA);
    LocalDate startDateExpected = PLUS_TWO_DAYS.adjust(VAL_DATE, REF_DATA);
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
        .tradeDate(VAL_DATE)
        .build();
    assertThat(trade.getProduct()).isEqualTo(depositExpected);
    assertThat(trade.getInfo()).isEqualTo(tradeInfoExpected);
  }

  @Test
  public void test_trade_noMarketData() {
    TermDepositCurveNode node = TermDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    MarketData marketData = MarketData.empty(valuationDate);
    assertThatExceptionOfType(MarketDataNotFoundException.class)
        .isThrownBy(() -> node.trade(1d, marketData, REF_DATA));
  }

  @Test
  public void test_sampleResolvedTrade() {
    TermDepositCurveNode node = TermDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    ResolvedTermDepositTrade trade = node.sampleResolvedTrade(valuationDate, FxRateProvider.minimal(), REF_DATA);
    ResolvedTermDepositTrade expected = TEMPLATE.createTrade(valuationDate, BuySell.BUY, 1d, SPREAD, REF_DATA).resolve(REF_DATA);
    assertThat(trade).isEqualTo(expected);
  }

  @Test
  public void test_initialGuess() {
    TermDepositCurveNode node = TermDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    double rate = 0.035;
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_ID, rate).build();
    assertThat(node.initialGuess(marketData, ValueType.ZERO_RATE)).isEqualTo(rate);
    assertThat(node.initialGuess(marketData, ValueType.FORWARD_RATE)).isEqualTo(rate);
    assertThat(node.initialGuess(marketData, ValueType.DISCOUNT_FACTOR)).isCloseTo(Math.exp(-rate * 0.25), offset(1.0e-12));
  }

  @Test
  public void test_metadata_end() {
    TermDepositCurveNode node = TermDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    ParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    assertThat(((TenorDateParameterMetadata) metadata).getDate()).isEqualTo(LocalDate.of(2015, 4, 27));
    assertThat(((TenorDateParameterMetadata) metadata).getTenor()).isEqualTo(Tenor.TENOR_3M);
  }

  @Test
  public void test_metadata_fixed() {
    LocalDate nodeDate = VAL_DATE.plusMonths(1);
    TermDepositCurveNode node =
        TermDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD).withDate(CurveNodeDate.of(nodeDate));
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    DatedParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    assertThat(metadata.getDate()).isEqualTo(nodeDate);
    assertThat(metadata.getLabel()).isEqualTo(node.getLabel());
  }

  @Test
  public void test_metadata_last_fixing() {
    TermDepositCurveNode node =
        TermDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD).withDate(CurveNodeDate.LAST_FIXING);
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> node.metadata(VAL_DATE, REF_DATA));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    TermDepositCurveNode test = TermDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    coverImmutableBean(test);
    TermDepositCurveNode test2 = TermDepositCurveNode.of(
        TermDepositTemplate.of(Period.ofMonths(1), CONVENTION), QuoteId.of(StandardId.of("OG-Ticker", "Deposit2")));
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    TermDepositCurveNode test = TermDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    assertSerialization(test);
  }

}
