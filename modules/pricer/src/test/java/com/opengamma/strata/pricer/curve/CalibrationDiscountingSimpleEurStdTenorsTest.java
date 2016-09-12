/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;
import static com.opengamma.strata.basics.index.OvernightIndices.EUR_EONIA;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_3M;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M;
import static com.opengamma.strata.product.swap.type.FixedOvernightSwapConventions.EUR_FIXED_1Y_EONIA_OIS;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.ImmutableMarketDataBuilder;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.InterpolatedNodalCurveDefinition;
import com.opengamma.strata.market.curve.NodalCurveDefinition;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.curve.node.FixedIborSwapCurveNode;
import com.opengamma.strata.market.curve.node.FixedOvernightSwapCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.ResolvedTrade;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;
import com.opengamma.strata.product.swap.type.FixedIborSwapTemplate;
import com.opengamma.strata.product.swap.type.FixedOvernightSwapTemplate;

/**
 * Test curve calibration
 */
@Test
public class CalibrationDiscountingSimpleEurStdTenorsTest {

  private static final LocalDate VAL_DATE = LocalDate.of(2015, 7, 24);

  private static final CurveInterpolator INTERPOLATOR_LINEAR = CurveInterpolators.LINEAR;
  private static final CurveExtrapolator EXTRAPOLATOR_FLAT = CurveExtrapolators.FLAT;
  private static final DayCount CURVE_DC = ACT_365F;

  // reference data
  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final String SCHEME = "CALIBRATION";

  /** Curve names */
  private static final String DSCON_NAME = "EUR_EONIA_EOD";
  private static final CurveName DSCON_CURVE_NAME = CurveName.of(DSCON_NAME);
  private static final String FWD3_NAME = "EUR_EURIBOR_3M";
  private static final CurveName FWD3_CURVE_NAME = CurveName.of(FWD3_NAME);
  private static final String FWD6_NAME = "EUR_EURIBOR_6M";
  private static final CurveName FWD6_CURVE_NAME = CurveName.of(FWD6_NAME);

  /** Data for EUR-DSCON curve */
  /* Market values */
  private static final double[] DSC_MARKET_QUOTES = new double[] {
      -0.0010787505441382185, 0.0016443214916477351, 0.00791319942756944, 0.014309183236345927};
  private static final int DSC_NB_NODES = DSC_MARKET_QUOTES.length;
  private static final String[] DSC_ID_VALUE = new String[] {
      "OIS2Y", "OIS5Y", "OIS10Y", "OIS30Y"};
  /* Nodes */
  private static final CurveNode[] DSC_NODES = new CurveNode[DSC_NB_NODES];
  /* Tenors */
  private static final Period[] DSC_OIS_TENORS = new Period[] {
      Period.ofYears(2), Period.ofYears(5), Period.ofYears(10), Period.ofYears(30)};
  private static final int DSC_NB_OIS_NODES = DSC_OIS_TENORS.length;
  static {
    for (int i = 0; i < DSC_NB_OIS_NODES; i++) {
      DSC_NODES[i] = FixedOvernightSwapCurveNode.of(
          FixedOvernightSwapTemplate.of(Period.ZERO, Tenor.of(DSC_OIS_TENORS[i]), EUR_FIXED_1Y_EONIA_OIS),
          QuoteId.of(StandardId.of(SCHEME, DSC_ID_VALUE[i])));
    }
  }

  /** Data for EUR-LIBOR3M curve */
  /* Market values */
  private static final double[] FWD3_MARKET_QUOTES = new double[] {
      0.00013533281680009178, 0.0031298573232152152, 0.009328861288116275, 0.015219571759282416};
  private static final int FWD3_NB_NODES = FWD3_MARKET_QUOTES.length;
  private static final String[] FWD3_ID_VALUE = new String[] {
      "IRS3M_2Y", "IRS3M_5Y", "IRS3M_10Y", "IRS3M_30Y"};
  /* Nodes */
  private static final CurveNode[] FWD3_NODES = new CurveNode[FWD3_NB_NODES];
  /* Tenors */
  private static final Period[] FWD3_IRS_TENORS = new Period[] {
      Period.ofYears(2), Period.ofYears(5), Period.ofYears(10), Period.ofYears(30)};
  private static final int FWD3_NB_IRS_NODES = FWD3_IRS_TENORS.length;
  static {
    for (int i = 0; i < FWD3_NB_IRS_NODES; i++) {
      FWD3_NODES[i] = FixedIborSwapCurveNode.of(
          FixedIborSwapTemplate.of(Period.ZERO, Tenor.of(FWD3_IRS_TENORS[i]), EUR_FIXED_1Y_EURIBOR_3M),
          QuoteId.of(StandardId.of(SCHEME, FWD3_ID_VALUE[i])));
    }
  }

