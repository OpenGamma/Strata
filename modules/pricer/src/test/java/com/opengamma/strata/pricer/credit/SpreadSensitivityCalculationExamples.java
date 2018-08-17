/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.credit.type.CdsConventions.USD_STANDARD;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.ImmutableMarketDataBuilder;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.IsdaCreditCurveDefinition;
import com.opengamma.strata.market.curve.node.CdsIndexIsdaCreditCurveNode;
import com.opengamma.strata.market.curve.node.CdsIsdaCreditCurveNode;
import com.opengamma.strata.market.observable.LegalEntityInformation;
import com.opengamma.strata.market.observable.LegalEntityInformationId;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.pricer.common.PriceType;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.credit.Cds;
import com.opengamma.strata.product.credit.CdsIndex;
import com.opengamma.strata.product.credit.CdsIndexTrade;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.credit.ResolvedCdsIndexTrade;
import com.opengamma.strata.product.credit.ResolvedCdsTrade;
import com.opengamma.strata.product.credit.type.TenorCdsTemplate;

/**
 * Spread sensitivity calculation with relative shift.
 */
@Test
public class SpreadSensitivityCalculationExamples {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double ONE_BP = 1.0e-4;
  private static final double ONE_PC = 1.0e-2;

  private static final IsdaCompliantCreditCurveCalibrator BUILDER = FastCreditCurveCalibrator.standard();
  private static final IsdaCompliantIndexCurveCalibrator BUILDER_INDEX = IsdaCompliantIndexCurveCalibrator.standard();
  private static final IsdaCdsTradePricer PRICER = IsdaCdsTradePricer.DEFAULT;
  private static final IsdaHomogenousCdsIndexTradePricer PRICER_INDEX = IsdaHomogenousCdsIndexTradePricer.DEFAULT;
  // valuation CDS
  private static final LocalDate VALUATION_DATE = LocalDate.of(2018, 4, 21);
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG", "ABCD");
  private static final double NOTIONAL = 1.0e7;
  private static final LocalDate START = LocalDate.of(2018, 3, 20);
  private static final LocalDate END = LocalDate.of(2026, 6, 20);
  private static final double FIXED_COUPON = 100d;
  private static final ResolvedCdsTrade CDS = CdsTrade.builder()
      .product(Cds.of(BUY, LEGAL_ENTITY, USD, NOTIONAL, START, END, P3M, USNY, FIXED_COUPON * ONE_BP))
      .info(TradeInfo.of(VALUATION_DATE))
      .build()
      .resolve(REF_DATA);
  private static final ImmutableList<Tenor> TENORS = ImmutableList.of(
      Tenor.TENOR_1Y, Tenor.TENOR_3Y, Tenor.TENOR_5Y, Tenor.TENOR_7Y, Tenor.TENOR_10Y);
  private static final DoubleArray SPREADS = DoubleArray.of(75d, 90d, 115d, 145d, 165d);
  private static final int NUM_QUOTES = TENORS.size();
  // valuation CDS index
  private static final StandardId INDEX_ID = StandardId.of("OG", "AAXX");
  private static final ImmutableList<StandardId> LEGAL_ENTITIES = ImmutableList.of(
      StandardId.of("OG", "AA1"), StandardId.of("OG", "AA2"), StandardId.of("OG", "AA3"), StandardId.of("OG", "AA4"),
      StandardId.of("OG", "AA5"), StandardId.of("OG", "AA6"), StandardId.of("OG", "AA7"), StandardId.of("OG", "AA8"));
  private static final ResolvedCdsIndexTrade CDS_INDEX = CdsIndexTrade.builder()
      .product(CdsIndex.of(BUY, INDEX_ID, LEGAL_ENTITIES, USD, NOTIONAL, START, END, P3M, USNY, FIXED_COUPON * ONE_BP))
      .info(TradeInfo.of(VALUATION_DATE))
      .build()
      .resolve(REF_DATA);
  // curves
  private static final double RECOVERY_RATE = 0.4;
  private static final RecoveryRates RECOVERY_CURVE = ConstantRecoveryRates.of(LEGAL_ENTITY, VALUATION_DATE, RECOVERY_RATE);
  private static final RecoveryRates RECOVERY_CURVE_INDEX = ConstantRecoveryRates.of(INDEX_ID, VALUATION_DATE, RECOVERY_RATE);
  private static final IsdaCreditDiscountFactors DISCOUNT_CURVE = IsdaCreditDiscountFactors.of(
      USD, VALUATION_DATE, CurveName.of("discount"), DoubleArray.of(20d), DoubleArray.of(0.05d), ACT_365F);
  private static final CurveName CDS_CURVE_NAME = CurveName.of("single");
  private static final CurveName INDEX_CURVE_NAME = CurveName.of("index");
  private static final LegalEntitySurvivalProbabilities CREDIT_CURVE;
  private static final LegalEntitySurvivalProbabilities CREDIT_CURVE_INDEX;
  private static final IsdaCreditCurveDefinition CDS_CURVE_DEFINITION;
  private static final IsdaCreditCurveDefinition INDEX_CURVE_DEFINITION;
  private static final ImmutableCreditRatesProvider RATES_PROVIDER_NO_CREDIT;
  static {
    ImmutableMarketDataBuilder dataBuilder = ImmutableMarketData.builder(VALUATION_DATE);
    Builder<CdsIsdaCreditCurveNode> nodesBuilder = ImmutableList.builder();
    Builder<CdsIndexIsdaCreditCurveNode> nodesIndexBuilder = ImmutableList.builder();
    for (int i = 0; i < NUM_QUOTES; i++) {
      QuoteId quoteId = QuoteId.of(StandardId.of("OG", TENORS.get(i).toString())); // same id for single name and index
      CdsIsdaCreditCurveNode node = CdsIsdaCreditCurveNode.ofQuotedSpread(
          TenorCdsTemplate.of(TENORS.get(i), USD_STANDARD),
          quoteId,
          LEGAL_ENTITY,
          FIXED_COUPON * ONE_BP);
      CdsIndexIsdaCreditCurveNode nodeIndex = CdsIndexIsdaCreditCurveNode.ofQuotedSpread(
          TenorCdsTemplate.of(TENORS.get(i), USD_STANDARD),
          quoteId,
          INDEX_ID,
          LEGAL_ENTITIES,
          FIXED_COUPON * ONE_BP);
      dataBuilder.addValue(quoteId, SPREADS.get(i) * ONE_BP);
      nodesBuilder.add(node);
      nodesIndexBuilder.add(nodeIndex);
    }
    LEGAL_ENTITIES.stream().forEach(
        id -> dataBuilder.addValue(LegalEntityInformationId.of(id), getLegalEntityInformation(id)));
    ImmutableMarketData marketData = dataBuilder.build();
    ImmutableList<CdsIsdaCreditCurveNode> nodes = nodesBuilder.build();
    RATES_PROVIDER_NO_CREDIT = ImmutableCreditRatesProvider.builder()
        .valuationDate(VALUATION_DATE)
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, RECOVERY_CURVE, INDEX_ID, RECOVERY_CURVE_INDEX))
        .discountCurves(ImmutableMap.of(USD, DISCOUNT_CURVE))
        .build();
    CDS_CURVE_DEFINITION = IsdaCreditCurveDefinition.of(
        CDS_CURVE_NAME, USD, VALUATION_DATE, ACT_365F, nodes, true, true);
    CREDIT_CURVE = BUILDER.calibrate(CDS_CURVE_DEFINITION, marketData, RATES_PROVIDER_NO_CREDIT, REF_DATA);
    ImmutableList<CdsIndexIsdaCreditCurveNode> nodesIndex = nodesIndexBuilder.build();
    INDEX_CURVE_DEFINITION = IsdaCreditCurveDefinition.of(
        INDEX_CURVE_NAME, USD, VALUATION_DATE, ACT_365F, nodesIndex, true, true);
    CREDIT_CURVE_INDEX = BUILDER_INDEX.calibrate(INDEX_CURVE_DEFINITION, marketData, RATES_PROVIDER_NO_CREDIT, REF_DATA);
  }
  private static final CreditRatesProvider RATES_PROVIDER = ImmutableCreditRatesProvider.builder()
      .valuationDate(VALUATION_DATE)
      .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, RECOVERY_CURVE, INDEX_ID, RECOVERY_CURVE_INDEX))
      .discountCurves(ImmutableMap.of(USD, DISCOUNT_CURVE))
      .creditCurves(ImmutableMap.of(Pair.of(LEGAL_ENTITY, USD), CREDIT_CURVE, Pair.of(INDEX_ID, USD), CREDIT_CURVE_INDEX))
      .build();

  //-------------------------------------------------------------------------
  public void parallel_singleName() {
    double relativeShift = ONE_PC;
    DoubleArray bumpedSpreads = SPREADS.map(d -> d * (1d + relativeShift));
    MarketData bumpedMarketData = createData(bumpedSpreads);
    LegalEntitySurvivalProbabilities bumpedCrditCurve = BUILDER.calibrate(
        CDS_CURVE_DEFINITION, bumpedMarketData, RATES_PROVIDER_NO_CREDIT, REF_DATA);
    CreditRatesProvider bumpedRatesProvider = createCreditRatesProvider(Pair.of(LEGAL_ENTITY, USD), bumpedCrditCurve);
    CurrencyAmount pvBase = PRICER.presentValue(CDS, RATES_PROVIDER, PriceType.DIRTY, REF_DATA);
    CurrencyAmount pvBumped = PRICER.presentValue(CDS, bumpedRatesProvider, PriceType.DIRTY, REF_DATA);
    CurrencyAmount cs1p = pvBumped.minus(pvBase);
    System.out.println(cs1p);
  }

  public void bucketed_singleName() {
    double relativeShift = ONE_PC;
    CurrencyAmount pvBase = PRICER.presentValue(CDS, RATES_PROVIDER, PriceType.DIRTY, REF_DATA);
    double[] bucketedSensitivities = new double[NUM_QUOTES];
    for (int i = 0; i < NUM_QUOTES; ++i) {
      int index = i;
      DoubleArray bumpedSpreads = SPREADS.mapWithIndex((n, v) -> n == index ? v * (1d + relativeShift) : v);
      MarketData bumpedMarketData = createData(bumpedSpreads);
      LegalEntitySurvivalProbabilities bumpedCrditCurve = BUILDER.calibrate(
          CDS_CURVE_DEFINITION, bumpedMarketData, RATES_PROVIDER_NO_CREDIT, REF_DATA);
      CreditRatesProvider bumpedRatesProvider = createCreditRatesProvider(Pair.of(LEGAL_ENTITY, USD), bumpedCrditCurve);
      CurrencyAmount pvBumped = PRICER.presentValue(CDS, bumpedRatesProvider, PriceType.DIRTY, REF_DATA);
      bucketedSensitivities[i] = pvBumped.getAmount() - pvBase.getAmount();
    }
    System.out.println(DoubleArray.ofUnsafe(bucketedSensitivities));
  }

  public void parallel_index() {
    double relativeShift = ONE_PC;
    DoubleArray bumpedSpreads = SPREADS.map(d -> d * (1d + relativeShift));
    MarketData bumpedMarketData = createData(bumpedSpreads);
    LegalEntitySurvivalProbabilities bumpedCrditCurve = BUILDER_INDEX.calibrate(
        INDEX_CURVE_DEFINITION, bumpedMarketData, RATES_PROVIDER_NO_CREDIT, REF_DATA);
    CreditRatesProvider bumpedRatesProvider = createCreditRatesProvider(Pair.of(INDEX_ID, USD), bumpedCrditCurve);
    CurrencyAmount pvBase = PRICER_INDEX.presentValue(CDS_INDEX, RATES_PROVIDER, PriceType.DIRTY, REF_DATA);
    CurrencyAmount pvBumped = PRICER_INDEX.presentValue(CDS_INDEX, bumpedRatesProvider, PriceType.DIRTY, REF_DATA);
    CurrencyAmount cs1p = pvBumped.minus(pvBase);
    System.out.println(cs1p);
  }

  public void bucketed_index() {
    double relativeShift = ONE_PC;
    CurrencyAmount pvBase = PRICER_INDEX.presentValue(CDS_INDEX, RATES_PROVIDER, PriceType.DIRTY, REF_DATA);
    double[] bucketedSensitivities = new double[NUM_QUOTES];
    for (int i = 0; i < NUM_QUOTES; ++i) {
      int index = i;
      DoubleArray bumpedSpreads = SPREADS.mapWithIndex((n, v) -> n == index ? v * (1d + relativeShift) : v);
      MarketData bumpedMarketData = createData(bumpedSpreads);
      LegalEntitySurvivalProbabilities bumpedCrditCurve = BUILDER_INDEX.calibrate(
          INDEX_CURVE_DEFINITION, bumpedMarketData, RATES_PROVIDER_NO_CREDIT, REF_DATA);
      CreditRatesProvider bumpedRatesProvider = createCreditRatesProvider(Pair.of(INDEX_ID, USD), bumpedCrditCurve);
      CurrencyAmount pvBumped = PRICER_INDEX.presentValue(CDS_INDEX, bumpedRatesProvider, PriceType.DIRTY, REF_DATA);
      bucketedSensitivities[i] = pvBumped.getAmount() - pvBase.getAmount();
    }
    System.out.println(DoubleArray.ofUnsafe(bucketedSensitivities));
  }

  //-------------------------------------------------------------------------
  private static MarketData createData(DoubleArray spreads) {
    ImmutableMarketDataBuilder dataBuilder = ImmutableMarketData.builder(VALUATION_DATE);
    for (int i = 0; i < NUM_QUOTES; i++) {
      QuoteId quoteId = QuoteId.of(StandardId.of("OG", TENORS.get(i).toString()));
      dataBuilder.addValue(quoteId, spreads.get(i) * ONE_BP);
    }
    LEGAL_ENTITIES.stream().forEach(
        id -> dataBuilder.addValue(LegalEntityInformationId.of(id), getLegalEntityInformation(id)));
    return dataBuilder.build();
  }

  private static CreditRatesProvider createCreditRatesProvider(
      Pair<StandardId, Currency> idCcy,
      LegalEntitySurvivalProbabilities creditCurve) {

    return ImmutableCreditRatesProvider.builder()
        .valuationDate(VALUATION_DATE)
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, RECOVERY_CURVE, INDEX_ID, RECOVERY_CURVE_INDEX))
        .discountCurves(ImmutableMap.of(USD, DISCOUNT_CURVE))
        .creditCurves(ImmutableMap.of(idCcy, creditCurve))
        .build();
  }

  private static final LegalEntityInformation getLegalEntityInformation(StandardId id) {
    return id.equals(LEGAL_ENTITIES.get(2)) ?  // one single name defaulted
        LegalEntityInformation.isDefaulted(id) :
        LegalEntityInformation.isNotDefaulted(id);
  }

}
