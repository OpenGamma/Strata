/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.ImmutableMarketDataBuilder;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.IsdaCreditCurveDefinition;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.curve.node.CdsIsdaCreditCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ResolvedTradeParameterMetadata;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.credit.Cds;
import com.opengamma.strata.product.credit.CdsIndex;
import com.opengamma.strata.product.credit.CdsIndexTrade;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.credit.ResolvedCdsIndexTrade;
import com.opengamma.strata.product.credit.ResolvedCdsTrade;
import com.opengamma.strata.product.credit.type.CdsConvention;
import com.opengamma.strata.product.credit.type.DatesCdsTemplate;
import com.opengamma.strata.product.credit.type.ImmutableCdsConvention;

/**
 * Test {@link AnalyticSpreadSensitivityCalculator} and {@link FiniteDifferenceSpreadSensitivityCalculator}.
 */
@Test
public class SpreadSensitivityCalculatorTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double ONE_BP = 1.0e-4;

  private static final IsdaCompliantCreditCurveCalibrator BUILDER = FastCreditCurveCalibrator.standard();
  private static final IsdaCdsTradePricer PRICER = IsdaCdsTradePricer.DEFAULT;
  private static final IsdaHomogenousCdsIndexTradePricer PRICER_INDEX = IsdaHomogenousCdsIndexTradePricer.DEFAULT;
  private static final FiniteDifferenceSpreadSensitivityCalculator CS01_FD = FiniteDifferenceSpreadSensitivityCalculator.DEFAULT;
  private static final AnalyticSpreadSensitivityCalculator CS01_AN = AnalyticSpreadSensitivityCalculator.DEFAULT;
  private static final MarketQuoteSensitivityCalculator QUOTE_CAL = MarketQuoteSensitivityCalculator.DEFAULT;
  // valuation CDS
  private static final LocalDate VALUATION_DATE = LocalDate.of(2013, 4, 21);
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG", "ABCD");
  private static final double NOTIONAL = 1e7;
  private static final LocalDate START = LocalDate.of(2013, 2, 3);
  private static final LocalDate END1 = LocalDate.of(2018, 3, 20);
  private static final LocalDate END2 = LocalDate.of(2020, 2, 20);
  private static final double DEAL_SPREAD = 101;
  private static final ResolvedCdsTrade CDS1 = CdsTrade.builder()
      .product(Cds.of(BUY, LEGAL_ENTITY, USD, NOTIONAL, START, END1, P3M, SAT_SUN, DEAL_SPREAD * ONE_BP))
      .info(TradeInfo.of(VALUATION_DATE))
      .build()
      .resolve(REF_DATA);
  private static final ResolvedCdsTrade CDS2 = CdsTrade.builder()
      .product(Cds.of(BUY, LEGAL_ENTITY, USD, NOTIONAL, START, END2, P3M, SAT_SUN, DEAL_SPREAD * ONE_BP))
      .info(TradeInfo.of(VALUATION_DATE))
      .build()
      .resolve(REF_DATA);
  // market CDSs
  private static final LocalDate[] PAR_SPD_DATES =
      new LocalDate[] {LocalDate.of(2013, 6, 20), LocalDate.of(2013, 9, 20), LocalDate.of(2014, 3, 20), LocalDate.of(2015, 3, 20),
          LocalDate.of(2016, 3, 20), LocalDate.of(2018, 3, 20), LocalDate.of(2023, 3, 20)};
  private static final double[] PAR_SPREADS = new double[] {50, 70, 80, 95, 100, 95, 80};
  private static final int NUM_MARKET_CDS = PAR_SPD_DATES.length;
  private static final ResolvedCdsTrade[] MARKET_CDS = new ResolvedCdsTrade[NUM_MARKET_CDS];
  private static final ResolvedCdsIndexTrade[] MARKET_CDS_INDEX = new ResolvedCdsIndexTrade[NUM_MARKET_CDS];
  // valuation CDS index
  private static final StandardId INDEX_ID = StandardId.of("OG", "AAXX");
  private static final ImmutableList<StandardId> LEGAL_ENTITIES = ImmutableList.of(
      StandardId.of("OG", "AA1"), StandardId.of("OG", "AA2"), StandardId.of("OG", "AA3"), StandardId.of("OG", "AA4"));
  private static final double INDEX_FACTOR = 0.75;
  private static final ResolvedCdsIndexTrade CDS_INDEX = CdsIndexTrade.builder()
      .product(CdsIndex.of(
          BUY, INDEX_ID, LEGAL_ENTITIES, USD, NOTIONAL, START, END2, P3M, SAT_SUN, DEAL_SPREAD * ONE_BP))
      .info(TradeInfo.of(VALUATION_DATE))
      .build()
      .resolve(REF_DATA);
  // curve
  private static final double RECOVERY_RATE = 0.4;
  private static final RecoveryRates RECOVERY_CURVE = ConstantRecoveryRates.of(LEGAL_ENTITY, VALUATION_DATE, RECOVERY_RATE);
  private static final RecoveryRates RECOVERY_CURVE_INDEX = ConstantRecoveryRates.of(INDEX_ID, VALUATION_DATE, RECOVERY_RATE);
  private static final IsdaCreditDiscountFactors YIELD_CURVE;
  private static final LegalEntitySurvivalProbabilities CREDIT_CURVE;
  private static final LegalEntitySurvivalProbabilities CREDIT_CURVE_INDEX;
  private static final CurveName CREDIT_CURVE_NAME = CurveName.of("credit");
  private static final CdsConvention CDS_CONV = ImmutableCdsConvention.builder()
      .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, SAT_SUN))
      .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
      .currency(USD)
      .dayCount(ACT_360)
      .name("sat_sun_conv")
      .paymentFrequency(Frequency.P3M)
      .settlementDateOffset(DaysAdjustment.ofBusinessDays(3, SAT_SUN))
      .build();
  private static final ImmutableList<ResolvedTradeParameterMetadata> CDS_METADATA;
  private static final ImmutableList<ResolvedTradeParameterMetadata> CDS_INDEX_METADATA;
  static {
    double flatRate = 0.05;
    double t = 20.0;
    YIELD_CURVE = IsdaCreditDiscountFactors.of(
        USD, VALUATION_DATE, CurveName.of("discount"), DoubleArray.of(t), DoubleArray.of(flatRate), ACT_365F);
    ImmutableMarketDataBuilder dataBuilder = ImmutableMarketData.builder(VALUATION_DATE);
    Builder<CdsIsdaCreditCurveNode> nodesBuilder = ImmutableList.builder();
    Builder<ResolvedTradeParameterMetadata> cdsMetadataBuilder = ImmutableList.builder();
    Builder<ResolvedTradeParameterMetadata> cdsIndexMetadataBuilder = ImmutableList.builder();
    for (int i = 0; i < NUM_MARKET_CDS; i++) {
      QuoteId quoteId = QuoteId.of(StandardId.of("OG", PAR_SPD_DATES[i].toString()));
      CdsIsdaCreditCurveNode node = CdsIsdaCreditCurveNode.ofParSpread(
          DatesCdsTemplate.of(VALUATION_DATE, PAR_SPD_DATES[i], CDS_CONV), quoteId, LEGAL_ENTITY);
      MARKET_CDS[i] = CdsTrade.builder()
          .product(Cds.of(
              BUY, LEGAL_ENTITY, USD, NOTIONAL, VALUATION_DATE, PAR_SPD_DATES[i], P3M, SAT_SUN, PAR_SPREADS[i] * ONE_BP))
          .info(TradeInfo.of(VALUATION_DATE))
          .build()
          .resolve(REF_DATA);
      MARKET_CDS_INDEX[i] = CdsIndexTrade.builder()
          .product(CdsIndex.of(
              BuySell.BUY, INDEX_ID, LEGAL_ENTITIES, USD, NOTIONAL, VALUATION_DATE, PAR_SPD_DATES[i], P3M, SAT_SUN,
              PAR_SPREADS[i] * ONE_BP))
          .info(TradeInfo.of(VALUATION_DATE))
          .build()
          .resolve(REF_DATA);
      dataBuilder.addValue(quoteId, PAR_SPREADS[i] * ONE_BP);
      nodesBuilder.add(node);
      cdsMetadataBuilder.add(ResolvedTradeParameterMetadata.of(
          MARKET_CDS[i],
          MARKET_CDS[i].getProduct().getProtectionEndDate().toString()));
      cdsIndexMetadataBuilder.add(ResolvedTradeParameterMetadata.of(
          MARKET_CDS_INDEX[i],
          MARKET_CDS_INDEX[i].getProduct().getProtectionEndDate().toString()));
    }
    ImmutableMarketData marketData = dataBuilder.build();
    ImmutableList<CdsIsdaCreditCurveNode> nodes = nodesBuilder.build();
    CDS_METADATA = cdsMetadataBuilder.build();
    CDS_INDEX_METADATA = cdsIndexMetadataBuilder.build();
    ImmutableCreditRatesProvider rates = ImmutableCreditRatesProvider.builder()
        .valuationDate(VALUATION_DATE)
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, RECOVERY_CURVE))
        .discountCurves(ImmutableMap.of(USD, YIELD_CURVE))
        .build();
    IsdaCreditCurveDefinition definition = IsdaCreditCurveDefinition.of(
        CREDIT_CURVE_NAME, USD, VALUATION_DATE, ACT_365F, nodes, true, true);
    CREDIT_CURVE = BUILDER.calibrate(definition, marketData, rates, REF_DATA);
    NodalCurve underlyingCurve = ((IsdaCreditDiscountFactors) CREDIT_CURVE.getSurvivalProbabilities()).getCurve();
    NodalCurve curveWithFactor = underlyingCurve.withMetadata(
        underlyingCurve.getMetadata()
            .withInfo(CurveInfoType.CDS_INDEX_FACTOR, INDEX_FACTOR)
            .withParameterMetadata(CDS_INDEX_METADATA)); // replace parameter metadata
    CREDIT_CURVE_INDEX = LegalEntitySurvivalProbabilities.of(
        INDEX_ID, IsdaCreditDiscountFactors.of(USD, VALUATION_DATE, curveWithFactor));
  }
  private static final CreditRatesProvider RATES_PROVIDER = ImmutableCreditRatesProvider.builder()
      .valuationDate(VALUATION_DATE)
      .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, RECOVERY_CURVE, INDEX_ID, RECOVERY_CURVE_INDEX))
      .discountCurves(ImmutableMap.of(USD, YIELD_CURVE))
      .creditCurves(ImmutableMap.of(Pair.of(LEGAL_ENTITY, USD), CREDIT_CURVE, Pair.of(INDEX_ID, USD), CREDIT_CURVE_INDEX))
      .build();
  private static final double TOL = 1.0e-13;

  public void parellelCs01Test() {
    double fromExcel = 4238.557409;
    CurrencyAmount fd = CS01_FD.parallelCs01(CDS1, ImmutableList.copyOf(MARKET_CDS), RATES_PROVIDER, REF_DATA);
    CurrencyAmount analytic = CS01_AN.parallelCs01(CDS1, ImmutableList.copyOf(MARKET_CDS), RATES_PROVIDER, REF_DATA);
    assertEquals(fd.getCurrency(), USD);
    assertEquals(fd.getAmount() * ONE_BP, fromExcel, TOL * NOTIONAL);
    assertEquals(analytic.getCurrency(), USD);
    assertEquals(analytic.getAmount() * ONE_BP, fd.getAmount() * ONE_BP, ONE_BP * NOTIONAL);
    // equivalence to market quote sensitivity for par spread quote
    PointSensitivities point = PRICER.presentValueOnSettleSensitivity(CDS1, RATES_PROVIDER, REF_DATA);
    CurrencyParameterSensitivity paramSensi = RATES_PROVIDER.singleCreditCurveParameterSensitivity(point, LEGAL_ENTITY, USD);
    CurrencyParameterSensitivities quoteSensi =
        QUOTE_CAL.sensitivity(CurrencyParameterSensitivities.of(paramSensi), RATES_PROVIDER);
    double cs01FromQuoteSensi = quoteSensi.getSensitivities().get(0).getSensitivity().sum();
    assertEquals(cs01FromQuoteSensi * ONE_BP, analytic.getAmount() * ONE_BP, TOL * NOTIONAL);
  }

  public void parellelCs01FromNodesTest() {
    double fromExcel = 4238.557409;
    CurrencyAmount fd = CS01_FD.parallelCs01(CDS1, RATES_PROVIDER, REF_DATA);
    CurrencyAmount analytic = CS01_AN.parallelCs01(CDS1, RATES_PROVIDER, REF_DATA);
    assertEquals(fd.getCurrency(), USD);
    assertEquals(fd.getAmount() * ONE_BP, fromExcel, TOL * NOTIONAL);
    assertEquals(analytic.getCurrency(), USD);
    assertEquals(analytic.getAmount() * ONE_BP, fd.getAmount() * ONE_BP, ONE_BP * NOTIONAL);
    // equivalence to market quote sensitivity for par spread quote
    PointSensitivities point = PRICER.presentValueOnSettleSensitivity(CDS1, RATES_PROVIDER, REF_DATA);
    CurrencyParameterSensitivity paramSensi = RATES_PROVIDER.singleCreditCurveParameterSensitivity(point, LEGAL_ENTITY, USD);
    CurrencyParameterSensitivities quoteSensi =
        QUOTE_CAL.sensitivity(CurrencyParameterSensitivities.of(paramSensi), RATES_PROVIDER);
    double cs01FromQuoteSensi = quoteSensi.getSensitivities().get(0).getSensitivity().sum();
    assertEquals(cs01FromQuoteSensi * ONE_BP, analytic.getAmount() * ONE_BP, TOL * NOTIONAL);
  }

  public void bucketedCs01Test() {
    double[] expectedFd = new double[] {
        0.02446907003406107, 0.1166137422736746, 0.5196553952424576, 1.4989046391578054, 3.5860718603647483, 4233.77162264947,
        0.0};
    CurrencyParameterSensitivity fd = CS01_FD.bucketedCs01(CDS1, ImmutableList.copyOf(MARKET_CDS), RATES_PROVIDER, REF_DATA);
    CurrencyParameterSensitivity analytic =
        CS01_AN.bucketedCs01(CDS1, ImmutableList.copyOf(MARKET_CDS), RATES_PROVIDER, REF_DATA);
    assertEquals(fd.getCurrency(), USD);
    assertEquals(fd.getMarketDataName(), CurveName.of("impliedSpreads"));
    assertEquals(fd.getParameterCount(), NUM_MARKET_CDS);
    assertEquals(fd.getParameterMetadata(), CDS_METADATA);
    assertTrue(DoubleArrayMath.fuzzyEquals(fd.getSensitivity().multipliedBy(ONE_BP).toArray(), expectedFd, NOTIONAL * TOL));
    assertEquals(analytic.getCurrency(), USD);
    assertEquals(analytic.getMarketDataName(), CurveName.of("impliedSpreads"));
    assertEquals(analytic.getParameterCount(), NUM_MARKET_CDS);
    assertEquals(analytic.getParameterMetadata(), CDS_METADATA);
    assertTrue(DoubleArrayMath.fuzzyEquals(
        analytic.getSensitivity().toArray(), fd.getSensitivity().toArray(), NOTIONAL * ONE_BP * 10d));
    PointSensitivities point = PRICER.presentValueOnSettleSensitivity(CDS1, RATES_PROVIDER, REF_DATA);
    CurrencyParameterSensitivity paramSensi = RATES_PROVIDER.singleCreditCurveParameterSensitivity(point, LEGAL_ENTITY, USD);
    CurrencyParameterSensitivities quoteSensi =
        QUOTE_CAL.sensitivity(CurrencyParameterSensitivities.of(paramSensi), RATES_PROVIDER);
    assertTrue(DoubleArrayMath.fuzzyEquals(
        quoteSensi.getSensitivities().get(0).getSensitivity().toArray(), analytic.getSensitivity().toArray(), NOTIONAL * TOL));
  }

  public void bucketedCs01SingleNodeCurveTest() {
    ImmutableCreditRatesProvider ratesProviderNoCredit = ImmutableCreditRatesProvider.builder()
        .valuationDate(VALUATION_DATE)
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, RECOVERY_CURVE))
        .discountCurves(ImmutableMap.of(USD, YIELD_CURVE))
        .build();
    QuoteId quoteId = QuoteId.of(StandardId.of("OG", END2.toString()));
    CdsIsdaCreditCurveNode node =
        CdsIsdaCreditCurveNode.ofParSpread(DatesCdsTemplate.of(START, END2, CDS_CONV), quoteId, LEGAL_ENTITY);
    ImmutableMarketData marketData = ImmutableMarketData.builder(VALUATION_DATE).addValue(quoteId, DEAL_SPREAD * ONE_BP).build();
    IsdaCreditCurveDefinition definition = IsdaCreditCurveDefinition.of(
        CREDIT_CURVE_NAME, USD, VALUATION_DATE, ACT_365F, ImmutableList.of(node), true, false);
    LegalEntitySurvivalProbabilities creditCurve = BUILDER.calibrate(definition, marketData, ratesProviderNoCredit, REF_DATA);
    ImmutableCreditRatesProvider ratesProvider = ImmutableCreditRatesProvider.builder()
        .valuationDate(VALUATION_DATE)
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, RECOVERY_CURVE))
        .discountCurves(ImmutableMap.of(USD, YIELD_CURVE))
        .creditCurves(ImmutableMap.of(Pair.of(LEGAL_ENTITY, USD), creditCurve))
        .build();
    double[] expectedFd = new double[] {
        -6.876275937539589E-4, 1.1832215762730414E-4, 0.0012340982402658796, 0.002784985575488008, 0.005287295115619095,
        2429.636217554099, 3101.303324461041};
    CurrencyParameterSensitivity analytic = CS01_AN.bucketedCs01(CDS2, ImmutableList.copyOf(MARKET_CDS), ratesProvider, REF_DATA);
    CurrencyParameterSensitivity fd = CS01_FD.bucketedCs01(CDS2, ImmutableList.copyOf(MARKET_CDS), ratesProvider, REF_DATA);
    assertEquals(fd.getCurrency(), USD);
    assertEquals(fd.getMarketDataName(), CurveName.of("impliedSpreads"));
    assertEquals(fd.getParameterCount(), NUM_MARKET_CDS);
    assertEquals(fd.getParameterMetadata(), CDS_METADATA);
    assertTrue(DoubleArrayMath.fuzzyEquals(fd.getSensitivity().multipliedBy(ONE_BP).toArray(), expectedFd, NOTIONAL * TOL));
    assertEquals(analytic.getCurrency(), USD);
    assertEquals(analytic.getMarketDataName(), CurveName.of("impliedSpreads"));
    assertEquals(analytic.getParameterCount(), NUM_MARKET_CDS);
    assertEquals(analytic.getParameterMetadata(), CDS_METADATA);
    assertTrue(DoubleArrayMath.fuzzyEquals(
        analytic.getSensitivity().toArray(), fd.getSensitivity().toArray(), NOTIONAL * ONE_BP * 10d));
  }

  //-------------------------------------------------------------------------
  public void parellelCs01IndexTest() {
    CurrencyAmount fdSingle = CS01_FD.parallelCs01(CDS2, ImmutableList.copyOf(MARKET_CDS), RATES_PROVIDER, REF_DATA);
    CurrencyAmount analyticSingle = CS01_AN.parallelCs01(CDS2, ImmutableList.copyOf(MARKET_CDS), RATES_PROVIDER, REF_DATA);
    CurrencyAmount fd = CS01_FD.parallelCs01(CDS_INDEX, ImmutableList.copyOf(MARKET_CDS_INDEX), RATES_PROVIDER, REF_DATA);
    CurrencyAmount analytic = CS01_AN.parallelCs01(CDS_INDEX, ImmutableList.copyOf(MARKET_CDS_INDEX), RATES_PROVIDER, REF_DATA);
    assertEquals(fd.getCurrency(), USD);
    assertEquals(fd.getAmount(), fdSingle.getAmount() * INDEX_FACTOR, TOL * NOTIONAL);
    assertEquals(analytic.getAmount(), analyticSingle.getAmount() * INDEX_FACTOR, TOL * NOTIONAL);
    assertEquals(analytic.getCurrency(), USD);
    // equivalence to market quote sensitivity for par spread quote
    PointSensitivities point = PRICER_INDEX.presentValueOnSettleSensitivity(CDS_INDEX, RATES_PROVIDER, REF_DATA);
    CurrencyParameterSensitivity paramSensi = RATES_PROVIDER.singleCreditCurveParameterSensitivity(point, INDEX_ID, USD);
    CurrencyParameterSensitivities quoteSensi =
        QUOTE_CAL.sensitivity(CurrencyParameterSensitivities.of(paramSensi), RATES_PROVIDER);
    double cs01FromQuoteSensi = quoteSensi.getSensitivities().get(0).getSensitivity().sum();
    assertEquals(cs01FromQuoteSensi, analytic.getAmount(), TOL * NOTIONAL);
  }

  public void parellelCs01WithNodesIndexTest() {
    CurrencyAmount fdSingle = CS01_FD.parallelCs01(CDS2, RATES_PROVIDER, REF_DATA);
    CurrencyAmount analyticSingle = CS01_AN.parallelCs01(CDS2, RATES_PROVIDER, REF_DATA);
    CurrencyAmount fd = CS01_FD.parallelCs01(CDS_INDEX, RATES_PROVIDER, REF_DATA);
    CurrencyAmount analytic = CS01_AN.parallelCs01(CDS_INDEX, RATES_PROVIDER, REF_DATA);
    assertEquals(fd.getCurrency(), USD);
    assertEquals(fd.getAmount(), fdSingle.getAmount() * INDEX_FACTOR, TOL * NOTIONAL);
    assertEquals(analytic.getAmount(), analyticSingle.getAmount() * INDEX_FACTOR, TOL * NOTIONAL);
    assertEquals(analytic.getCurrency(), USD);
    // equivalence to market quote sensitivity for par spread quote
    PointSensitivities point = PRICER_INDEX.presentValueOnSettleSensitivity(CDS_INDEX, RATES_PROVIDER, REF_DATA);
    CurrencyParameterSensitivity paramSensi = RATES_PROVIDER.singleCreditCurveParameterSensitivity(point, INDEX_ID, USD);
    CurrencyParameterSensitivities quoteSensi =
        QUOTE_CAL.sensitivity(CurrencyParameterSensitivities.of(paramSensi), RATES_PROVIDER);
    double cs01FromQuoteSensi = quoteSensi.getSensitivities().get(0).getSensitivity().sum();
    assertEquals(cs01FromQuoteSensi, analytic.getAmount(), TOL * NOTIONAL);
  }

  public void bucketedCs01IndexTest() {
    CurrencyParameterSensitivity fdSingle = CS01_FD.bucketedCs01(
        CDS2, ImmutableList.copyOf(MARKET_CDS), RATES_PROVIDER, REF_DATA);
    CurrencyParameterSensitivity analyticSingle = CS01_AN.bucketedCs01(
        CDS2, ImmutableList.copyOf(MARKET_CDS), RATES_PROVIDER, REF_DATA);
    CurrencyParameterSensitivity fd = CS01_FD.bucketedCs01(
        CDS_INDEX, ImmutableList.copyOf(MARKET_CDS_INDEX), RATES_PROVIDER, REF_DATA);
    CurrencyParameterSensitivity analytic = CS01_AN.bucketedCs01(
        CDS_INDEX, ImmutableList.copyOf(MARKET_CDS_INDEX), RATES_PROVIDER, REF_DATA);
    assertEquals(fd.getCurrency(), USD);
    assertEquals(fd.getMarketDataName(), CurveName.of("impliedSpreads"));
    assertEquals(fd.getParameterCount(), NUM_MARKET_CDS);
    assertEquals(fd.getParameterMetadata(), CDS_INDEX_METADATA);
    assertTrue(DoubleArrayMath.fuzzyEquals(
        fd.getSensitivity().toArray(),
        fdSingle.getSensitivity().multipliedBy(INDEX_FACTOR).toArray(),
        NOTIONAL * TOL));
    assertEquals(analytic.getCurrency(), USD);
    assertEquals(analytic.getMarketDataName(), CurveName.of("impliedSpreads"));
    assertEquals(analytic.getParameterCount(), NUM_MARKET_CDS);
    assertEquals(analytic.getParameterMetadata(), CDS_INDEX_METADATA);
    assertTrue(DoubleArrayMath.fuzzyEquals(
        analytic.getSensitivity().toArray(),
        analyticSingle.getSensitivity().multipliedBy(INDEX_FACTOR).toArray(),
        NOTIONAL * TOL));
    PointSensitivities point = PRICER_INDEX.presentValueOnSettleSensitivity(CDS_INDEX, RATES_PROVIDER, REF_DATA);
    CurrencyParameterSensitivity paramSensi = RATES_PROVIDER.singleCreditCurveParameterSensitivity(point, INDEX_ID, USD);
    CurrencyParameterSensitivities quoteSensi =
        QUOTE_CAL.sensitivity(CurrencyParameterSensitivities.of(paramSensi), RATES_PROVIDER);
    assertTrue(DoubleArrayMath.fuzzyEquals(
        quoteSensi.getSensitivities().get(0).getSensitivity().toArray(),
        analytic.getSensitivity().toArray(),
        NOTIONAL * TOL));
  }

  public void bucketedCs01WithNodesIndexTest() {
    CurrencyParameterSensitivity fdSingle = CS01_FD.bucketedCs01(CDS2, RATES_PROVIDER, REF_DATA);
    CurrencyParameterSensitivity analyticSingle = CS01_AN.bucketedCs01(CDS2, RATES_PROVIDER, REF_DATA);
    CurrencyParameterSensitivity fd = CS01_FD.bucketedCs01(CDS_INDEX, RATES_PROVIDER, REF_DATA);
    CurrencyParameterSensitivity analytic = CS01_AN.bucketedCs01(CDS_INDEX, RATES_PROVIDER, REF_DATA);
    assertEquals(fd.getCurrency(), USD);
    assertEquals(fd.getMarketDataName(), CurveName.of("impliedSpreads"));
    assertEquals(fd.getParameterCount(), NUM_MARKET_CDS);
    assertEquals(fd.getParameterMetadata(), CDS_INDEX_METADATA);
    assertTrue(DoubleArrayMath.fuzzyEquals(
        fd.getSensitivity().toArray(),
        fdSingle.getSensitivity().multipliedBy(INDEX_FACTOR).toArray(),
        NOTIONAL * TOL));
    assertEquals(analytic.getCurrency(), USD);
    assertEquals(analytic.getMarketDataName(), CurveName.of("impliedSpreads"));
    assertEquals(analytic.getParameterCount(), NUM_MARKET_CDS);
    assertEquals(analytic.getParameterMetadata(), CDS_INDEX_METADATA);
    assertTrue(DoubleArrayMath.fuzzyEquals(
        analytic.getSensitivity().toArray(),
        analyticSingle.getSensitivity().multipliedBy(INDEX_FACTOR).toArray(),
        NOTIONAL * TOL));
    PointSensitivities point = PRICER_INDEX.presentValueOnSettleSensitivity(CDS_INDEX, RATES_PROVIDER, REF_DATA);
    CurrencyParameterSensitivity paramSensi = RATES_PROVIDER.singleCreditCurveParameterSensitivity(point, INDEX_ID, USD);
    CurrencyParameterSensitivities quoteSensi =
        QUOTE_CAL.sensitivity(CurrencyParameterSensitivities.of(paramSensi), RATES_PROVIDER);
    assertTrue(DoubleArrayMath.fuzzyEquals(
        quoteSensi.getSensitivities().get(0).getSensitivity().toArray(),
        analytic.getSensitivity().toArray(),
        NOTIONAL * TOL));
  }

}
