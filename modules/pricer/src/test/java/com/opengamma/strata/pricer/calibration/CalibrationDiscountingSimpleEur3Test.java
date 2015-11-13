/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.calibration;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.market.ObservableValues;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.definition.CurveGroupDefinition;
import com.opengamma.strata.market.curve.definition.CurveGroupEntry;
import com.opengamma.strata.market.curve.definition.CurveNode;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.deposit.DiscountingIborFixingDepositProductPricer;
import com.opengamma.strata.pricer.rate.fra.DiscountingFraProductPricer;
import com.opengamma.strata.pricer.rate.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.rate.deposit.IborFixingDepositTrade;
import com.opengamma.strata.product.rate.fra.FraTrade;
import com.opengamma.strata.product.rate.swap.SwapTrade;

/**
 * Test curve calibration
 */
@Test
public class CalibrationDiscountingSimpleEur3Test {

  private static final LocalDate VALUATION_DATE = LocalDate.of(2015, 7, 24);

  /** Data for EUR-DSCON curve */
  /* Market values */
  private static final double[] DSC_MARKET_QUOTES = new double[] {
      0.0010, 0.0020, 0.0030, 0.0040};
  /* Tenors */
  private static final Period[] DSC_OIS_TENORS = new Period[] {
      Period.ofYears(2), Period.ofYears(5), Period.ofYears(10), Period.ofYears(30)};

  /** Data for EUR-EURIBOR3M curve */
  /* Market values */
  private static final double FWD3_FIXING_QUOTE = 0.0050;
  private static final double[] FWD3_FRA_QUOTES = new double[] {0.0051, 0.0052, 0.0053};
  private static final double[] FWD3_IRS_QUOTES = new double[] {0.0054, 0.0055, 0.0056, 0.0057};
  /* Tenors */
  private static final Period[] FWD3_FRA_TENORS = new Period[] { // Period to start
      Period.ofMonths(3), Period.ofMonths(6), Period.ofMonths(9)};
  private static final Period[] FWD3_IRS_TENORS = new Period[] {
      Period.ofYears(2), Period.ofYears(5), Period.ofYears(10), Period.ofYears(30)};

  /** Data for EUR-EURIBOR6M curve */
  /* Market values */
  private static final double FWD6_FIXING_QUOTE = 0.001;
  private static final double[] FWD6_FRA_QUOTES = new double[] {0.011, 0.012};
  private static final double[] FWD6_IRS_QUOTES = new double[] {0.013, 0.014, 0.015, 0.016, 0.017};
  /* Tenors */
  private static final Period[] FWD6_FRA_TENORS = new Period[] { // Period to start
      Period.ofMonths(3), Period.ofMonths(6)};
  private static final Period[] FWD6_IRS_TENORS = new Period[] {
      Period.ofYears(2), Period.ofYears(3), Period.ofYears(5), Period.ofYears(10), Period.ofYears(30)};

  private static final DiscountingIborFixingDepositProductPricer PRICER_FIXING =
      DiscountingIborFixingDepositProductPricer.DEFAULT;
  private static final DiscountingFraProductPricer PRICER_FRA =
      DiscountingFraProductPricer.DEFAULT;
  private static final DiscountingSwapProductPricer SWAP_PRICER =
      DiscountingSwapProductPricer.DEFAULT;
  private static final MarketQuoteSensitivityCalculator MQC = MarketQuoteSensitivityCalculator.DEFAULT;

  // Constants
  private static final double TOLERANCE_PV = 1.0E-6;
  private static final double TOLERANCE_DELTA = 1.0E-10;

