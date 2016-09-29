/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.node;

import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.SELL;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveNodeDateOrder;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.TenorDateParameterMetadata;
import com.opengamma.strata.product.credit.CdsQuote;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.credit.CdsTradeForCalibration;
import com.opengamma.strata.product.credit.ResolvedCdsTrade;
import com.opengamma.strata.product.credit.type.CdsConventions;
import com.opengamma.strata.product.credit.type.CdsQuoteConvention;
import com.opengamma.strata.product.credit.type.CdsTemplate;

/**
 * Test {@code CdsCurveNode}.
 */
@Test
public class CdsCurveNodeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final CdsTemplate TEMPLATE = CdsTemplate.of(TENOR_10Y, CdsConventions.USD_STANDARD);
  private static final QuoteId QUOTE_ID = QuoteId.of(StandardId.of("OG-Ticker", "Cds1"));
  private static final String LABEL = "Label";
  private static final String LABEL_AUTO = "10Y";
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG", "ABC");

  public void test_builder() {
    CdsCurveNode test = CdsCurveNode.builder()
        .label(LABEL)
        .template(TEMPLATE)
        .observableId(QUOTE_ID)
        .quoteConvention(CdsQuoteConvention.PAR_SPREAD)
        .dateOrder(CurveNodeDateOrder.DEFAULT)
        .legalEntityId(LEGAL_ENTITY)
        .endDate(date(2025, 6, 20))
        .build();
    assertEquals(test.getLabel(), LABEL);
    assertEquals(test.getObservableId(), QUOTE_ID);
    assertEquals(test.getTemplate(), TEMPLATE);
    assertEquals(test.date(VAL_DATE, REF_DATA), date(2025, 6, 20));
    assertEquals(test.getEndDate().get(), date(2025, 6, 20));
  }

  public void test_of_noRate() {
    CdsCurveNode test = CdsCurveNode.of(TEMPLATE, QUOTE_ID, LEGAL_ENTITY, CdsQuoteConvention.QUOTED_SPREAD, 0.01);
    assertEquals(test.getLabel(), LABEL_AUTO);
    assertEquals(test.getObservableId(), QUOTE_ID);
    assertEquals(test.getTemplate(), TEMPLATE);
    assertEquals(test.getEndDate(), Optional.empty());
    assertEquals(test.date(VAL_DATE, REF_DATA), date(2025, 6, 20));
  }

  public void test_of_withRate() {
    CdsCurveNode test = CdsCurveNode.of(TEMPLATE, QUOTE_ID, LEGAL_ENTITY, CdsQuoteConvention.POINTS_UPFRONT, 0.01);
    assertEquals(test.getLabel(), LABEL_AUTO);
    assertEquals(test.getObservableId(), QUOTE_ID);
    assertEquals(test.getTemplate(), TEMPLATE);
    assertEquals(test.getEndDate(), Optional.empty());
    assertEquals(test.date(VAL_DATE, REF_DATA), date(2025, 6, 20));
  }

  public void test_of_fail() {
    assertThrows(
        () -> CdsCurveNode.of(TEMPLATE, QUOTE_ID, LEGAL_ENTITY, CdsQuoteConvention.QUOTED_SPREAD),
        IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_requirements() {
    CdsCurveNode test = CdsCurveNode.of(TEMPLATE, QUOTE_ID, LEGAL_ENTITY, CdsQuoteConvention.QUOTED_SPREAD, 0.01);
    Set<ObservableId> set = test.requirements();
    Iterator<ObservableId> itr = set.iterator();
    assertEquals(itr.next(), QUOTE_ID);
    assertFalse(itr.hasNext());
  }

  //-------------------------------------------------------------------------
  public void test_trade() {
    CdsCurveNode node = CdsCurveNode.of(TEMPLATE, QUOTE_ID, LEGAL_ENTITY, CdsQuoteConvention.QUOTED_SPREAD, 0.01);
    double rate = 0.0125;
    double quantity = -1234.56;
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_ID, rate).build();
    CdsTradeForCalibration trade = node.trade(quantity, marketData, REF_DATA);
    CdsTrade expected = TEMPLATE.createTrade(LEGAL_ENTITY, VAL_DATE, SELL, -quantity, 0.01, REF_DATA);
    assertEquals(trade.getTrade(), expected);
    assertEquals(trade.getQuote(), CdsQuote.of(CdsQuoteConvention.QUOTED_SPREAD, rate));

    CdsCurveNode node1 = CdsCurveNode.of(TEMPLATE, QUOTE_ID, LEGAL_ENTITY, CdsQuoteConvention.PAR_SPREAD);
    CdsTrade expected1 = TEMPLATE.createTrade(LEGAL_ENTITY, VAL_DATE, SELL, -quantity, rate, REF_DATA);
    ResolvedCdsTrade resolved = node1.resolvedTrade(quantity, marketData, REF_DATA);
    assertEquals(resolved, expected1.resolve(REF_DATA));
  }

  public void test_trade_noMarketData() {
    CdsCurveNode node = CdsCurveNode.of(TEMPLATE, QUOTE_ID, LEGAL_ENTITY, CdsQuoteConvention.PAR_SPREAD);
    MarketData marketData = MarketData.empty(VAL_DATE);
    assertThrows(() -> node.trade(1d, marketData, REF_DATA), MarketDataNotFoundException.class);
  }

  //-------------------------------------------------------------------------
  public void test_initialGuess() {
    CdsCurveNode node = CdsCurveNode.of(TEMPLATE, QUOTE_ID, LEGAL_ENTITY, CdsQuoteConvention.PAR_SPREAD);
    double rate = 0.0125;
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_ID, rate).build();
    assertThrows(() -> node.initialGuess(marketData, ValueType.ZERO_RATE), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_metadata() {
    CdsCurveNode node = CdsCurveNode.of(TEMPLATE, QUOTE_ID, LEGAL_ENTITY, CdsQuoteConvention.QUOTED_SPREAD, 0.01);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    ParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    assertEquals(((TenorDateParameterMetadata) metadata).getDate(), LocalDate.of(2024, 12, 20));
    assertEquals(((TenorDateParameterMetadata) metadata).getTenor(), Tenor.TENOR_10Y);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CdsCurveNode test1 = CdsCurveNode.of(TEMPLATE, QUOTE_ID, LEGAL_ENTITY, CdsQuoteConvention.QUOTED_SPREAD, 0.01);
    coverImmutableBean(test1);
    CdsCurveNode test2 = CdsCurveNode.of(
        CdsTemplate.of(TENOR_10Y, CdsConventions.EUR_GB_STANDARD),
        QuoteId.of(StandardId.of("OG-Ticker", "Cds2")),
        StandardId.of("OG", "DEF"),
        CdsQuoteConvention.POINTS_UPFRONT,
        0.01);
    QuoteId.of(StandardId.of("OG-Ticker", "Deposit2"));
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    CdsCurveNode test = CdsCurveNode.of(TEMPLATE, QUOTE_ID, LEGAL_ENTITY, CdsQuoteConvention.QUOTED_SPREAD, 0.01);
    assertSerialization(test);
  }

}
