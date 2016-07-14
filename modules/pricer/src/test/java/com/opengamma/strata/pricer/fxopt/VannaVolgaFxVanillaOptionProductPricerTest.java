package com.opengamma.strata.pricer.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.product.common.LongShort.LONG;
import static com.opengamma.strata.product.common.LongShort.SHORT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.math.impl.linearalgebra.DecompositionResult;
import com.opengamma.strata.math.impl.linearalgebra.SVDecompositionCommons;
import com.opengamma.strata.pricer.fx.DiscountingFxSingleProductPricer;
import com.opengamma.strata.pricer.fx.RatesProviderFxDataSets;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOption;

/**
 * Test {@link VannaVolgaFxVanillaOptionProductPricer}.
 */
@Test
public class VannaVolgaFxVanillaOptionProductPricerTest {

  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final ZonedDateTime VAL_DATETIME = ZonedDateTime.of(2011, 6, 13, 13, 10, 0, 0, ZONE);
  private static final LocalDate VAL_DATE = VAL_DATETIME.toLocalDate();
  private static final ZonedDateTime EXPIRY = ZonedDateTime.of(2012, 12, 13, 10, 0, 0, 0, ZONE);
  private static final LocalDate PAY = LocalDate.of(2012, 12, 17);
  private static final ZonedDateTime AFTER = ZonedDateTime.of(2012, 12, 24, 10, 0, 0, 0, ZONE);
  private static final ImmutableRatesProvider RATES_PROVIDER =
      RatesProviderFxDataSets.createProviderEurUsdActActIsda(VAL_DATE);
  private static final ImmutableRatesProvider RATES_PROVIDER_AFTER =
      RatesProviderFxDataSets.createProviderEurUsdActActIsda(AFTER.toLocalDate());