  //-------------------------------------------------------------------------
  public void calibration_present_value() {
    ImmutableRatesProvider result =
        CalibrationEurStandard.calibrateEurStandard(VALUATION_DATE,
            DSC_MARKET_QUOTES, DSC_OIS_TENORS,
            FWD3_FIXING_QUOTE, FWD3_FRA_QUOTES, FWD3_IRS_QUOTES, FWD3_FRA_TENORS, FWD3_IRS_TENORS,
            FWD6_FIXING_QUOTE, FWD6_FRA_QUOTES, FWD6_IRS_QUOTES, FWD6_FRA_TENORS, FWD6_IRS_TENORS);

    /* Curve Discounting/EUR-EONIA */
    String[] dscIdValues = CalibrationEurStandard.dscIdValues(DSC_OIS_TENORS);
    /* Curve EUR-EURIBOR-3M */
    double[] fwd3MarketQuotes = CalibrationEurStandard.fwdMarketQuotes(FWD3_FIXING_QUOTE, FWD3_FRA_QUOTES, FWD3_IRS_QUOTES);
    String[] fwd3IdValue =
        CalibrationEurStandard.fwdIdValue(3, FWD3_FIXING_QUOTE, FWD3_FRA_QUOTES, FWD3_IRS_QUOTES, FWD3_FRA_TENORS,
            FWD3_IRS_TENORS);
    /* Curve EUR-EURIBOR-6M */
    double[] fwd6MarketQuotes = CalibrationEurStandard.fwdMarketQuotes(FWD6_FIXING_QUOTE, FWD6_FRA_QUOTES, FWD6_IRS_QUOTES);
    String[] fwd6IdValue =
        CalibrationEurStandard.fwdIdValue(6, FWD6_FIXING_QUOTE, FWD6_FRA_QUOTES, FWD6_IRS_QUOTES, FWD6_FRA_TENORS,
            FWD6_IRS_TENORS);
    /* All quotes for the curve calibration */
    ObservableValues allQuotes =
        CalibrationEurStandard.allQuotes(
            DSC_MARKET_QUOTES, dscIdValues, fwd3MarketQuotes, fwd3IdValue, fwd6MarketQuotes, fwd6IdValue);
    /* All nodes by groups. */
    CurveGroupDefinition config = CalibrationEurStandard.config(DSC_OIS_TENORS, dscIdValues,
        FWD3_FRA_TENORS, FWD3_IRS_TENORS, fwd3IdValue, FWD6_FRA_TENORS, FWD6_IRS_TENORS, fwd6IdValue);

    ImmutableList<CurveGroupEntry> entries = config.getEntries();
    // Test PV Dsc
    ImmutableList<CurveNode> dscNodes = entries.get(0).getCurveDefinition().getNodes();
    List<Trade> dscTrades = new ArrayList<>();
    for (int i = 0; i < dscNodes.size(); i++) {
      dscTrades.add(dscNodes.get(i).trade(VALUATION_DATE, allQuotes));
    }
    // OIS
    for (int i = 0; i < DSC_MARKET_QUOTES.length; i++) {
      MultiCurrencyAmount pvIrs = SWAP_PRICER
          .presentValue(((SwapTrade) dscTrades.get(i)).getProduct(), result);
      assertEquals(pvIrs.getAmount(EUR).getAmount(), 0.0, TOLERANCE_PV);
    }
    // Test PV Fwd3
    ImmutableList<CurveNode> fwd3Nodes = entries.get(1).getCurveDefinition().getNodes();
    List<Trade> fwd3Trades = new ArrayList<>();
    for (int i = 0; i < fwd3Nodes.size(); i++) {
      fwd3Trades.add(fwd3Nodes.get(i).trade(VALUATION_DATE, allQuotes));
    }
    // FRA
    for (int i = 0; i < FWD3_FRA_QUOTES.length; i++) {
      CurrencyAmount pvFra = PRICER_FRA
          .presentValue(((FraTrade) fwd3Trades.get(i + 1)).getProduct(), result);
      assertEquals(pvFra.getAmount(), 0.0, TOLERANCE_PV);
    }
    // IRS
    for (int i = 0; i < FWD3_IRS_QUOTES.length; i++) {
      MultiCurrencyAmount pvIrs = SWAP_PRICER
          .presentValue(((SwapTrade) fwd3Trades.get(i + 1 + FWD3_FRA_QUOTES.length)).getProduct(), result);
      assertEquals(pvIrs.getAmount(EUR).getAmount(), 0.0, TOLERANCE_PV);
    }
    // Test PV Fwd6
    ImmutableList<CurveNode> fwd6Nodes = entries.get(2).getCurveDefinition().getNodes();
    List<Trade> fwd6Trades = new ArrayList<>();
    for (int i = 0; i < fwd6Nodes.size(); i++) {
      fwd6Trades.add(fwd6Nodes.get(i).trade(VALUATION_DATE, allQuotes));
    }
    // IRS
    for (int i = 0; i < FWD6_IRS_QUOTES.length; i++) {
      MultiCurrencyAmount pvIrs = SWAP_PRICER
          .presentValue(((SwapTrade) fwd6Trades.get(i + 1 + FWD6_FRA_QUOTES.length)).getProduct(), result);
      assertEquals(pvIrs.getAmount(EUR).getAmount(), 0.0, TOLERANCE_PV);
    }
  }

