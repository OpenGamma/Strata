/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.node;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.index.IborIndices.EUR_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
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
import com.opengamma.strata.product.deposit.IborFixingDeposit;
import com.opengamma.strata.product.deposit.IborFixingDepositTrade;
import com.opengamma.strata.product.deposit.ResolvedIborFixingDeposit;
import com.opengamma.strata.product.deposit.type.IborFixingDepositTemplate;
import com.opengamma.strata.product.deposit.type.ImmutableIborFixingDepositConvention;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test {@link IborFixingDepositCurveNode}.
 */
public class IborFixingDepositCurveNodeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final QuoteId QUOTE_ID = QuoteId.of(StandardId.of("OG-Ticker", "Deposit1"));
  private static final double SPREAD = 0.0015;
  private static final String LABEL = "Label";
  private static final String LABEL_AUTO = "3M";
  private static final IborFixingDepositTemplate TEMPLATE = IborFixingDepositTemplate.of(EUR_LIBOR_3M);

  @Test
  public void test_builder() {
    IborFixingDepositCurveNode test = IborFixingDepositCurveNode.builder()
        .label(LABEL)
        .rateId(QUOTE_ID)
        .template(TEMPLATE)
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
    IborFixingDepositCurveNode test = IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_ID);
    assertThat(test.getLabel()).isEqualTo(LABEL_AUTO);
    assertThat(test.getRateId()).isEqualTo(QUOTE_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(0.0);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
  }

  @Test
  public void test_of_withSpread() {
    IborFixingDepositCurveNode test = IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    assertThat(test.getLabel()).isEqualTo(LABEL_AUTO);
    assertThat(test.getRateId()).isEqualTo(QUOTE_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(SPREAD);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
  }

  @Test
  public void test_of_withSpreadAndLabel() {
    IborFixingDepositCurveNode test = IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD, LABEL);
    assertThat(test.getLabel()).isEqualTo(LABEL);
    assertThat(test.getRateId()).isEqualTo(QUOTE_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(SPREAD);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
  }

  @Test
  public void test_requirements() {
    IborFixingDepositCurveNode test = IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    Set<ObservableId> set = test.requirements();
    Iterator<ObservableId> itr = set.iterator();
    assertThat(itr.next()).isEqualTo(QUOTE_ID);
    assertThat(itr.hasNext()).isFalse();
  }

  @Test
  public void test_trade() {
    IborFixingDepositCurveNode node = IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    double rate = 0.035;
    MarketData marketData = ImmutableMarketData.builder(valuationDate).addValue(QUOTE_ID, rate).build();
    IborFixingDepositTrade trade = node.trade(1d, marketData, REF_DATA);
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
    assertThat(trade.getProduct()).isEqualTo(depositExpected);
    assertThat(trade.getInfo()).isEqualTo(tradeInfoExpected);
  }

  @Test
  public void test_trade_noMarketData() {
    IborFixingDepositCurveNode node = IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    MarketData marketData = MarketData.empty(VAL_DATE);
    assertThatExceptionOfType(MarketDataNotFoundException.class)
        .isThrownBy(() -> node.trade(1d, marketData, REF_DATA));
  }

  @Test
  public void test_initialGuess() {
    IborFixingDepositCurveNode node = IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    double rate = 0.035;
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_ID, rate).build();
    assertThat(node.initialGuess(marketData, ValueType.ZERO_RATE)).isEqualTo(rate);
    assertThat(node.initialGuess(marketData, ValueType.FORWARD_RATE)).isEqualTo(rate);
    assertThat(node.initialGuess(marketData, ValueType.DISCOUNT_FACTOR)).isCloseTo(Math.exp(-rate * 0.25d), offset(1.0E-12));
  }

  @Test
  public void test_metadata_end() {
    IborFixingDepositCurveNode node = IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    ParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    assertThat(((TenorDateParameterMetadata) metadata).getDate()).isEqualTo(LocalDate.of(2015, 4, 27));
    assertThat(((TenorDateParameterMetadata) metadata).getTenor()).isEqualTo(Tenor.TENOR_3M);
  }

  @Test
  public void test_metadata_fixed() {
    LocalDate nodeDate = VAL_DATE.plusMonths(1);
    IborFixingDepositCurveNode node =
        IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD).withDate(CurveNodeDate.of(nodeDate));
    DatedParameterMetadata metadata = node.metadata(VAL_DATE, REF_DATA);
    assertThat(metadata.getDate()).isEqualTo(nodeDate);
    assertThat(metadata.getLabel()).isEqualTo(node.getLabel());
  }

  @Test
  public void test_metadata_last_fixing() {
    IborFixingDepositCurveNode node =
        IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD).withDate(CurveNodeDate.LAST_FIXING);
    ImmutableMarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_ID, 0.0d).build();
    IborFixingDepositTrade trade = node.trade(1d, marketData, REF_DATA);
    ResolvedIborFixingDeposit product = trade.getProduct().resolve(REF_DATA);
    LocalDate fixingDate = ((IborRateComputation) product.getFloatingRate()).getFixingDate();
    DatedParameterMetadata metadata = node.metadata(VAL_DATE, REF_DATA);
    assertThat(((TenorDateParameterMetadata) metadata).getDate()).isEqualTo(fixingDate);
    assertThat(((TenorDateParameterMetadata) metadata).getTenor().getPeriod()).isEqualTo(TEMPLATE.getDepositPeriod());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    IborFixingDepositCurveNode test = IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    coverImmutableBean(test);
    IborFixingDepositCurveNode test2 = IborFixingDepositCurveNode.of(
        IborFixingDepositTemplate.of(GBP_LIBOR_6M), QuoteId.of(StandardId.of("OG-Ticker", "Deposit2")));
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    IborFixingDepositCurveNode test = IborFixingDepositCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    assertSerialization(test);
  }

}
