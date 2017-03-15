/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.GBP_FIXED_6M_LIBOR_6M;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.ImmutableMarketDataBuilder;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.ParameterizedFunctionalCurveDefinition;
import com.opengamma.strata.market.curve.node.FixedIborSwapCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.math.impl.interpolation.SmithWilsonCurveFunction;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.ResolvedTrade;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;
import com.opengamma.strata.product.swap.type.FixedIborSwapTemplate;

/**
 * Curve calibration example with {@link SmithWilsonCurveFunction}.
 */
@Test
public class CalibrationDiscountingSmithWilsonTest {

  private static final LocalDate VAL_DATE = LocalDate.of(2015, 7, 21);
  private static final DayCount CURVE_DC = ACT_365F;
  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final String SCHEME = "CALIBRATION";

  /** Curve name */
  private static final CurveName CURVE_NAME = CurveName.of("GBP-ALL-IRS6M");
  /** Curves associations to currencies and indices. */
  private static final Map<CurveName, Currency> DSC_NAMES = new HashMap<>();
  private static final Map<CurveName, Set<Index>> IDX_NAMES = new HashMap<>();
  private static final Set<Index> IBOR_INDICES = new HashSet<>();
  static {
    IBOR_INDICES.add(GBP_LIBOR_6M);
    DSC_NAMES.put(CURVE_NAME, GBP);
    IDX_NAMES.put(CURVE_NAME, IBOR_INDICES);
  }

  /** Market values */
  private static final double[] FWD6_MARKET_QUOTES = new double[] {
      0.0273403667327403, 0.0327205345299401, 0.0336112121443886, 0.0346854377006694, 0.0395043823351044, 0.0425511326946310,
      0.0475939564387996};
  private static final int FWD6_NB_NODES = FWD6_MARKET_QUOTES.length;
  private static final String[] FWD6_ID_VALUE = new String[] {
      "IRS1Y", "IRS3Y", "IRS5Y", "IRS7Y", "IRS10Y", "IRS15Y", "IRS20Y", "IRS30Y"};
  /** Nodes for the Fwd 3M GBP curve */
  private static final CurveNode[] ALL_NODES = new CurveNode[FWD6_NB_NODES];
  private static final double[] NODE_TIMES = new double[FWD6_NB_NODES];
  /** Tenors for the Fwd 3M GBP swaps */
  private static final Period[] FWD6_IRS_TENORS = new Period[] {
      Period.ofYears(3), Period.ofYears(5), Period.ofYears(7), Period.ofYears(10), Period.ofYears(15), Period.ofYears(20),
      Period.ofYears(30)};
  static {
    for (int i = 0; i < FWD6_NB_NODES; i++) {
      ALL_NODES[i] = FixedIborSwapCurveNode.of(
          FixedIborSwapTemplate.of(Period.ZERO, Tenor.of(FWD6_IRS_TENORS[i]), GBP_FIXED_6M_LIBOR_6M),
          QuoteId.of(StandardId.of(SCHEME, FWD6_ID_VALUE[i])));
      NODE_TIMES[i] = CURVE_DC.relativeYearFraction(VAL_DATE, ALL_NODES[i].date(VAL_DATE, REF_DATA));
    }
  }

  /** All quotes for the curve calibration */
  private static final MarketData ALL_QUOTES;
  static {
    ImmutableMarketDataBuilder builder = ImmutableMarketData.builder(VAL_DATE);
    for (int i = 0; i < FWD6_NB_NODES; i++) {
      builder.addValue(QuoteId.of(StandardId.of(SCHEME, FWD6_ID_VALUE[i])), FWD6_MARKET_QUOTES[i]);
    }
    ALL_QUOTES = builder.build();
  }

  /** All nodes by groups. */
  private static final List<List<CurveNode[]>> CURVES_NODES = new ArrayList<>();
  static {
    List<CurveNode[]> groupNodes = new ArrayList<>();
    groupNodes.add(ALL_NODES);
    CURVES_NODES.add(groupNodes);
  }

