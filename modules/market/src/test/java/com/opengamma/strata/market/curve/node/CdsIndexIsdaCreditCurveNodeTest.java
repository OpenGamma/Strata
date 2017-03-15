/*
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

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.LabelDateParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.TenorDateParameterMetadata;
import com.opengamma.strata.product.credit.CdsIndex;
import com.opengamma.strata.product.credit.CdsIndexCalibrationTrade;
import com.opengamma.strata.product.credit.CdsIndexTrade;
import com.opengamma.strata.product.credit.CdsQuote;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.credit.type.CdsConventions;
import com.opengamma.strata.product.credit.type.CdsQuoteConvention;
import com.opengamma.strata.product.credit.type.CdsTemplate;
import com.opengamma.strata.product.credit.type.DatesCdsTemplate;
import com.opengamma.strata.product.credit.type.TenorCdsTemplate;

/**
 * Test {@link CdsIndexIsdaCreditCurveNode}.
 */
@Test
public class CdsIndexIsdaCreditCurveNodeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final CdsTemplate TEMPLATE = TenorCdsTemplate.of(TENOR_10Y, CdsConventions.USD_STANDARD);
  private static final LocalDate START_DATE = LocalDate.of(2015, 5, 20);
  private static final LocalDate END_DATE = LocalDate.of(2020, 10, 20);
  private static final CdsTemplate TEMPLATE_NS = DatesCdsTemplate.of(START_DATE, END_DATE, CdsConventions.EUR_GB_STANDARD);
  private static final QuoteId QUOTE_ID = QuoteId.of(StandardId.of("OG-Ticker", "Cds1"));
  private static final String LABEL = "Label";
  private static final String LABEL_AUTO = "10Y";
  private static final StandardId INDEX_ID = StandardId.of("OG", "ABCXX");
  private static final ImmutableList<StandardId> LEGAL_ENTITIES = ImmutableList.of(StandardId.of("OG", "ABC1"),
      StandardId.of("OG", "ABC2"), StandardId.of("OG", "ABC3"), StandardId.of("OG", "ABC4"));

  public void test_builder() {
    CdsIndexIsdaCreditCurveNode test = CdsIndexIsdaCreditCurveNode.builder()
        .label(LABEL)
        .template(TEMPLATE)
        .observableId(QUOTE_ID)
        .quoteConvention(CdsQuoteConvention.PAR_SPREAD)
        .cdsIndexId(INDEX_ID)
        .legalEntityIds(LEGAL_ENTITIES)
        .build();
    assertEquals(test.getLabel(), LABEL);
    assertEquals(test.getCdsIndexId(), INDEX_ID);
    assertEquals(test.getLegalEntityIds(), LEGAL_ENTITIES);
    assertEquals(test.getObservableId(), QUOTE_ID);
    assertEquals(test.getTemplate(), TEMPLATE);
    assertEquals(test.date(VAL_DATE, REF_DATA), date(2025, 6, 20));
  }

  public void test_of_quotedSpread() {
    CdsIndexIsdaCreditCurveNode test =
        CdsIndexIsdaCreditCurveNode.ofQuotedSpread(TEMPLATE, QUOTE_ID, INDEX_ID, LEGAL_ENTITIES, 0.01);
    assertEquals(test.getLabel(), LABEL_AUTO);
    assertEquals(test.getCdsIndexId(), INDEX_ID);
    assertEquals(test.getLegalEntityIds(), LEGAL_ENTITIES);
    assertEquals(test.getObservableId(), QUOTE_ID);
    assertEquals(test.getTemplate(), TEMPLATE);
    assertEquals(test.date(VAL_DATE, REF_DATA), date(2025, 6, 20));
  }

  public void test_of_pardSpread() {
    CdsIndexIsdaCreditCurveNode test = CdsIndexIsdaCreditCurveNode.ofParSpread(TEMPLATE_NS, QUOTE_ID, INDEX_ID, LEGAL_ENTITIES);
    assertEquals(test.getLabel(), END_DATE.toString());
    assertEquals(test.getCdsIndexId(), INDEX_ID);
    assertEquals(test.getLegalEntityIds(), LEGAL_ENTITIES);
    assertEquals(test.getObservableId(), QUOTE_ID);
    assertEquals(test.getTemplate(), TEMPLATE_NS);
    assertEquals(test.date(VAL_DATE, REF_DATA), END_DATE);
  }

  public void test_of_pointsUpfront() {
    CdsIndexIsdaCreditCurveNode test =
        CdsIndexIsdaCreditCurveNode.ofPointsUpfront(TEMPLATE, QUOTE_ID, INDEX_ID, LEGAL_ENTITIES, 0.01);
    assertEquals(test.getLabel(), LABEL_AUTO);
    assertEquals(test.getCdsIndexId(), INDEX_ID);
    assertEquals(test.getLegalEntityIds(), LEGAL_ENTITIES);
    assertEquals(test.getObservableId(), QUOTE_ID);
    assertEquals(test.getTemplate(), TEMPLATE);
    assertEquals(test.date(VAL_DATE, REF_DATA), date(2025, 6, 20));
  }

  public void test_build_fail_noRate() {
    assertThrows(
        () -> CdsIndexIsdaCreditCurveNode.builder().template(TEMPLATE).observableId(QUOTE_ID).cdsIndexId(INDEX_ID)
            .legalEntityIds(LEGAL_ENTITIES).quoteConvention(CdsQuoteConvention.QUOTED_SPREAD).build(),
        IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_trade() {
    CdsIndexIsdaCreditCurveNode node =
        CdsIndexIsdaCreditCurveNode.ofQuotedSpread(TEMPLATE, QUOTE_ID, INDEX_ID, LEGAL_ENTITIES, 0.01);
    double rate = 0.0125;
    double quantity = -1234.56;
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_ID, rate).build();
    CdsIndexCalibrationTrade trade = node.trade(quantity, marketData, REF_DATA);
    CdsTrade cdsTrade = TEMPLATE.createTrade(INDEX_ID, VAL_DATE, SELL, -quantity, 0.01, REF_DATA);
    CdsIndex cdsIndex = CdsIndex.of(
        SELL, INDEX_ID, LEGAL_ENTITIES, TEMPLATE.getConvention().getCurrency(), -quantity, date(2015, 6, 20),
        date(2025, 6, 20), Frequency.P3M, TEMPLATE.getConvention().getSettlementDateOffset().getCalendar(), 0.01);
    CdsIndex cdsIndexMod = cdsIndex.toBuilder()
        .paymentSchedule(
            cdsIndex.getPaymentSchedule().toBuilder()
                .rollConvention(RollConventions.DAY_20)
                .startDateBusinessDayAdjustment(cdsIndex.getPaymentSchedule().getBusinessDayAdjustment())
                .build())
        .build();
    CdsIndexTrade expected = CdsIndexTrade.builder()
        .product(cdsIndexMod)
        .info(cdsTrade.getInfo())
        .build();
    assertEquals(trade.getUnderlyingTrade(), expected);
    assertEquals(trade.getQuote(), CdsQuote.of(CdsQuoteConvention.QUOTED_SPREAD, rate));

    CdsIndexIsdaCreditCurveNode node1 = CdsIndexIsdaCreditCurveNode.ofParSpread(TEMPLATE, QUOTE_ID, INDEX_ID, LEGAL_ENTITIES);
    CdsTrade cdsTrade1 = TEMPLATE.createTrade(INDEX_ID, VAL_DATE, SELL, -quantity, rate, REF_DATA);
    CdsIndexCalibrationTrade trade1 = node1.trade(quantity, marketData, REF_DATA);
    CdsIndex cdsIndex1 = CdsIndex.of(
        SELL, INDEX_ID, LEGAL_ENTITIES, TEMPLATE.getConvention().getCurrency(), -quantity, date(2015, 6, 20),
        date(2025, 6, 20), Frequency.P3M, TEMPLATE.getConvention().getSettlementDateOffset().getCalendar(), rate);
    CdsIndex cdsIndexMod1 = cdsIndex1.toBuilder()
        .paymentSchedule(
            cdsIndex.getPaymentSchedule().toBuilder()
                .rollConvention(RollConventions.DAY_20)
                .startDateBusinessDayAdjustment(cdsIndex1.getPaymentSchedule().getBusinessDayAdjustment())
                .build())
        .build();
    CdsIndexTrade expected1 = CdsIndexTrade.builder()
        .product(cdsIndexMod1)
        .info(cdsTrade1.getInfo())
        .build();
    assertEquals(trade1.getUnderlyingTrade(), expected1);
    assertEquals(trade1.getQuote(), CdsQuote.of(CdsQuoteConvention.PAR_SPREAD, rate));
  }

  public void test_trade_noMarketData() {
    CdsIndexIsdaCreditCurveNode node = CdsIndexIsdaCreditCurveNode.ofParSpread(TEMPLATE, QUOTE_ID, INDEX_ID, LEGAL_ENTITIES);
    MarketData marketData = MarketData.empty(VAL_DATE);
    assertThrows(() -> node.trade(1d, marketData, REF_DATA), MarketDataNotFoundException.class);
  }

  //-------------------------------------------------------------------------
  public void test_metadata_tenor() {
    CdsIndexIsdaCreditCurveNode node =
        CdsIndexIsdaCreditCurveNode.ofQuotedSpread(TEMPLATE, QUOTE_ID, INDEX_ID, LEGAL_ENTITIES, 0.01);
    LocalDate nodeDate = LocalDate.of(2015, 1, 22);
    ParameterMetadata metadata = node.metadata(nodeDate);
    assertEquals(((TenorDateParameterMetadata) metadata).getDate(), nodeDate);
    assertEquals(((TenorDateParameterMetadata) metadata).getTenor(), Tenor.TENOR_10Y);
  }

  public void test_metadata_dates() {
    CdsIndexIsdaCreditCurveNode node = CdsIndexIsdaCreditCurveNode.ofParSpread(TEMPLATE_NS, QUOTE_ID, INDEX_ID, LEGAL_ENTITIES);
    ParameterMetadata metadata = node.metadata(END_DATE);
    assertEquals(((LabelDateParameterMetadata) metadata).getDate(), END_DATE);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CdsIndexIsdaCreditCurveNode test1 =
        CdsIndexIsdaCreditCurveNode.ofQuotedSpread(TEMPLATE, QUOTE_ID, INDEX_ID, LEGAL_ENTITIES, 0.01);
    coverImmutableBean(test1);
    CdsIndexIsdaCreditCurveNode test2 = CdsIndexIsdaCreditCurveNode.ofPointsUpfront(
        TenorCdsTemplate.of(TENOR_10Y, CdsConventions.EUR_GB_STANDARD),
        QuoteId.of(StandardId.of("OG-Ticker", "Cdx2")),
        StandardId.of("OG", "DEF"),
        ImmutableList.of(StandardId.of("OG", "DEF1"), StandardId.of("OG", "DEF2")),
        0.01);
    QuoteId.of(StandardId.of("OG-Ticker", "Deposit2"));
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    CdsIndexIsdaCreditCurveNode test =
        CdsIndexIsdaCreditCurveNode.ofQuotedSpread(TEMPLATE, QUOTE_ID, INDEX_ID, LEGAL_ENTITIES, 0.01);
    assertSerialization(test);
  }

}
