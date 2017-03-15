/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M;
import static com.opengamma.strata.product.swap.type.FixedOvernightSwapConventions.EUR_FIXED_1Y_EONIA_OIS;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.loader.csv.QuotesCsvLoader;
import com.opengamma.strata.loader.csv.RatesCalibrationCsvLoader;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveParameterSize;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.JacobianCalibrationMatrix;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.TenorParameterMetadata;
import com.opengamma.strata.pricer.curve.CalibrationMeasures;
import com.opengamma.strata.pricer.curve.CurveCalibrator;
import com.opengamma.strata.pricer.deposit.DiscountingIborFixingDepositProductPricer;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.ResolvedTrade;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.deposit.ResolvedIborFixingDepositTrade;
import com.opengamma.strata.product.deposit.type.IborFixingDepositConvention;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;

/**
 * Tests {@link CurveSensitivityUtils}.
 */
@Test
public class CurveSensitivityUtilsJacobianTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VALUATION_DATE = LocalDate.of(2015, 11, 20);

  // Configuration and data stored in csv to avoid long code description of the input data
  private static final String CONFIG_PATH = "src/test/resources/curve-config/";
  private static final String QUOTES_PATH = "src/test/resources/quotes/";

  // Quotes
  private static final CurveCalibrator CALIBRATOR = CurveCalibrator.standard();  
  private static final String QUOTES_FILE = "quotes-20151120-eur.csv";
  private static final Map<QuoteId, Double> MQ_INPUT = 
      QuotesCsvLoader.load(VALUATION_DATE, ImmutableList.of(ResourceLocator.of(QUOTES_PATH + QUOTES_FILE)));
  private static final ImmutableMarketData MARKET_QUOTES_INPUT = ImmutableMarketData.of(VALUATION_DATE, MQ_INPUT);
  
  // Group input based on IRS for EURIBOR6M  
  public static final CurveName EUR_SINGLE_NAME = CurveName.of("EUR-ALLIRS");
  private static final String GROUPS_IN_1_FILE = "EUR-ALLIRS-group.csv";
  private static final String SETTINGS_IN_1_FILE = "EUR-ALLIRS-settings.csv";
  private static final String NODES_IN_1_FILE = "EUR-ALLIRS-STD-nodes.csv";
  private static final CurveGroupDefinition GROUPS_IN_1 =
      RatesCalibrationCsvLoader.load(
          ResourceLocator.of(CONFIG_PATH + GROUPS_IN_1_FILE),
          ResourceLocator.of(CONFIG_PATH + SETTINGS_IN_1_FILE),
          ResourceLocator.of(CONFIG_PATH + NODES_IN_1_FILE)).get(CurveGroupName.of("EUR-SINGLE"));
  private static final RatesProvider MULTICURVE_EUR_SINGLE_CALIBRATED =
      CALIBRATOR.calibrate(GROUPS_IN_1, MARKET_QUOTES_INPUT, REF_DATA);
  
  public static final CalibrationMeasures MARKET_QUOTE = CalibrationMeasures.MARKET_QUOTE;
  
  public static final DiscountingSwapProductPricer PRICER_SWAP_PRODUCT = DiscountingSwapProductPricer.DEFAULT;
  public static final DiscountingIborFixingDepositProductPricer PRICER_IBORFIX_PRODUCT =
      DiscountingIborFixingDepositProductPricer.DEFAULT;


  public static final DoubleArray TIME_EUR = 
      DoubleArray.of(1.0d/365.0d, 1.0d/12d, 0.25, 0.50, 1.00, 2.00, 3.00, 4.00, 5.00, 7.00, 10.0, 15.0, 20.0, 30.0);
  public static final ImmutableRatesProvider MULTICURVE_EUR_SINGLE_INPUT;
  static {
    Tenor[] tenors = new Tenor[] {Tenor.TENOR_1D, Tenor.TENOR_1M, Tenor.TENOR_3M, Tenor.TENOR_6M,
        Tenor.TENOR_1Y, Tenor.TENOR_2Y, Tenor.TENOR_3Y, Tenor.TENOR_4Y, Tenor.TENOR_5Y, 
        Tenor.TENOR_7Y, Tenor.TENOR_10Y, Tenor.TENOR_15Y, Tenor.TENOR_20Y, Tenor.TENOR_30Y};
    List<TenorParameterMetadata> metadataList = new ArrayList<>();
    for(int looptenor=0; looptenor< tenors.length; looptenor++) {
      metadataList.add(TenorParameterMetadata.of(tenors[looptenor]));
    }
    DoubleArray rate_eur = 
        DoubleArray.of(0.0160, 0.0165, 0.0155, 0.0155, 0.0155, 0.0150, 0.0150, 0.0160, 0.0165, 0.0155, 0.0155, 0.0155, 0.0150, 0.0140);
    InterpolatedNodalCurve curve_single_eur = InterpolatedNodalCurve.builder()
        .metadata(DefaultCurveMetadata.builder()
            .curveName(EUR_SINGLE_NAME)
            .parameterMetadata(metadataList)
            .dayCount(ACT_365F)
            .xValueType(ValueType.YEAR_FRACTION)
            .yValueType(ValueType.ZERO_RATE).build())
        .xValues(TIME_EUR)
        .yValues(rate_eur)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .interpolator(CurveInterpolators.LINEAR)
        .build();
    MULTICURVE_EUR_SINGLE_INPUT = ImmutableRatesProvider.builder(VALUATION_DATE)
        .discountCurve(EUR, curve_single_eur)
        .iborIndexCurve(EUR_EURIBOR_6M, curve_single_eur)
        .build();
  }
  public static final List<CurveParameterSize> LIST_CURVE_NAMES_1 = new ArrayList<>();
  static {
    LIST_CURVE_NAMES_1.add(CurveParameterSize.of(EUR_SINGLE_NAME, TIME_EUR.size()));
  }

  private static final String OG_TICKER = "OG-Ticker";
  private static final Tenor[] TENORS_STD_1 = new Tenor[] {Tenor.TENOR_2Y, Tenor.TENOR_5Y, Tenor.TENOR_10Y, Tenor.TENOR_30Y};
  private static final String[] TICKERS_STD_1 = new String[] {"EUR-IRS6M-2Y", "EUR-IRS6M-5Y", "EUR-IRS6M-10Y", "EUR-IRS6M-30Y"};
  
  private static final double TOLERANCE_JAC = 1.0E-6;
  private static final double TOLERANCE_JAC_APPROX = 1.0E-2;
  
  /**
   * Calibrate a single curve to 4 points. Use the resulting calibrated curves as starting point of the computation 
   * of a Jacobian. Compare the direct Jacobian and the one reconstructed from trades.
   */
  public void direct_one_curve() {
    /* Create trades */
    List<ResolvedTrade> trades = new ArrayList<>();
    List<LocalDate> nodeDates = new ArrayList<>();
    for (int looptenor = 0; looptenor < TENORS_STD_1.length; looptenor++) {
      ResolvedSwapTrade t0 = EUR_FIXED_1Y_EURIBOR_6M
          .createTrade(VALUATION_DATE, TENORS_STD_1[looptenor], BuySell.BUY, 1.0, 0.0, REF_DATA).resolve(REF_DATA);
      double rate = MARKET_QUOTE.value(t0, MULTICURVE_EUR_SINGLE_CALIBRATED);
      ResolvedSwapTrade t = EUR_FIXED_1Y_EURIBOR_6M
          .createTrade(VALUATION_DATE, TENORS_STD_1[looptenor], BuySell.BUY, 1.0, rate, REF_DATA).resolve(REF_DATA);
      nodeDates.add(t.getProduct().getEndDate());
      trades.add(t);
    }
    /* Par rate sensitivity */
    Function<ResolvedTrade, CurrencyParameterSensitivities> sensitivityFunction =
        (t) -> MULTICURVE_EUR_SINGLE_CALIBRATED.parameterSensitivity(
                PRICER_SWAP_PRODUCT.parRateSensitivity(((ResolvedSwapTrade) t).getProduct(), MULTICURVE_EUR_SINGLE_CALIBRATED).build());
    DoubleMatrix jiComputed = 
        CurveSensitivityUtils.jacobianFromMarketQuoteSensitivities(LIST_CURVE_NAMES_1, trades, sensitivityFunction);
    DoubleMatrix jiExpected =
        MULTICURVE_EUR_SINGLE_CALIBRATED.findData(EUR_SINGLE_NAME).get().getMetadata().findInfo(CurveInfoType.JACOBIAN).get()
            .getJacobianMatrix();
    /* Comparison */
    assertEquals(jiComputed.rowCount() , jiExpected.rowCount());
    assertEquals(jiComputed.columnCount() , jiExpected.columnCount());
    for(int i=0; i<jiComputed.rowCount(); i++) {
      for(int j=0; j<jiComputed.columnCount(); j++) {
        assertEquals(jiComputed.get(i, j), jiExpected.get(i, j), TOLERANCE_JAC);
      }
    }    
  }

  /**
   * Start from a generic zero-coupon curve. Compute the (inverse) Jacobian matrix using linear projection to a small 
   * number of points and the Jacobian utility. Compare the direct Jacobian obtained by calibrating a curve
   * based on the trades with market quotes computed from the zero-coupon curve.
   */
  public void with_rebucketing_one_curve() {
    /* Create trades */
    List<ResolvedTrade> trades = new ArrayList<>();
    List<LocalDate> nodeDates = new ArrayList<>();
    double[] marketQuotes = new double[TENORS_STD_1.length];
    for (int looptenor = 0; looptenor < TENORS_STD_1.length; looptenor++) {
      ResolvedSwapTrade t0 = EUR_FIXED_1Y_EURIBOR_6M
          .createTrade(VALUATION_DATE, TENORS_STD_1[looptenor], BuySell.BUY, 1.0, 0.0, REF_DATA).resolve(REF_DATA);
      marketQuotes[looptenor] = MARKET_QUOTE.value(t0, MULTICURVE_EUR_SINGLE_INPUT);
      ResolvedSwapTrade t = EUR_FIXED_1Y_EURIBOR_6M
          .createTrade(VALUATION_DATE, TENORS_STD_1[looptenor], BuySell.BUY, 1.0, marketQuotes[looptenor], REF_DATA).resolve(REF_DATA);
      nodeDates.add(t.getProduct().getEndDate());
      trades.add(t);
    }
    Function<ResolvedTrade, CurrencyParameterSensitivities> sensitivityFunction =
        (t) -> CurveSensitivityUtils.linearRebucketing(
            MULTICURVE_EUR_SINGLE_INPUT.parameterSensitivity(
                PRICER_SWAP_PRODUCT.parRateSensitivity(((ResolvedSwapTrade) t).getProduct(), MULTICURVE_EUR_SINGLE_INPUT).build()),
            nodeDates, VALUATION_DATE);

    /* Market quotes for comparison */
    Map<QuoteId, Double> mqCmp = new HashMap<>();
    for (int looptenor = 0; looptenor < TENORS_STD_1.length; looptenor++) {
      mqCmp.put(QuoteId.of(StandardId.of(OG_TICKER, TICKERS_STD_1[looptenor])), marketQuotes[looptenor]);
    }
    ImmutableMarketData marketQuotesObject = ImmutableMarketData.of(VALUATION_DATE, mqCmp);
    RatesProvider multicurveCmp = CALIBRATOR.calibrate(GROUPS_IN_1, marketQuotesObject, REF_DATA);

    /* Comparison */
    DoubleMatrix jiComputed =
        CurveSensitivityUtils.jacobianFromMarketQuoteSensitivities(LIST_CURVE_NAMES_1, trades, sensitivityFunction);
    DoubleMatrix jiExpected = multicurveCmp
        .findData(EUR_SINGLE_NAME).get().getMetadata().findInfo(CurveInfoType.JACOBIAN).get().getJacobianMatrix();
    assertEquals(jiComputed.rowCount(), jiExpected.rowCount());
    assertEquals(jiComputed.columnCount(), jiExpected.columnCount());
    for (int i = 0; i < jiComputed.rowCount(); i++) {
      for (int j = 0; j < jiComputed.columnCount(); j++) {
        assertEquals(jiComputed.get(i, j), jiExpected.get(i, j), TOLERANCE_JAC_APPROX); 
        // The comparison is not perfect due to the incoherences introduced by the re-bucketing
      }
    }
  }
  

  // Group input based on OIS for DSC-ON and IRS for EURIBOR6M  
  public static final CurveName EUR_DSCON_OIS = CurveName.of("EUR-DSCON-OIS");
  public static final CurveName EUR_EURIBOR6M_IRS = CurveName.of("EUR-EURIBOR6M-IRS");
  private static final String GROUPS_IN_2_FILE = "EUR-DSCONOIS-E6IRS-group.csv";
  private static final String SETTINGS_IN_2_FILE = "EUR-DSCONOIS-E6IRS-settings.csv";
  private static final String NODES_IN_2_FILE = "EUR-DSCONOIS-E6IRS-STD-nodes.csv";
  private static final CurveGroupDefinition GROUPS_IN_2 =
      RatesCalibrationCsvLoader.load(
          ResourceLocator.of(CONFIG_PATH + GROUPS_IN_2_FILE),
          ResourceLocator.of(CONFIG_PATH + SETTINGS_IN_2_FILE),
          ResourceLocator.of(CONFIG_PATH + NODES_IN_2_FILE)).get(CurveGroupName.of("EUR-DSCONOIS-E6IRS"));
  private static final RatesProvider MULTICURVE_EUR_2_CALIBRATED =
      CALIBRATOR.calibrate(GROUPS_IN_2, MARKET_QUOTES_INPUT, REF_DATA);
  

  private static final Tenor[] TENORS_STD_2_OIS = new Tenor[] {
    Tenor.TENOR_1M, Tenor.TENOR_3M, Tenor.TENOR_6M, Tenor.TENOR_1Y, Tenor.TENOR_2Y, Tenor.TENOR_5Y, Tenor.TENOR_10Y, Tenor.TENOR_30Y};
  private static final Tenor[] TENORS_STD_2_IRS = new Tenor[] {
    Tenor.TENOR_1Y, Tenor.TENOR_2Y, Tenor.TENOR_5Y, Tenor.TENOR_10Y, Tenor.TENOR_30Y};
  
  /**
   * Calibrate a single curve to 4 points. Use the resulting calibrated curves as starting point of the computation 
   * of a Jacobian. Compare the direct Jacobian and the one reconstructed from trades.
   */
  public void direct_two_curves() {
    JacobianCalibrationMatrix jiObject = 
        MULTICURVE_EUR_2_CALIBRATED.findData(EUR_DSCON_OIS).get().getMetadata().findInfo(CurveInfoType.JACOBIAN).get();
    ImmutableList<CurveParameterSize> order = jiObject.getOrder(); // To obtain the order of the curves in the jacobian

    /* Create trades */
    List<ResolvedTrade> tradesDsc = new ArrayList<>();
    for (int looptenor = 0; looptenor < TENORS_STD_2_OIS.length; looptenor++) {
      ResolvedSwapTrade t0 = EUR_FIXED_1Y_EONIA_OIS
          .createTrade(VALUATION_DATE, TENORS_STD_2_OIS[looptenor], BuySell.BUY, 1.0, 0.0, REF_DATA).resolve(REF_DATA);
      double rate = MARKET_QUOTE.value(t0, MULTICURVE_EUR_2_CALIBRATED);
      ResolvedSwapTrade t = EUR_FIXED_1Y_EONIA_OIS
          .createTrade(VALUATION_DATE, TENORS_STD_2_OIS[looptenor], BuySell.BUY, 1.0, rate, REF_DATA).resolve(REF_DATA);
      tradesDsc.add(t);
    }
    List<ResolvedTrade> tradesE3 = new ArrayList<>();
    // Fixing
    IborFixingDepositConvention c = IborFixingDepositConvention.of(EUR_EURIBOR_6M);
    ResolvedIborFixingDepositTrade fix0 = c.createTrade(VALUATION_DATE, 
        EUR_EURIBOR_6M.getTenor().getPeriod(), BuySell.BUY, 1.0, 0.0, REF_DATA).resolve(REF_DATA);
    double rateFixing = MARKET_QUOTE.value(fix0, MULTICURVE_EUR_2_CALIBRATED);
    ResolvedIborFixingDepositTrade fix = c.createTrade(VALUATION_DATE, 
        EUR_EURIBOR_6M.getTenor().getPeriod(), BuySell.BUY, 1.0, rateFixing, REF_DATA).resolve(REF_DATA);
    tradesE3.add(fix);    
    // IRS
    for (int looptenor = 0; looptenor < TENORS_STD_2_IRS.length; looptenor++) {
      ResolvedSwapTrade t0 = EUR_FIXED_1Y_EURIBOR_6M
          .createTrade(VALUATION_DATE, TENORS_STD_2_IRS[looptenor], BuySell.BUY, 1.0, 0.0, REF_DATA).resolve(REF_DATA);
      double rate = MARKET_QUOTE.value(t0, MULTICURVE_EUR_2_CALIBRATED);
      ResolvedSwapTrade t = EUR_FIXED_1Y_EURIBOR_6M
          .createTrade(VALUATION_DATE, TENORS_STD_2_IRS[looptenor], BuySell.BUY, 1.0, rate, REF_DATA).resolve(REF_DATA);
      tradesE3.add(t);
    }
    List<ResolvedTrade> trades = new ArrayList<>();
    if (order.get(0).getName().equals(EUR_DSCON_OIS)) {
      trades.addAll(tradesDsc);
      trades.addAll(tradesE3);
    } else {
      trades.addAll(tradesE3);
      trades.addAll(tradesDsc);
    }
    /* Par rate sensitivity */
    Function<ResolvedTrade, CurrencyParameterSensitivities> sensitivityFunction =
        (t) -> MULTICURVE_EUR_2_CALIBRATED.parameterSensitivity(
            (t instanceof ResolvedSwapTrade) ?
                PRICER_SWAP_PRODUCT.parRateSensitivity(
                    ((ResolvedSwapTrade) t).getProduct(), MULTICURVE_EUR_2_CALIBRATED).build() :
                PRICER_IBORFIX_PRODUCT.parRateSensitivity(
                    ((ResolvedIborFixingDepositTrade) t).getProduct(), MULTICURVE_EUR_2_CALIBRATED));
    DoubleMatrix jiComputed =
        CurveSensitivityUtils.jacobianFromMarketQuoteSensitivities(order, trades, sensitivityFunction);
    DoubleMatrix jiExpectedDsc =
        MULTICURVE_EUR_2_CALIBRATED.findData(EUR_DSCON_OIS).get()
            .getMetadata().getInfo(CurveInfoType.JACOBIAN).getJacobianMatrix();
    DoubleMatrix jiExpectedE3 =
        MULTICURVE_EUR_2_CALIBRATED.findData(EUR_EURIBOR6M_IRS).get()
            .getMetadata().getInfo(CurveInfoType.JACOBIAN).getJacobianMatrix();
    /* Comparison */
    assertEquals(jiComputed.rowCount(), jiExpectedDsc.rowCount() + jiExpectedE3.rowCount());
    assertEquals(jiComputed.columnCount(), jiExpectedDsc.columnCount());
    assertEquals(jiComputed.columnCount(), jiExpectedE3.columnCount());
    int shiftDsc = order.get(0).getName().equals(EUR_DSCON_OIS) ? 0 : jiExpectedE3.rowCount();
    for (int i = 0; i < jiExpectedDsc.rowCount(); i++) {
      for (int j = 0; j < jiExpectedDsc.columnCount(); j++) {
        assertEquals(jiComputed.get(i + shiftDsc, j), jiExpectedDsc.get(i, j), TOLERANCE_JAC);
      }
    }
    int shiftE3 = order.get(0).getName().equals(EUR_DSCON_OIS) ? jiExpectedDsc.rowCount() : 0;
    for (int i = 0; i < jiExpectedE3.rowCount(); i++) {
      for (int j = 0; j < jiExpectedDsc.columnCount(); j++) {
        assertEquals(jiComputed.get(i + shiftE3, j), jiExpectedE3.get(i, j), TOLERANCE_JAC);
      }
    }
  }

}