  /** Data for EUR-EURIBOR6M curve */
  /* Market values */
  private static final double[] FWD6_MARKET_QUOTES = new double[] {
      0.00013533281680009178, 0.0031298573232152152, 0.009328861288116275, 0.015219571759282416};
  private static final int FWD6_NB_NODES = FWD3_MARKET_QUOTES.length;
  private static final String[] FWD6_ID_VALUE = new String[] {
      "IRS6M_2Y", "IRS6M_5Y", "IRS6M_10Y", "IRS6M_30Y"};
  /* Nodes */
  private static final CurveNode[] FWD6_NODES = new CurveNode[FWD3_NB_NODES];
  /* Tenors */
  private static final Period[] FWD6_IRS_TENORS = new Period[] {
      Period.ofYears(2), Period.ofYears(5), Period.ofYears(10), Period.ofYears(30)};
  private static final int FWD6_NB_IRS_NODES = FWD6_IRS_TENORS.length;
  static {
    for (int i = 0; i < FWD6_NB_IRS_NODES; i++) {
      FWD6_NODES[i] = FixedIborSwapCurveNode.of(
          FixedIborSwapTemplate.of(Period.ZERO, Tenor.of(FWD6_IRS_TENORS[i]), EUR_FIXED_1Y_EURIBOR_6M),
          QuoteId.of(StandardId.of(SCHEME, FWD6_ID_VALUE[i])));
    }
  }

  /** All quotes for the curve calibration */
  private static final MarketData ALL_QUOTES;
  static {
    ImmutableMarketDataBuilder builder = ImmutableMarketData.builder(VAL_DATE);
    for (int i = 0; i < DSC_NB_NODES; i++) {
      builder.addValue(QuoteId.of(StandardId.of(SCHEME, DSC_ID_VALUE[i])), DSC_MARKET_QUOTES[i]);
    }
    for (int i = 0; i < FWD3_NB_NODES; i++) {
      builder.addValue(QuoteId.of(StandardId.of(SCHEME, FWD3_ID_VALUE[i])), FWD3_MARKET_QUOTES[i]);
    }
    for (int i = 0; i < FWD6_NB_NODES; i++) {
      builder.addValue(QuoteId.of(StandardId.of(SCHEME, FWD6_ID_VALUE[i])), FWD6_MARKET_QUOTES[i]);
    }
    ALL_QUOTES = builder.build();
  }

  private static final DiscountingSwapProductPricer SWAP_PRICER =
      DiscountingSwapProductPricer.DEFAULT;

  private static final CurveCalibrator CALIBRATOR = CurveCalibrator.of(1e-9, 1e-9, 100);

  // Constants
  private static final double TOLERANCE_PV = 1.0E-6;