  private static final DoubleArray TIME_TO_EXPIRY = DoubleArray.of(0.0001, 0.25205479452054796, 0.5013698630136987,
      1.0015120892282356, 2.0, 5.001512089228235);
  private static final DoubleArray ATM = DoubleArray.of(0.11, 0.115, 0.12, 0.12, 0.125, 0.13);
  private static final DoubleArray DELTA = DoubleArray.of(0.25);
  private static final DoubleMatrix RISK_REVERSAL = DoubleMatrix.ofUnsafe(new double[][] {
    {0.015 }, {0.020 }, {0.025 }, {0.03 }, {0.025 }, {0.030 } });
  private static final DoubleMatrix STRANGLE = DoubleMatrix.ofUnsafe(new double[][] {
    {0.002 }, {0.003 }, {0.004 }, {0.0045 }, {0.0045 }, {0.0045 } });
  private static final CurveInterpolator INTERP_STRIKE = CurveInterpolators.DOUBLE_QUADRATIC;
  private static final CurveExtrapolator EXTRAP_STRIKE = CurveExtrapolators.LINEAR;
  private static final InterpolatedStrikeSmileDeltaTermStructure SMILE_TERM =
      InterpolatedStrikeSmileDeltaTermStructure.of(
          TIME_TO_EXPIRY, DELTA, ATM, RISK_REVERSAL, STRANGLE, ACT_ACT_ISDA, INTERP_STRIKE, EXTRAP_STRIKE, EXTRAP_STRIKE);
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, USD);
  private static final BlackFxOptionSmileVolatilities VOLS =
      BlackFxOptionSmileVolatilities.of(FxOptionVolatilitiesName.of("Test"), CURRENCY_PAIR, VAL_DATETIME, SMILE_TERM);
  private static final BlackFxOptionSmileVolatilities VOLS_AFTER =
      BlackFxOptionSmileVolatilities.of(FxOptionVolatilitiesName.of("Test"), CURRENCY_PAIR, AFTER, SMILE_TERM);

  private static final int NB_STRIKES = 11;
  private static final double STRIKE_MIN = 1.00;
  private static final double STRIKE_RANGE = 0.80;
  private static final double NOTIONAL = 100_000_000;
  private static final ResolvedFxVanillaOption[] CALLS = new ResolvedFxVanillaOption[NB_STRIKES];
  private static final ResolvedFxVanillaOption[] PUTS = new ResolvedFxVanillaOption[NB_STRIKES];
  private static final ResolvedFxSingle[] UNDERLYING = new ResolvedFxSingle[NB_STRIKES];
  static {
    for (int i = 0; i < NB_STRIKES; ++i) {
      double strike = STRIKE_MIN + i * STRIKE_RANGE / (NB_STRIKES - 1d);
      CurrencyAmount eurAmount = CurrencyAmount.of(EUR, NOTIONAL);
      CurrencyAmount usdAmount = CurrencyAmount.of(USD, -NOTIONAL * strike);
      UNDERLYING[i] = ResolvedFxSingle.of(eurAmount, usdAmount, PAY);
      CALLS[i] = ResolvedFxVanillaOption.builder()
          .longShort(LONG)
          .expiry(EXPIRY)
          .underlying(UNDERLYING[i])
          .build();
      PUTS[i] = ResolvedFxVanillaOption.builder()
          .longShort(SHORT)
          .expiry(EXPIRY)
          .underlying(UNDERLYING[i].inverse())
          .build();
    }
  }

  private static final double TOL = 1.0e-13;
  private static final double FD_EPS = 1.0e-7;
  private static final VannaVolgaFxVanillaOptionProductPricer PRICER = VannaVolgaFxVanillaOptionProductPricer.DEFAULT;
  private static final DiscountingFxSingleProductPricer FX_PRICER = DiscountingFxSingleProductPricer.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(FD_EPS);
  private static final SVDecompositionCommons SVD = new SVDecompositionCommons();

  //-------------------------------------------------------------------------
  public void test_price_presentValue() {
    for (int i = 0; i < NB_STRIKES; ++i) {
      double computedPriceCall = PRICER.price(CALLS[i], RATES_PROVIDER, VOLS);
      CurrencyAmount computedCall = PRICER.presentValue(CALLS[i], RATES_PROVIDER, VOLS);
      double timeToExpiry = VOLS.relativeTime(EXPIRY);
      FxRate forward = FX_PRICER.forwardFxRate(UNDERLYING[i], RATES_PROVIDER);
      double forwardRate = forward.fxRate(CURRENCY_PAIR);
      double strikeRate = CALLS[i].getStrike();
      SmileDeltaParameters smileAtTime = VOLS.getSmile().smileForExpiry(timeToExpiry);
      double[] strikes = smileAtTime.strike(forwardRate).toArray();
      double[] vols = smileAtTime.getVolatility().toArray();
      double df = RATES_PROVIDER.discountFactor(USD, PAY);
      double[] weights = weights(forwardRate, strikeRate, strikes, timeToExpiry, vols[1]);
      double expectedPriceCall = BlackFormulaRepository.price(forwardRate, strikeRate, timeToExpiry, vols[1], true);
      for (int j = 0; j < 3; ++j) {
        expectedPriceCall += weights[j] * (
            BlackFormulaRepository.price(forwardRate, strikes[j], timeToExpiry, vols[j], true)
            - BlackFormulaRepository.price(forwardRate, strikes[j], timeToExpiry, vols[1], true));
      }
      expectedPriceCall *= df;
      assertEquals(computedPriceCall, expectedPriceCall, TOL);
      assertEquals(computedCall.getAmount(), expectedPriceCall * NOTIONAL, TOL * NOTIONAL);
    }
  }

  public void test_price_presentValue_afterExpiry() {
    for (int i = 0; i < NB_STRIKES; ++i) {
      double computedPriceCall = PRICER.price(CALLS[i], RATES_PROVIDER_AFTER, VOLS_AFTER);
      CurrencyAmount computedCall = PRICER.presentValue(CALLS[i], RATES_PROVIDER_AFTER, VOLS_AFTER);
      assertEquals(computedPriceCall, 0d, TOL);
      assertEquals(computedCall.getAmount(), 0d, TOL);
      double computedPricePut = PRICER.price(PUTS[i], RATES_PROVIDER_AFTER, VOLS_AFTER);
      CurrencyAmount computedPut = PRICER.presentValue(PUTS[i], RATES_PROVIDER_AFTER, VOLS_AFTER);
      assertEquals(computedPricePut, 0d, TOL);
      assertEquals(computedPut.getAmount(), 0d, TOL);
    }
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    for (int i = 0; i < NB_STRIKES; ++i) {
      ResolvedFxVanillaOption option = CALLS[i];
      PointSensitivityBuilder point = PRICER.presentValueSensitivityRatesStickyStrike(option, RATES_PROVIDER, VOLS);
      CurrencyParameterSensitivities sensiComputed = RATES_PROVIDER.parameterSensitivity(point.build());
      double timeToExpiry = VOLS.relativeTime(EXPIRY);
      double forwardRate = FX_PRICER.forwardFxRate(UNDERLYING[i], RATES_PROVIDER).fxRate(CURRENCY_PAIR);
      double strikeRate = option.getStrike();
      SmileDeltaParameters smileAtTime = VOLS.getSmile().smileForExpiry(timeToExpiry);
      double[] vols = smileAtTime.getVolatility().toArray();
      double df = RATES_PROVIDER.discountFactor(USD, PAY);
      CurrencyParameterSensitivities sensiExpected =
          FD_CAL.sensitivity(RATES_PROVIDER, p -> PRICER.presentValue(option, p, VOLS));
      CurrencyParameterSensitivities sensiRes = FD_CAL.sensitivity(RATES_PROVIDER,
          new Function<ImmutableRatesProvider, CurrencyAmount>() {
            @Override
            public CurrencyAmount apply(ImmutableRatesProvider p) {
              double fwd = FX_PRICER.forwardFxRate(option.getUnderlying(), p).fxRate(CURRENCY_PAIR);
              double[] strs = smileAtTime.strike(fwd).toArray();
              double[] wghts = weights(fwd, strikeRate, strs, timeToExpiry, vols[1]);
              double res = 0d;
              for (int j = 0; j < 3; ++j) {
                res += wghts[j] * (BlackFormulaRepository.price(forwardRate, strs[j], timeToExpiry, vols[j], true)
                    - BlackFormulaRepository.price(forwardRate, strs[j], timeToExpiry, vols[1], true));
              }
              return CurrencyAmount.of(USD, -res * df * NOTIONAL);
            }
           }
          );
      assertTrue(sensiComputed.equalWithTolerance(sensiExpected.combinedWith(sensiRes), FD_EPS * NOTIONAL * 10d));
    }
  }

  public void test_presentValueSensitivity_afterExpiry() {
    for (int i = 0; i < NB_STRIKES; ++i) {
      PointSensitivityBuilder computedCall =
          PRICER.presentValueSensitivityRatesStickyStrike(CALLS[i], RATES_PROVIDER_AFTER, VOLS_AFTER);
      assertEquals(computedCall, PointSensitivityBuilder.none());
      PointSensitivityBuilder computedPut =
          PRICER.presentValueSensitivityRatesStickyStrike(PUTS[i], RATES_PROVIDER_AFTER, VOLS_AFTER);
      assertEquals(computedPut, PointSensitivityBuilder.none());
    }
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivityVolatility() {
    for (int i = 0; i < NB_STRIKES; ++i) {
      PointSensitivities computedCall =
          PRICER.presentValueSensitivityModelParamsVolatility(CALLS[i], RATES_PROVIDER, VOLS).build();
      double timeToExpiry = VOLS.relativeTime(EXPIRY);
      FxRate forward = FX_PRICER.forwardFxRate(UNDERLYING[i], RATES_PROVIDER);
      double forwardRate = forward.fxRate(CURRENCY_PAIR);
      double strikeRate = CALLS[i].getStrike();
      SmileDeltaParameters smileAtTime = VOLS.getSmile().smileForExpiry(timeToExpiry);
      double[] strikes = smileAtTime.strike(forwardRate).toArray();
      double[] vols = smileAtTime.getVolatility().toArray();
      double df = RATES_PROVIDER.discountFactor(USD, PAY);
      double[] weights = weights(forwardRate, strikeRate, strikes, timeToExpiry, vols[1]);
      double[] vegas = new double[3];
      vegas[2] = BlackFormulaRepository.vega(forwardRate, strikeRate, timeToExpiry, vols[1]) * df * NOTIONAL;
      for (int j = 0; j < 3; j += 2) {
        vegas[2] -= weights[j] * NOTIONAL * df *
            BlackFormulaRepository.vega(forwardRate, strikes[j], timeToExpiry, vols[1]);
      }
      vegas[0] = weights[0] * NOTIONAL * df *
          BlackFormulaRepository.vega(forwardRate, strikes[0], timeToExpiry, vols[0]);
      vegas[1] = weights[2] * NOTIONAL * df *
          BlackFormulaRepository.vega(forwardRate, strikes[2], timeToExpiry, vols[2]);
      double[] expStrikes = new double[] {strikes[0], strikes[2], strikes[1] };
      for (int j = 0; j < 3; ++j) {
        FxOptionSensitivity sensi = (FxOptionSensitivity) computedCall.getSensitivities().get(j);
        assertEquals(sensi.getSensitivity(), vegas[j], TOL * NOTIONAL);
        assertEquals(sensi.getStrike(), expStrikes[j], TOL);
        assertEquals(sensi.getForward(), forwardRate, TOL);
        assertEquals(sensi.getCurrency(), USD);
        assertEquals(sensi.getCurrencyPair(), CURRENCY_PAIR);
        assertEquals(sensi.getExpiry(), timeToExpiry);
      }
    }
  }

  public void test_presentValueSensitivityVolatility_afterExpiry() {
    for (int i = 0; i < NB_STRIKES; ++i) {
      PointSensitivityBuilder computedCall =
          PRICER.presentValueSensitivityModelParamsVolatility(CALLS[i], RATES_PROVIDER_AFTER, VOLS_AFTER);
      assertEquals(computedCall, PointSensitivityBuilder.none());
      PointSensitivityBuilder computedPut =
          PRICER.presentValueSensitivityModelParamsVolatility(PUTS[i], RATES_PROVIDER_AFTER, VOLS_AFTER);
      assertEquals(computedPut, PointSensitivityBuilder.none());
    }
  }

  //-------------------------------------------------------------------------
  public void test_currencyExposure() {
    for (int i = 0; i < NB_STRIKES; ++i) {
      CurrencyAmount pvCall = PRICER.presentValue(CALLS[i], RATES_PROVIDER, VOLS);
      PointSensitivityBuilder pvSensiCall = PRICER.presentValueSensitivityRatesStickyStrike(CALLS[i], RATES_PROVIDER, VOLS);
      MultiCurrencyAmount computedCall = PRICER.currencyExposure(CALLS[i], RATES_PROVIDER, VOLS);
      MultiCurrencyAmount expectedCall = RATES_PROVIDER.currencyExposure(pvSensiCall.build()).plus(pvCall);
      assertEquals(computedCall.getAmount(EUR).getAmount(), expectedCall.getAmount(EUR).getAmount(), NOTIONAL * TOL);
      assertEquals(computedCall.getAmount(USD).getAmount(), expectedCall.getAmount(USD).getAmount(), NOTIONAL * TOL);
      CurrencyAmount pvPut = PRICER.presentValue(PUTS[i], RATES_PROVIDER, VOLS);
      PointSensitivityBuilder pvSensiPut = PRICER.presentValueSensitivityRatesStickyStrike(PUTS[i], RATES_PROVIDER, VOLS);
      MultiCurrencyAmount computedPut = PRICER.currencyExposure(PUTS[i], RATES_PROVIDER, VOLS);
      MultiCurrencyAmount expectedPut = RATES_PROVIDER.currencyExposure(pvSensiPut.build()).plus(pvPut);
      assertEquals(computedPut.getAmount(EUR).getAmount(), expectedPut.getAmount(EUR).getAmount(), NOTIONAL * TOL);
      assertEquals(computedPut.getAmount(USD).getAmount(), expectedPut.getAmount(USD).getAmount(), NOTIONAL * TOL);
    }
  }

  public void test_currencyExposure_atExpiry() {
    for (int i = 0; i < NB_STRIKES; ++i) {
      MultiCurrencyAmount computedCall = PRICER.currencyExposure(CALLS[i], RATES_PROVIDER_AFTER, VOLS_AFTER);
      assertEquals(computedCall, MultiCurrencyAmount.empty());
      MultiCurrencyAmount computedPut = PRICER.currencyExposure(PUTS[i], RATES_PROVIDER_AFTER, VOLS_AFTER);
      assertEquals(computedPut, MultiCurrencyAmount.empty());
    }
  }

  //-------------------------------------------------------------------------
  public void test_putCallParity() {
    double df = RATES_PROVIDER.discountFactor(USD, PAY);
    PointSensitivityBuilder dfSensi = RATES_PROVIDER.discountFactors(USD).zeroRatePointSensitivity(PAY);
    for (int i = 0; i < NB_STRIKES; ++i) {
      CurrencyAmount pvCall = PRICER.presentValue(CALLS[i], RATES_PROVIDER, VOLS);
      PointSensitivityBuilder pvSensiCall =
          PRICER.presentValueSensitivityRatesStickyStrike(CALLS[i], RATES_PROVIDER, VOLS);
      PointSensitivityBuilder pvSensiVolCall =
          PRICER.presentValueSensitivityModelParamsVolatility(CALLS[i], RATES_PROVIDER, VOLS);
      CurrencyAmount pvPut = PRICER.presentValue(PUTS[i], RATES_PROVIDER, VOLS);
      PointSensitivityBuilder pvSensiPut =
          PRICER.presentValueSensitivityRatesStickyStrike(PUTS[i], RATES_PROVIDER, VOLS);
      PointSensitivityBuilder pvSensiVolPut =
          PRICER.presentValueSensitivityModelParamsVolatility(PUTS[i], RATES_PROVIDER, VOLS);
      double forward = FX_PRICER.forwardFxRate(UNDERLYING[i], RATES_PROVIDER).fxRate(CURRENCY_PAIR);
      PointSensitivityBuilder forwardSensi = FX_PRICER.forwardFxRatePointSensitivity(UNDERLYING[i], RATES_PROVIDER);
      double strike = CALLS[i].getStrike();
      assertEquals(pvCall.getAmount() + pvPut.getAmount(), df * (forward - strike) * NOTIONAL, TOL * NOTIONAL);
      assertTrue(pvSensiCall.combinedWith(pvSensiPut).build().normalized().equalWithTolerance(
          dfSensi.multipliedBy((forward - strike) * NOTIONAL)
              .combinedWith(forwardSensi.multipliedBy(df * NOTIONAL)).build().normalized(),
          NOTIONAL * TOL));
      DoubleArray sensiVol = VOLS.parameterSensitivity(
          pvSensiVolCall.combinedWith(pvSensiVolPut).build()).getSensitivities().get(0).getSensitivity();
      assertTrue(DoubleArrayMath.fuzzyEquals(sensiVol.toArray(), new double[sensiVol.size()], NOTIONAL * TOL));
    }
  }

  //-------------------------------------------------------------------------
  public void regression_test() {
    double[] expected = new double[] {
      3.860405407112769E7, 3.0897699603079587E7, 2.3542824458812844E7, 1.6993448607300103E7, 1.1705393621236656E7,
      7865881.8260216825, 5312495.846331886, 3680367.6766224853, 2607701.430445888, 1849818.297903138, 1282881.9812227674 };
    double[][][] sensiExpected = new double[][][] {
      { {0d, 0d, -1.016834993607875E8, -1.0687281893573801E8, 0d },
      {0d, 0d, 7.321670893786977E7, 7.695325324735151E7, 0d } },
      { {0d, 0d, -1.0021953885887374E8, -1.0533414661787288E8, 0d },
      {0d, 0d, 7.743544635059586E7, 8.13872898657015E7, 0d } },
      { {0d, 0d, -9.430418338541561E7, -9.911690666813123E7, 0d },
      {0d, 0d, 7.69436064730077E7, 8.087034941308834E7, 0d } },
      { {0d, 0d, -8.284596766339977E7, -8.707393192902757E7, 0d },
      {0d, 0d, 7.031492781003796E7, 7.3903382511043E7, 0d } },
      { {0d, 0d, -6.764082328040574E7, -7.109280762910862E7, 0d },
      {0d, 0d, 5.900921722111582E7, 6.2020695857797466E7, 0d } },
      { {0d, 0d, -5.2035331262043096E7, -5.4690904337366335E7, 0d },
      {0d, 0d, 4.623499720852033E7, 4.859455581508104E7, 0d } },
      { {0d, 0d, -3.862682913929568E7, -4.059811220715709E7, 0d },
      {0d, 0d, 3.470937255296122E7, 3.64807319923551E7, 0d } },
      { {0d, 0d, -2.8260648102423556E7, -2.970290309286549E7, 0d },
      {0d, 0d, 2.554672963322189E7, 2.6850482405254934E7, 0d } },
      { {0d, 0d, -2.0537629799871795E7, -2.1585747980437294E7, 0d },
      {0d, 0d, 1.8614699892839946E7, 1.9564683195371673E7, 0d } },
      { {0d, 0d, -1.4728101851302534E7, -1.5479736361515924E7, 0d },
      {0d, 0d, 1.3364038126029937E7, 1.404605895619165E7, 0d } },
      { {0d, 0d, -1.0288414551608022E7, -1.0813473891259879E7, 0d },
      {0d, 0d, 9342412.029968219, 9819193.040939828, 0d } } };
    double[][] sensiVolExpected = new double[][] {
      {-5.026579681006058E7, 1.8086314260527827E7, 3.7857814067085885E7 },
      {-8.042768121510313E7, 2.6917736783424407E7, 6.828128237717555E7 },
      {-7.799249393870309E7, 2.3424689020542752E7, 8.3630726080757E7 },
      {-3.164036884467365E7, 7924431.550466087, 6.934450937795731E7 },
      {3.014881927958022E7, -5158819.178957329, 3.45255051012762E7 },
      {6.557093411024924E7, -1634314.6028730718, 2572569.1856012754 },
      {5.660208041880186E7, 1.8970507395428047E7, -1.03548720023163E7 },
      {1.685473267352155E7, 4.502558010977008E7, -4691785.767471665 },
      {-2.724527388461766E7, 6.375255786622942E7, 9031557.200953793 },
      {-5.657559310469577E7, 6.925498619398344E7, 2.0671226006236725E7 },
      {-6.6067357100129634E7, 6.307938934530911E7, 2.5692820222277485E7 } };
    CurveName eurName = RatesProviderFxDataSets.getCurveName(EUR);
    CurveName usdName = RatesProviderFxDataSets.getCurveName(USD);
    for (int i = 0; i < NB_STRIKES; ++i) {
      // pv
      CurrencyAmount computed = PRICER.presentValue(CALLS[i], RATES_PROVIDER, VOLS);
      assertEquals(computed.getAmount(), expected[i], NOTIONAL * TOL);
      // curve sensitivity
      PointSensitivityBuilder point = PRICER.presentValueSensitivityRatesStickyStrike(CALLS[i], RATES_PROVIDER, VOLS);
      CurrencyParameterSensitivities sensiComputed = RATES_PROVIDER.parameterSensitivity(point.build());
      assertTrue(DoubleArrayMath.fuzzyEquals(
          sensiComputed.getSensitivity(eurName, USD).getSensitivity().toArray(),
          sensiExpected[i][0],
          NOTIONAL * TOL));
      assertTrue(DoubleArrayMath.fuzzyEquals(
          sensiComputed.getSensitivity(usdName, USD).getSensitivity().toArray(),
          sensiExpected[i][1],
          NOTIONAL * TOL));
      // vol sensitivity
      PointSensitivities pointVol =
          PRICER.presentValueSensitivityModelParamsVolatility(CALLS[i], RATES_PROVIDER, VOLS).build();
      assertEquals(pointVol.getSensitivities().get(0).getSensitivity(), sensiVolExpected[i][2], NOTIONAL * TOL);
      assertEquals(pointVol.getSensitivities().get(1).getSensitivity(), sensiVolExpected[i][1], NOTIONAL * TOL);
      assertEquals(pointVol.getSensitivities().get(2).getSensitivity(), sensiVolExpected[i][0], NOTIONAL * TOL);
    }
  }

  //-------------------------------------------------------------------------
  private double[] weights(double forward, double strike, double[] strikes, double timeToExpiry, double atmVol) {
    double[][] mat = new double[3][3];
    double[] vec = new double[3];
    for (int i = 0; i < 3; ++i) {
      mat[0][i] = BlackFormulaRepository.vega(forward, strikes[i], timeToExpiry, atmVol);
      mat[1][i] = BlackFormulaRepository.vanna(forward, strikes[i], timeToExpiry, atmVol);
      mat[2][i] = BlackFormulaRepository.volga(forward, strikes[i], timeToExpiry, atmVol);
    }
    vec[0] = BlackFormulaRepository.vega(forward, strike, timeToExpiry, atmVol);
    vec[1] = BlackFormulaRepository.vanna(forward, strike, timeToExpiry, atmVol);
    vec[2] = BlackFormulaRepository.volga(forward, strike, timeToExpiry, atmVol);
    DecompositionResult res = SVD.apply(DoubleMatrix.ofUnsafe(mat));
    return res.solve(vec);
  }

}