  //-------------------------------------------------------------------------
  public void calibration_transition_coherence_par_rate() {
    ImmutableRatesProvider provider =
        CalibrationEurStandard.calibrateEurStandard(VALUATION_DATE,
            DSC_MARKET_QUOTES, DSC_OIS_TENORS,
            FWD3_FIXING_QUOTE, FWD3_FRA_QUOTES, FWD3_IRS_QUOTES, FWD3_FRA_TENORS, FWD3_IRS_TENORS,
            FWD6_FIXING_QUOTE, FWD6_FRA_QUOTES, FWD6_IRS_QUOTES, FWD6_FRA_TENORS, FWD6_IRS_TENORS);

    /* Curve Discounting/EUR-EONIA */
    String[] dscIdValues = CalibrationEurStandard.dscIdValues(DSC_OIS_TENORS);
    /* Curve EUR-EURIBOR-3M */
    double[] fwd3MarketQuotes = CalibrationEurStandard.fwdMarketQuotes(FWD3_FIXING_QUOTE, FWD3_FRA_QUOTES, FWD3_IRS_QUOTES);
    String[] fwd3IdValue =
        CalibrationEurStandard.fwdIdValue(3, FWD3_FIXING_QUOTE, FWD3_FRA_QUOTES, FWD3_IRS_QUOTES, FWD3_FRA_TENORS,
            FWD3_IRS_TENORS);
    /* Curve EUR-EURIBOR-6M */
    double[] fwd6MarketQuotes = CalibrationEurStandard.fwdMarketQuotes(FWD6_FIXING_QUOTE, FWD6_FRA_QUOTES, FWD6_IRS_QUOTES);
    String[] fwd6IdValue =
        CalibrationEurStandard.fwdIdValue(6, FWD6_FIXING_QUOTE, FWD6_FRA_QUOTES, FWD6_IRS_QUOTES, FWD6_FRA_TENORS,
            FWD6_IRS_TENORS);
    /* All quotes for the curve calibration */
    ObservableValues allQuotes =
        CalibrationEurStandard.allQuotes(DSC_MARKET_QUOTES, dscIdValues, fwd3MarketQuotes, fwd3IdValue, fwd6MarketQuotes,
            fwd6IdValue);
    /* All nodes by groups. */
    CurveGroupDefinition config = CalibrationEurStandard.config(DSC_OIS_TENORS, dscIdValues,
        FWD3_FRA_TENORS, FWD3_IRS_TENORS, fwd3IdValue, FWD6_FRA_TENORS, FWD6_IRS_TENORS, fwd6IdValue);

    ImmutableList<CurveGroupEntry> entries = config.getEntries();
    // Test PV Dsc
    ImmutableList<CurveNode> dscNodes = entries.get(0).getCurveDefinition().getNodes();
    List<Trade> dscTrades = new ArrayList<>();
    for (int i = 0; i < dscNodes.size(); i++) {
      dscTrades.add(dscNodes.get(i).trade(VALUATION_DATE, allQuotes));
    }
    // OIS
    for (int loopnode = 0; loopnode < DSC_MARKET_QUOTES.length; loopnode++) {
      PointSensitivities pts = SWAP_PRICER
          .parRateSensitivity(((SwapTrade) dscTrades.get(loopnode)).getProduct(), provider).build();
      CurveCurrencyParameterSensitivities ps = provider.curveParameterSensitivity(pts);
      CurveCurrencyParameterSensitivities mqs = MQC.sensitivity(ps, provider);
      assertEquals(mqs.size(), 3); // Calibration of all curves simultaneously
      CurveCurrencyParameterSensitivity mqsDsc = mqs.getSensitivity(CalibrationEurStandard.DSCON_CURVE_NAME, EUR);
      assertTrue(mqsDsc.getCurveName().equals(CalibrationEurStandard.DSCON_CURVE_NAME));
      assertTrue(mqsDsc.getCurrency().equals(EUR));
      DoubleArray mqsData = mqsDsc.getSensitivity();
      assertEquals(mqsData.size(), DSC_MARKET_QUOTES.length);
      for (int i = 0; i < mqsData.size(); i++) {
        assertEquals(mqsData.get(i), (i == loopnode) ? 1.0 : 0.0, TOLERANCE_DELTA);
      }
    }
    // Test PV Fwd3
    ImmutableList<CurveNode> fwd3Nodes = entries.get(1).getCurveDefinition().getNodes();
    List<Trade> fwd3Trades = new ArrayList<>();
    for (int i = 0; i < fwd3Nodes.size(); i++) {
      fwd3Trades.add(fwd3Nodes.get(i).trade(VALUATION_DATE, allQuotes));
    }
    for (int loopnode = 0; loopnode < fwd3MarketQuotes.length; loopnode++) {
      PointSensitivities pts = null;
      if (fwd3Trades.get(loopnode) instanceof IborFixingDepositTrade) {
        pts = PRICER_FIXING
            .parSpreadSensitivity(((IborFixingDepositTrade) fwd3Trades.get(loopnode)).getProduct(), provider);
      }
      if (fwd3Trades.get(loopnode) instanceof FraTrade) {
        pts = PRICER_FRA
            .parSpreadSensitivity(((FraTrade) fwd3Trades.get(loopnode)).getProduct(), provider);
      }
      if (fwd3Trades.get(loopnode) instanceof SwapTrade) {
        pts = SWAP_PRICER
            .parSpreadSensitivity(((SwapTrade) fwd3Trades.get(loopnode)).getProduct(), provider).build();
      }
      CurveCurrencyParameterSensitivities ps = provider.curveParameterSensitivity(pts);
      CurveCurrencyParameterSensitivities mqs = MQC.sensitivity(ps, provider);
      assertEquals(mqs.size(), 3);  // Calibration of all curves simultaneously
      CurveCurrencyParameterSensitivity mqsDsc = mqs.getSensitivity(CalibrationEurStandard.DSCON_CURVE_NAME, EUR);
      CurveCurrencyParameterSensitivity mqsFwd3 = mqs.getSensitivity(CalibrationEurStandard.FWD3_CURVE_NAME, EUR);
      DoubleArray mqsDscData = mqsDsc.getSensitivity();
      assertEquals(mqsDscData.size(), DSC_MARKET_QUOTES.length);
      for (int i = 0; i < mqsDscData.size(); i++) {
        assertEquals(mqsDscData.get(i), 0.0, TOLERANCE_DELTA);
      }
      DoubleArray mqsFwd3Data = mqsFwd3.getSensitivity();
      assertEquals(mqsFwd3Data.size(), fwd3MarketQuotes.length);
      for (int i = 0; i < mqsFwd3Data.size(); i++) {
        assertEquals(mqsFwd3Data.get(i), (i == loopnode) ? 1.0 : 0.0, TOLERANCE_DELTA);
      }
    }
    // Test PV Fwd6
    ImmutableList<CurveNode> fwd6Nodes = entries.get(2).getCurveDefinition().getNodes();
    List<Trade> fwd6Trades = new ArrayList<>();
    for (int i = 0; i < fwd6Nodes.size(); i++) {
      fwd6Trades.add(fwd6Nodes.get(i).trade(VALUATION_DATE, allQuotes));
    }
    for (int loopnode = 0; loopnode < fwd6MarketQuotes.length; loopnode++) {
      PointSensitivities pts = null;
      if (fwd6Trades.get(loopnode) instanceof IborFixingDepositTrade) {
        pts = PRICER_FIXING
            .parSpreadSensitivity(((IborFixingDepositTrade) fwd6Trades.get(loopnode)).getProduct(), provider);
      }
      if (fwd6Trades.get(loopnode) instanceof FraTrade) {
        pts = PRICER_FRA
            .parSpreadSensitivity(((FraTrade) fwd6Trades.get(loopnode)).getProduct(), provider);
      }
      if (fwd6Trades.get(loopnode) instanceof SwapTrade) {
        pts = SWAP_PRICER
            .parSpreadSensitivity(((SwapTrade) fwd6Trades.get(loopnode)).getProduct(), provider).build();
      }
      CurveCurrencyParameterSensitivities ps = provider.curveParameterSensitivity(pts);
      CurveCurrencyParameterSensitivities mqs = MQC.sensitivity(ps, provider);
      assertEquals(mqs.size(), 3);
      CurveCurrencyParameterSensitivity mqsDsc = mqs.getSensitivity(CalibrationEurStandard.DSCON_CURVE_NAME, EUR);
      CurveCurrencyParameterSensitivity mqsFwd3 = mqs.getSensitivity(CalibrationEurStandard.FWD3_CURVE_NAME, EUR);
      CurveCurrencyParameterSensitivity mqsFwd6 = mqs.getSensitivity(CalibrationEurStandard.FWD6_CURVE_NAME, EUR);
      DoubleArray mqsDscData = mqsDsc.getSensitivity();
      assertEquals(mqsDscData.size(), DSC_MARKET_QUOTES.length);
      for (int i = 0; i < mqsDscData.size(); i++) {
        assertEquals(mqsDscData.get(i), 0.0, TOLERANCE_DELTA);
      }
      DoubleArray mqsFwd3Data = mqsFwd3.getSensitivity();
      assertEquals(mqsFwd3Data.size(), fwd3MarketQuotes.length);
      for (int i = 0; i < mqsFwd3Data.size(); i++) {
        assertEquals(mqsFwd3Data.get(i), 0.0, TOLERANCE_DELTA);
      }
      DoubleArray mqsFwd6Data = mqsFwd6.getSensitivity();
      assertEquals(mqsFwd6Data.size(), fwd6MarketQuotes.length);
      for (int i = 0; i < mqsFwd6Data.size(); i++) {
        assertEquals(mqsFwd6Data.get(i), (i == loopnode) ? 1.0 : 0.0, TOLERANCE_DELTA);
      }
    }

  }

}