  /** All metadata by groups */
  private static final List<List<CurveMetadata>> CURVES_METADATA = new ArrayList<>();
  static {
    List<CurveMetadata> groupMetadata = new ArrayList<>();
    groupMetadata.add(
        DefaultCurveMetadata.builder()
            .curveName(CURVE_NAME)
            .xValueType(ValueType.YEAR_FRACTION)
            .yValueType(ValueType.DISCOUNT_FACTOR)
            .dayCount(CURVE_DC)
            .build());
    CURVES_METADATA.add(groupMetadata);
  }
  private static final DiscountingSwapProductPricer SWAP_PRICER = DiscountingSwapProductPricer.DEFAULT;
  private static final CurveCalibrator CALIBRATOR = CurveCalibrator.of(1e-9, 1e-9, 100);

  /** Test with CurveGroupDefinition */
  private static final String CURVE_GROUP_NAME_STR = "GBP-SINGLE-CURVE";
  private static final CurveGroupName CURVE_GROUP_NAME = CurveGroupName.of(CURVE_GROUP_NAME_STR);
  private static final SmithWilsonCurveFunction SW_CURVE = SmithWilsonCurveFunction.DEFAULT;
  private static final double ALPHA = 0.186649;
  private static final BiFunction<DoubleArray, Double, Double> VALUE_FUNCTION = new BiFunction<DoubleArray, Double, Double>() {
    @Override
    public Double apply(DoubleArray t, Double u) {
      return SW_CURVE.value(u, ALPHA, DoubleArray.copyOf(NODE_TIMES), t);
    }
  };
  private static final BiFunction<DoubleArray, Double, Double> DERIVATIVE_FUNCTION =
      new BiFunction<DoubleArray, Double, Double>() {
        @Override
        public Double apply(DoubleArray t, Double u) {
          return SW_CURVE.firstDerivative(u, ALPHA, DoubleArray.copyOf(NODE_TIMES), t);
        }
      };
  private static final BiFunction<DoubleArray, Double, DoubleArray> SENSI_FUNCTION =
      new BiFunction<DoubleArray, Double, DoubleArray>() {
        @Override
        public DoubleArray apply(DoubleArray t, Double u) {
          return SW_CURVE.parameterSensitivity(u, ALPHA, DoubleArray.copyOf(NODE_TIMES));
        }
      };
  private static final ParameterizedFunctionalCurveDefinition CURVE_DEFN = ParameterizedFunctionalCurveDefinition.builder()
      .name(CURVE_NAME)
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.DISCOUNT_FACTOR)
      .dayCount(CURVE_DC)
      .initialGuess(DoubleArray.filled(FWD6_NB_NODES, 0d).toList())
      .valueFunction(VALUE_FUNCTION)
      .derivativeFunction(DERIVATIVE_FUNCTION)
      .sensitivityFunction(SENSI_FUNCTION)
      .nodes(ALL_NODES)
      .build();
  private static final CurveGroupDefinition CURVE_GROUP_DEFN = CurveGroupDefinition.builder()
      .name(CURVE_GROUP_NAME)
      .addCurve(CURVE_DEFN, GBP, GBP_LIBOR_6M)
      .build();
  /** expected discount factor values (Source: EIOPA - European Insurance and Occupational Pensions Authority) */
  private static final DoubleArray DSC_EXP = DoubleArray.ofUnsafe(new double[] {
      1d, 0.9784596573108600, 0.9540501162514210, 0.9242332228570250, 0.8885036235533640, 0.8529525938462010,
      0.8225785258044620, 0.7955993406787280, 0.7693449840658110, 0.7422694544490090, 0.7132193803280200, 0.6816197350030370,
      0.6485921254081380, 0.6153797190254940, 0.5829431201155780, 0.5520276608703360, 0.5230330826657420, 0.4955678268458750,
      0.4691264946001880, 0.4432705503035050, 0.4176082714808270, 0.3919343348393330, 0.3666298787856500, 0.3421032317280810,
      0.3186497485892540, 0.2964788703398110, 0.2757355981614060, 0.2565175652984340, 0.2388886512264410, 0.2228898936657480,
      0.2085483042428570, 0.1958299034344640, 0.1845026251379590, 0.1743314425719990, 0.1651272569309040, 0.1567376199945500,
      0.1490393434620470, 0.1419326107937100, 0.1353362856089140, 0.1291841730078400, 0.1234220398105030, 0.1180052392228150,
      0.1128968169062150, 0.1080660004841850, 0.1034869944717180, 0.0991380185020832, 0.0950005393771045, 0.0910586575419039,
      0.0872986166075841, 0.0837084109337509, 0.0802774713699451, 0.0769964133060395, 0.0738568344075488, 0.0708511519806606,
      0.0679724719574663, 0.0652144831209293, 0.0625713714864361, 0.0600377507899026, 0.0576086058551821, 0.0552792462687688,
      0.0530452683116555, 0.0509025235138388, 0.0488470925280131, 0.0468752632826767, 0.0449835125849284, 0.0431684905105571,
      0.0414270070523355, 0.0397560206036511, 0.0381526279392532, 0.0366140554223544, 0.0351376512211047, 0.0337208783603299,
      0.0323613084686289, 0.0310566161082008, 0.0298045735965500, 0.0286030462465983, 0.0274499879656266, 0.0263434371645669,
      0.0252815129380532, 0.0242624114827481, 0.0232844027271681, 0.0223458271508077, 0.0214450927740423, 0.0205806723032541,
      0.0197511004180213, 0.0189549711891452, 0.0181909356178633, 0.0174576992878771, 0.0167540201228721, 0.0160787062430671,
      0.0154306139150397, 0.0148086455896693, 0.0142117480235254, 0.0136389104794540, 0.0130891630024586, 0.0125615747672780,
      0.0120552524943231, 0.0115693389308603, 0.0111030113945241, 0.0106554803764249, 0.0102259882012646, 0.0098138077420219,
      0.0094182411868912, 0.0090386188562798, 0.0086742980677716, 0.0083246620470656, 0.0079891188829930, 0.0076671005247963,
      0.0073580618199396, 0.0070614795907931, 0.0067768517486054, 0.0065036964432469, 0.0062415512472709, 0.0059899723728984,
      0.0057485339205936, 0.0055168271579503, 0.0052944598276626, 0.0050810554834040, 0.0048762528524868, 0.0046797052242205,
      0.0044910798629311});
  /** Constants */
  private static final double TOLERANCE_PV = 1.0E-6;
  private static final double ONE_BP = 1.0e-4;
  private static final double ONE_PC = 1.0e-2;

  //-------------------------------------------------------------------------
  public void calibration_test() {
    RatesProvider result2 = CALIBRATOR.calibrate(CURVE_GROUP_DEFN, ALL_QUOTES, REF_DATA);
    // pv test
    CurveNode[] fwd3Nodes = CURVES_NODES.get(0).get(0);
    List<ResolvedTrade> fwd3Trades = new ArrayList<>();
    for (int i = 0; i < fwd3Nodes.length; i++) {
      fwd3Trades.add(fwd3Nodes[i].resolvedTrade(1d, ALL_QUOTES, REF_DATA));
    }
    for (int i = 0; i < FWD6_NB_NODES; i++) {
      MultiCurrencyAmount pvIrs2 = SWAP_PRICER.presentValue(((ResolvedSwapTrade) fwd3Trades.get(i)).getProduct(), result2);
      assertEquals(pvIrs2.getAmount(GBP).getAmount(), 0.0, TOLERANCE_PV);
    }
    // regression test for curve
    DiscountFactors dsc = result2.discountFactors(GBP);
    double prevDsc = 0d;
    for (int i = 0; i < 121; ++i) {
      double time = ((double) i);
      double curDsc = dsc.discountFactor(time);
      if (i > 59) {
        double fwd = prevDsc / curDsc - 1d;
        assertEquals(fwd, 0.042, 2d * ONE_BP);
      }
      assertEquals(curDsc, DSC_EXP.get(i), ONE_PC);
      prevDsc = curDsc;
    }
  }

}
