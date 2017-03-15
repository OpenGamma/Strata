/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.loader.csv.QuotesCsvLoader;
import com.opengamma.strata.loader.csv.RatesCalibrationCsvLoader;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveDefinition;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.CurveParameterSize;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.pricer.sensitivity.NotionalEquivalentCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapTradePricer;
import com.opengamma.strata.product.ResolvedTrade;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;
import com.opengamma.strata.product.swap.type.ThreeLegBasisSwapConventions;


/**
 * Test the notional equivalent computation based on present value sensitivity to quote in 
 * the calibrated curves by {@link CurveCalibrator}.
 */  
@Test
public class CalibrationNotionalEquivalentTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final LocalDate VALUATION_DATE = LocalDate.of(2016, 2, 29);

  private static final String BASE_DIR = "src/test/resources/";
  private static final String GROUPS_FILE = "curve-config/EUR-DSCONOIS-E3BS-E6IRS-group.csv";
  private static final String SETTINGS_FILE = "curve-config/EUR-DSCONOIS-E3BS-E6IRS-settings.csv";
  private static final String NODES_FILE = "curve-config/EUR-DSCONOIS-E3BS-E6IRS-nodes.csv";
  private static final String QUOTES_FILE = "quotes/quotes-20160229-eur.csv";

  private static final CalibrationMeasures CALIBRATION_MEASURES = CalibrationMeasures.PAR_SPREAD;
  private static final CurveCalibrator CALIBRATOR = CurveCalibrator.of(1e-9, 1e-9, 100, CALIBRATION_MEASURES);
  private static final CalibrationMeasures PV_MEASURES = CalibrationMeasures.of(
      "PresentValue",
      PresentValueCalibrationMeasure.FRA_PV,
      PresentValueCalibrationMeasure.IBOR_FIXING_DEPOSIT_PV,
      PresentValueCalibrationMeasure.IBOR_FUTURE_PV,
      PresentValueCalibrationMeasure.SWAP_PV,
      PresentValueCalibrationMeasure.TERM_DEPOSIT_PV);
  private static final DiscountingSwapTradePricer PRICER_SWAP_TRADE = DiscountingSwapTradePricer.DEFAULT;
  private static final MarketQuoteSensitivityCalculator MQSC = MarketQuoteSensitivityCalculator.DEFAULT;
  private static final NotionalEquivalentCalculator NEC = NotionalEquivalentCalculator.DEFAULT;

  private static final ResourceLocator QUOTES_RESOURCES = ResourceLocator.of(BASE_DIR + QUOTES_FILE);
  private static final ImmutableMap<QuoteId, Double> QUOTES = QuotesCsvLoader.load(VALUATION_DATE, QUOTES_RESOURCES);
  private static final ImmutableMarketData MARKET_QUOTES = ImmutableMarketData.of(VALUATION_DATE, QUOTES);
  private static final CurveGroupDefinition GROUP_DEFINITION = RatesCalibrationCsvLoader
      .load(ResourceLocator.of(BASE_DIR + GROUPS_FILE),
          ResourceLocator.of(BASE_DIR + SETTINGS_FILE),
          ResourceLocator.of(BASE_DIR + NODES_FILE))
      .get(CurveGroupName.of("EUR-DSCONOIS-E3BS-E6IRS"));
  private static final CurveGroupDefinition GROUP_DEFINITION_NO_INFO = GROUP_DEFINITION.toBuilder()
      .computeJacobian(false).computePvSensitivityToMarketQuote(false).build();
  private static final CurveGroupDefinition GROUP_DEFINITION_PV_SENSI = GROUP_DEFINITION.toBuilder()
      .computeJacobian(true).computePvSensitivityToMarketQuote(true).build();

  private static final double TOLERANCE_PV = 1.0E-8;
  private static final double TOLERANCE_PV_DELTA = 1.0E-2;

  public void check_pv_with_measures() {
    ImmutableRatesProvider multicurve =
        CALIBRATOR.calibrate(GROUP_DEFINITION, MARKET_QUOTES, REF_DATA);
    // the trades used for calibration
    List<ResolvedTrade> trades = new ArrayList<>();
    ImmutableList<CurveDefinition> curveGroups = GROUP_DEFINITION.getCurveDefinitions();
    for (CurveDefinition entry : curveGroups) {
      ImmutableList<CurveNode> nodes = entry.getNodes();
      for (CurveNode node : nodes) {
        trades.add(node.resolvedTrade(1d, MARKET_QUOTES, REF_DATA));
      }
    }
    // Check PV = 0
    for (ResolvedTrade trade : trades) {
      double pv = PV_MEASURES.value(trade, multicurve);
      assertEquals(pv, 0.0, TOLERANCE_PV);
    }
  }

  public void check_pv_sensitivity() {
    ImmutableRatesProvider multicurve =
        CALIBRATOR.calibrate(GROUP_DEFINITION_PV_SENSI, MARKET_QUOTES, REF_DATA);
    // the trades used for calibration
    Map<CurveName, List<Trade>> trades = new HashMap<>();
    Map<CurveName, List<ResolvedTrade>> resolvedTrades = new HashMap<>();
    ImmutableList<CurveDefinition> curveGroups = GROUP_DEFINITION.getCurveDefinitions();
    ImmutableList.Builder<CurveParameterSize> builder = ImmutableList.builder();
    for (CurveDefinition entry : curveGroups) {
      ImmutableList<CurveNode> nodes = entry.getNodes();
      List<Trade> tradesCurve = new ArrayList<>();
      List<ResolvedTrade> resolvedTradesCurve = new ArrayList<>();
      for (CurveNode node : nodes) {
        tradesCurve.add(node.trade(1d, MARKET_QUOTES, REF_DATA));
        resolvedTradesCurve.add(node.resolvedTrade(1d, MARKET_QUOTES, REF_DATA));
      }
      trades.put(entry.getName(), tradesCurve);
      resolvedTrades.put(entry.getName(), resolvedTradesCurve);
      builder.add(entry.toCurveParameterSize());
    }
    ImmutableList<CurveParameterSize> order = builder.build(); // order of the curves
    // Check CurveInfo present and sensitivity as expected
    Map<CurveName, DoubleArray> mqsGroup = new HashMap<>();
    int nodeIndex = 0;
    for (CurveParameterSize cps : order) {
      int nbParameters = cps.getParameterCount();
      double[] mqsCurve = new double[nbParameters];
      for (int looptrade = 0; looptrade < nbParameters; looptrade++) {
        DoubleArray mqsNode = PV_MEASURES.derivative(resolvedTrades.get(cps.getName()).get(looptrade), multicurve, order);
        mqsCurve[looptrade] = mqsNode.get(nodeIndex);
        nodeIndex++;
      }
      Optional<Curve> curve = multicurve.findData(cps.getName());
      DoubleArray pvSensitivityExpected = DoubleArray.ofUnsafe(mqsCurve);
      mqsGroup.put(cps.getName(), pvSensitivityExpected);
      assertTrue(curve.isPresent());
      assertTrue(curve.get().getMetadata().findInfo(CurveInfoType.PV_SENSITIVITY_TO_MARKET_QUOTE).isPresent());
      DoubleArray pvSensitivityMetadata =
          curve.get().getMetadata().findInfo(CurveInfoType.PV_SENSITIVITY_TO_MARKET_QUOTE).get();
      assertTrue(pvSensitivityExpected.equalWithTolerance(pvSensitivityMetadata, 1.0E-10));
    }
  }

  public void check_equivalent_notional() {
    ImmutableRatesProvider multicurve =
        CALIBRATOR.calibrate(GROUP_DEFINITION_PV_SENSI, MARKET_QUOTES, REF_DATA);
    // Create notional equivalent for a basis trade
    ResolvedSwapTrade trade = ThreeLegBasisSwapConventions.EUR_FIXED_1Y_EURIBOR_3M_EURIBOR_6M
        .createTrade(VALUATION_DATE, Period.ofMonths(7), Tenor.TENOR_6Y, BuySell.SELL, 1_000_000, 0.03, REF_DATA)
        .resolve(REF_DATA);
    PointSensitivities pts = PRICER_SWAP_TRADE.presentValueSensitivity(trade, multicurve);
    CurrencyParameterSensitivities ps = multicurve.parameterSensitivity(pts);
    CurrencyParameterSensitivities mqs = MQSC.sensitivity(ps, multicurve);
    CurrencyParameterSensitivities notionalEquivalent = NEC.notionalEquivalent(mqs, multicurve);
    // Check metadata are same as market quote sensitivities.
    for(CurrencyParameterSensitivity sensi: mqs.getSensitivities()){
      assertEquals(notionalEquivalent.getSensitivity(sensi.getMarketDataName(), sensi.getCurrency()).getParameterMetadata(), 
          sensi.getParameterMetadata());
    }
    // Check sensitivity: trade sensitivity = sum(notional equivalent sensitivities)
    int totalNbParameters = 0;
    Map<CurveName, List<ResolvedTrade>> equivalentTrades = new HashMap<>();
    ImmutableList<CurveDefinition> curveGroups = GROUP_DEFINITION.getCurveDefinitions();
    ImmutableList.Builder<CurveParameterSize> builder = ImmutableList.builder();
    for (CurveDefinition entry : curveGroups) {
      totalNbParameters += entry.getParameterCount();
      DoubleArray notionalCurve = notionalEquivalent.getSensitivity(entry.getName(), Currency.EUR).getSensitivity();
      ImmutableList<CurveNode> nodes = entry.getNodes();
      List<ResolvedTrade> resolvedTradesCurve = new ArrayList<>();
      for (int i = 0; i < nodes.size(); i++) {
        resolvedTradesCurve.add(nodes.get(i).resolvedTrade(notionalCurve.get(i), MARKET_QUOTES, REF_DATA));
      }
      equivalentTrades.put(entry.getName(), resolvedTradesCurve);
      builder.add(entry.toCurveParameterSize());
    }
    ImmutableList<CurveParameterSize> order = builder.build(); // order of the curves
    DoubleArray totalSensitivity = DoubleArray.filled(totalNbParameters);
    for (Entry<CurveName, List<ResolvedTrade>> entry : equivalentTrades.entrySet()) {
      for (ResolvedTrade t : entry.getValue()) {
        totalSensitivity = totalSensitivity.plus(PV_MEASURES.derivative(t, multicurve, order));
      }
    }
    DoubleArray instrumentSensi = PV_MEASURES.derivative(trade, multicurve, order);
    assertTrue(totalSensitivity.equalWithTolerance(instrumentSensi, TOLERANCE_PV_DELTA));
  }

  @SuppressWarnings("unused")
  @Test(enabled = false)
  public void performance() {
    long start, end;
    int nbRep = 5;
    int nbTests = 10;

    for (int looprep = 0; looprep < nbRep; looprep++) {
      System.out.println("Calibration time");

      start = System.currentTimeMillis();
      for (int i = 0; i < nbTests; i++) {
        ImmutableRatesProvider multicurve1 =
            CALIBRATOR.calibrate(GROUP_DEFINITION_NO_INFO, MARKET_QUOTES, REF_DATA);
      }
      end = System.currentTimeMillis();
      System.out.println("  |--> calibration only: " + (end - start) + " ms for " + nbTests + " runs.");

      start = System.currentTimeMillis();
      for (int i = 0; i < nbTests; i++) {
        ImmutableRatesProvider multicurve1 =
            CALIBRATOR.calibrate(GROUP_DEFINITION, MARKET_QUOTES, REF_DATA);
      }
      end = System.currentTimeMillis();
      System.out.println("  |--> calibration and Jacobian: " + (end - start) + " ms for " + nbTests + " runs.");

      start = System.currentTimeMillis();
      for (int i = 0; i < nbTests; i++) {
        ImmutableRatesProvider multicurve1 =
            CALIBRATOR.calibrate(GROUP_DEFINITION_PV_SENSI, MARKET_QUOTES, REF_DATA);
      }
      end = System.currentTimeMillis();
      System.out.println("  |--> calibration, Jacobian and PV sensi MQ: " + (end - start) + " ms for " + nbTests + " runs.");
    }

  }

}