  /** Test with CurveGroupDefinition */
  private static final String CURVE_GROUP_NAME_STR = "EUR-DSCON-EURIBOR3M-EURIBOR6M";
  private static final CurveGroupName CURVE_GROUP_NAME = CurveGroupName.of(CURVE_GROUP_NAME_STR);
  private static final InterpolatedNodalCurveDefinition DSC_CURVE_DEFN =
      InterpolatedNodalCurveDefinition.builder()
          .name(DSCON_CURVE_NAME)
          .xValueType(ValueType.YEAR_FRACTION)
          .yValueType(ValueType.ZERO_RATE)
          .dayCount(CURVE_DC)
          .interpolator(INTERPOLATOR_LINEAR)
          .extrapolatorLeft(EXTRAPOLATOR_FLAT)
          .extrapolatorRight(EXTRAPOLATOR_FLAT)
          .nodes(DSC_NODES).build();
  private static final InterpolatedNodalCurveDefinition FWD3_CURVE_DEFN =
      InterpolatedNodalCurveDefinition.builder()
          .name(FWD3_CURVE_NAME)
          .xValueType(ValueType.YEAR_FRACTION)
          .yValueType(ValueType.ZERO_RATE)
          .dayCount(CURVE_DC)
          .interpolator(INTERPOLATOR_LINEAR)
          .extrapolatorLeft(EXTRAPOLATOR_FLAT)
          .extrapolatorRight(EXTRAPOLATOR_FLAT)
          .nodes(FWD3_NODES).build();
  private static final InterpolatedNodalCurveDefinition FWD6_CURVE_DEFN =
      InterpolatedNodalCurveDefinition.builder()
          .name(FWD6_CURVE_NAME)
          .xValueType(ValueType.YEAR_FRACTION)
          .yValueType(ValueType.ZERO_RATE)
          .dayCount(CURVE_DC)
          .interpolator(INTERPOLATOR_LINEAR)
          .extrapolatorLeft(EXTRAPOLATOR_FLAT)
          .extrapolatorRight(EXTRAPOLATOR_FLAT)
          .nodes(FWD3_NODES).build();
  private static final CurveGroupDefinition CURVE_GROUP_CONFIG =
      CurveGroupDefinition.builder()
          .name(CURVE_GROUP_NAME)
          .addCurve(DSC_CURVE_DEFN, EUR, EUR_EONIA)
          .addForwardCurve(FWD3_CURVE_DEFN, EUR_EURIBOR_3M)
          .addForwardCurve(FWD6_CURVE_DEFN, EUR_EURIBOR_6M).build();

  //-------------------------------------------------------------------------
  public void calibration_present_value() {
    RatesProvider result =
        CALIBRATOR.calibrate(CURVE_GROUP_CONFIG, ALL_QUOTES, REF_DATA);

    ImmutableList<NodalCurveDefinition> definitions = CURVE_GROUP_CONFIG.getCurveDefinitions();
    // Test PV Dsc
    ImmutableList<CurveNode> dscNodes = definitions.get(0).getNodes();
    List<ResolvedTrade> dscTrades = new ArrayList<>();
    for (int i = 0; i < dscNodes.size(); i++) {
      dscTrades.add(dscNodes.get(i).resolvedTrade(1d, ALL_QUOTES, REF_DATA));
    }
    // OIS
    for (int i = 0; i < DSC_NB_OIS_NODES; i++) {
      MultiCurrencyAmount pvIrs = SWAP_PRICER
          .presentValue(((ResolvedSwapTrade) dscTrades.get(i)).getProduct(), result);
      assertEquals(pvIrs.getAmount(EUR).getAmount(), 0.0, TOLERANCE_PV);
    }
    // Test PV Fwd3
    ImmutableList<CurveNode> fwd3Nodes = definitions.get(1).getNodes();
    List<ResolvedTrade> fwd3Trades = new ArrayList<>();
    for (int i = 0; i < fwd3Nodes.size(); i++) {
      fwd3Trades.add(fwd3Nodes.get(i).resolvedTrade(1d, ALL_QUOTES, REF_DATA));
    }
    // IRS
    for (int i = 0; i < FWD3_NB_IRS_NODES; i++) {
      MultiCurrencyAmount pvIrs = SWAP_PRICER
          .presentValue(((ResolvedSwapTrade) fwd3Trades.get(i)).getProduct(), result);
      assertEquals(pvIrs.getAmount(EUR).getAmount(), 0.0, TOLERANCE_PV);
    }
    // Test PV Fwd6
    ImmutableList<CurveNode> fwd6Nodes = definitions.get(2).getNodes();
    List<ResolvedTrade> fwd6Trades = new ArrayList<>();
    for (int i = 0; i < fwd6Nodes.size(); i++) {
      fwd6Trades.add(fwd6Nodes.get(i).resolvedTrade(1d, ALL_QUOTES, REF_DATA));
    }
    // IRS
    for (int i = 0; i < FWD6_NB_IRS_NODES; i++) {
      MultiCurrencyAmount pvIrs = SWAP_PRICER
          .presentValue(((ResolvedSwapTrade) fwd6Trades.get(i)).getProduct(), result);
      assertEquals(pvIrs.getAmount(EUR).getAmount(), 0.0, TOLERANCE_PV);
    }
  }

}
