/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.product.common.LongShort.LONG;
import static com.opengamma.strata.product.common.LongShort.SHORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import com.opengamma.strata.pricer.fx.DiscountFxForwardRates;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
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
import com.opengamma.strata.math.impl.linearalgebra.SVDecompositionCommons;
import com.opengamma.strata.math.linearalgebra.DecompositionResult;
import com.opengamma.strata.pricer.fx.DiscountingFxSingleProductPricer;
import com.opengamma.strata.pricer.fx.RatesProviderFxDataSets;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOption;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOptionTrade;

/**
 * Test {@link VannaVolgaFxVanillaOptionProductPricer}.
 */
public class VannaVolgaFxVanillaOptionProductPricerTest {

  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final ZonedDateTime VAL_DATETIME = ZonedDateTime.of(2011, 6, 13, 13, 10, 0, 0, ZONE);

  private static final LocalDate SPOT_DATE = LocalDate.of(2011, 6, 15);
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
      {0.015}, {0.020}, {0.025}, {0.03}, {0.025}, {0.030}});
  private static final DoubleMatrix STRANGLE = DoubleMatrix.ofUnsafe(new double[][] {
      {0.002}, {0.003}, {0.004}, {0.0045}, {0.0045}, {0.0045}});
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
  private static final VannaVolgaFxVanillaOptionTradePricer TRADE_PRICER = VannaVolgaFxVanillaOptionTradePricer.DEFAULT;
  private static final DiscountingFxSingleProductPricer FX_PRICER = DiscountingFxSingleProductPricer.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(FD_EPS);
  private static final SVDecompositionCommons SVD = new SVDecompositionCommons();

  //-------------------------------------------------------------------------
  @Test
  public void test_price_presentValue() {
    for (int i = 0; i < NB_STRIKES; ++i) {
      ResolvedFxVanillaOption call = CALLS[i];
      ResolvedFxVanillaOptionTrade callTrade = ResolvedFxVanillaOptionTrade.builder()
          .product(call)
          .premium(Payment.of(EUR, 0, VAL_DATE))
          .build();
      double computedPriceCall = PRICER.price(call, RATES_PROVIDER, VOLS);
      CurrencyAmount computedCall = PRICER.presentValue(call, RATES_PROVIDER, VOLS);
      double timeToExpiry = VOLS.relativeTime(EXPIRY);
      FxRate forward = FX_PRICER.forwardFxRate(UNDERLYING[i], RATES_PROVIDER);
      double forwardRate = forward.fxRate(CURRENCY_PAIR);
      double strikeRate = call.getStrike();
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
      assertThat(computedPriceCall).isCloseTo(expectedPriceCall, offset(TOL));
      assertThat(computedCall.getAmount()).isCloseTo(expectedPriceCall * NOTIONAL, offset(TOL * NOTIONAL));
      // test against trade pricer
      assertThat(computedCall).isEqualTo(TRADE_PRICER.presentValue(callTrade, RATES_PROVIDER, VOLS).getAmount(USD));
    }
  }

  @Test
  public void test_price_presentValue_afterExpiry() {
    for (int i = 0; i < NB_STRIKES; ++i) {
      ResolvedFxVanillaOption call = CALLS[i];
      ResolvedFxVanillaOptionTrade callTrade = ResolvedFxVanillaOptionTrade.builder()
          .product(call)
          .premium(Payment.of(EUR, 0, VOLS_AFTER.getValuationDate()))
          .build();
      double computedPriceCall = PRICER.price(call, RATES_PROVIDER_AFTER, VOLS_AFTER);
      CurrencyAmount computedCall = PRICER.presentValue(call, RATES_PROVIDER_AFTER, VOLS_AFTER);
      assertThat(computedPriceCall).isCloseTo(0d, offset(TOL));
      assertThat(computedCall.getAmount()).isCloseTo(0d, offset(TOL));
      ResolvedFxVanillaOption put = PUTS[i];
      ResolvedFxVanillaOptionTrade putTrade = ResolvedFxVanillaOptionTrade.builder()
          .product(put)
          .premium(Payment.of(EUR, 0, VOLS_AFTER.getValuationDate()))
          .build();
      double computedPricePut = PRICER.price(put, RATES_PROVIDER_AFTER, VOLS_AFTER);
      CurrencyAmount computedPut = PRICER.presentValue(put, RATES_PROVIDER_AFTER, VOLS_AFTER);
      assertThat(computedPricePut).isCloseTo(0d, offset(TOL));
      assertThat(computedPut.getAmount()).isCloseTo(0d, offset(TOL));
      // test against trade pricer
      assertThat(computedCall).isEqualTo(TRADE_PRICER.presentValue(callTrade, RATES_PROVIDER_AFTER, VOLS_AFTER).getAmount(USD));
      assertThat(computedPut).isEqualTo(TRADE_PRICER.presentValue(putTrade, RATES_PROVIDER_AFTER, VOLS_AFTER).getAmount(USD));
    }
  }

  //-------------------------------------------------------------------------
  @Test
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
          });
      assertThat(sensiComputed.equalWithTolerance(sensiExpected.combinedWith(sensiRes), FD_EPS * NOTIONAL * 10d)).isTrue();
    }
  }

  @Test
  public void test_presentValueSensitivity_afterExpiry() {
    for (int i = 0; i < NB_STRIKES; ++i) {
      PointSensitivityBuilder computedCall =
          PRICER.presentValueSensitivityRatesStickyStrike(CALLS[i], RATES_PROVIDER_AFTER, VOLS_AFTER);
      assertThat(computedCall).isEqualTo(PointSensitivityBuilder.none());
      PointSensitivityBuilder computedPut =
          PRICER.presentValueSensitivityRatesStickyStrike(PUTS[i], RATES_PROVIDER_AFTER, VOLS_AFTER);
      assertThat(computedPut).isEqualTo(PointSensitivityBuilder.none());
    }
  }

  //-------------------------------------------------------------------------
  @Test
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
        assertThat(sensi.getSensitivity()).isCloseTo(vegas[j], offset(TOL * NOTIONAL));
        assertThat(sensi.getStrike()).isCloseTo(expStrikes[j], offset(TOL));
        assertThat(sensi.getForward()).isCloseTo(forwardRate, offset(TOL));
        assertThat(sensi.getCurrency()).isEqualTo(USD);
        assertThat(sensi.getCurrencyPair()).isEqualTo(CURRENCY_PAIR);
        assertThat(sensi.getExpiry()).isEqualTo(timeToExpiry);
      }
    }
  }

  @Test
  public void test_presentValueSensitivityVolatility_afterExpiry() {
    for (int i = 0; i < NB_STRIKES; ++i) {
      PointSensitivityBuilder computedCall =
          PRICER.presentValueSensitivityModelParamsVolatility(CALLS[i], RATES_PROVIDER_AFTER, VOLS_AFTER);
      assertThat(computedCall).isEqualTo(PointSensitivityBuilder.none());
      PointSensitivityBuilder computedPut =
          PRICER.presentValueSensitivityModelParamsVolatility(PUTS[i], RATES_PROVIDER_AFTER, VOLS_AFTER);
      assertThat(computedPut).isEqualTo(PointSensitivityBuilder.none());
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_currencyExposure() {
    for (int i = 0; i < NB_STRIKES; ++i) {

      double dfBaseSpot = RATES_PROVIDER.discountFactor(EUR, SPOT_DATE);
      double dfCounterSpot = RATES_PROVIDER.discountFactor(USD, SPOT_DATE);
      double adjustedFxSpotScalingFactor = DiscountFxForwardRates.adjustedFxScalingFactor(dfCounterSpot, dfBaseSpot);
      double adjustedFxSpotScalingFactorInv = DiscountFxForwardRates.adjustedFxScalingFactor(dfBaseSpot, dfCounterSpot);

      CurrencyAmount pvCall = PRICER.presentValue(CALLS[i], RATES_PROVIDER, VOLS);
      PointSensitivityBuilder pvSensiCall = PRICER.presentValueSensitivityRatesStickyStrike(CALLS[i], RATES_PROVIDER, VOLS);
      MultiCurrencyAmount computedCall = PRICER.currencyExposure(CALLS[i], RATES_PROVIDER, VOLS);
      MultiCurrencyAmount expectedCall = RATES_PROVIDER.currencyExposure(pvSensiCall.build()).plus(pvCall);
      assertThat(computedCall.getAmount(EUR).getAmount()).isCloseTo(expectedCall.getAmount(EUR).getAmount() * adjustedFxSpotScalingFactorInv, offset(NOTIONAL * TOL));
      assertThat(computedCall.getAmount(USD).getAmount()).isCloseTo(expectedCall.getAmount(USD).getAmount(), offset(NOTIONAL * TOL));
      CurrencyAmount pvPut = PRICER.presentValue(PUTS[i], RATES_PROVIDER, VOLS);
      PointSensitivityBuilder pvSensiPut = PRICER.presentValueSensitivityRatesStickyStrike(PUTS[i], RATES_PROVIDER, VOLS);
      MultiCurrencyAmount computedPut = PRICER.currencyExposure(PUTS[i], RATES_PROVIDER, VOLS);
      MultiCurrencyAmount expectedPut = RATES_PROVIDER.currencyExposure(pvSensiPut.build()).plus(pvPut);
      assertThat(computedPut.getAmount(EUR).getAmount()).isCloseTo(expectedPut.getAmount(EUR).getAmount() * adjustedFxSpotScalingFactorInv, offset(NOTIONAL * TOL));
      assertThat(computedPut.getAmount(USD).getAmount()).isCloseTo(expectedPut.getAmount(USD).getAmount(), offset(NOTIONAL * TOL));
    }
  }

  @Test
  public void test_currencyExposure_atExpiry() {
    for (int i = 0; i < NB_STRIKES; ++i) {
      MultiCurrencyAmount computedCall = PRICER.currencyExposure(CALLS[i], RATES_PROVIDER_AFTER, VOLS_AFTER);
      assertThat(computedCall).isEqualTo(MultiCurrencyAmount.empty());
      MultiCurrencyAmount computedPut = PRICER.currencyExposure(PUTS[i], RATES_PROVIDER_AFTER, VOLS_AFTER);
      assertThat(computedPut).isEqualTo(MultiCurrencyAmount.empty());
    }
  }

  //-------------------------------------------------------------------------
  @Test
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
      assertThat(pvCall.getAmount() + pvPut.getAmount()).isCloseTo(df * (forward - strike) * NOTIONAL, offset(TOL * NOTIONAL));
      assertThat(pvSensiCall.combinedWith(pvSensiPut).build().normalized().equalWithTolerance(
          dfSensi.multipliedBy((forward - strike) * NOTIONAL)
              .combinedWith(forwardSensi.multipliedBy(df * NOTIONAL)).build().normalized(),
          NOTIONAL * TOL)).isTrue();
      DoubleArray sensiVol = VOLS.parameterSensitivity(
          pvSensiVolCall.combinedWith(pvSensiVolPut).build()).getSensitivities().get(0).getSensitivity();
      assertThat(DoubleArrayMath.fuzzyEquals(sensiVol.toArray(), new double[sensiVol.size()], NOTIONAL * TOL)).isTrue();
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void regression_test() {
    double[] expected = new double[] {
        3.860773915622995E7, 3.090131827286494E7, 2.3546231524774473E7, 1.6996423960637994E7, 1.1707754134803377E7,
        7867588.444112781,  5313656.974715042, 3681155.37525992, 2608264.822354848, 1850247.4208945043, 1283217.0491718557};
    double[][][] sensiExpected = new double[][][] {
        {{747322.9448341425, 8280.586646361655, -1.0168629798378006E8, -1.0687576038376284E8, 0d},
            {-747322.9448341425, -8280.586646361655, 7.321679016374376E7, 7.695333861850986E7, 0d}},
        {{736572.1375601828, 8161.464128090634, -1.0022346347617815E8, -1.0533827152424611E8, 0d},
            {-736572.1375601827, -8161.4641280906335, 7.74367025457133E7, 8.138861016952837E7, 0d}},
        {{693113.6656310421, 7679.929813086307, -9.431018173225E7, -9.912321113482618E7, 0d},
            {-693113.665631042, -7679.929813086305, 7.694709243519697E7, 8.087401327800462E7, 0d}},
        {{608919.024346212, 6747.025200512016, -8.285403490642656E7, -8.708241087604193E7, 0d},
            {-608919.0243462119, -6747.0252005120155, 7.032080101510832E7, 7.390955544948438E7, 0d}},
        {{497178.5978847026, 5508.904131686432, -6.764980441873366E7, -7.110224711119166E7, 0d},
            {-497178.59788470255, -5508.904131686432, 5.901645770689799E7, 6.202830585480651E7, 0d}},
        {{382485.17340124457, 4238.062863171667, -5.204376713669427E7, -5.46997707287654E7, 0d},
            {-382485.17340124445, -4238.062863171666, 4.624217461587575E7, 4.860209951451651E7, 0d}},
        {{283931.65843247465, 3146.0571571465202, -3.863384554959862E7, -4.060548669321036E7, 0d},
            {-283931.6584324746, -3146.05715714652, 3.471553274235335E7, 3.648720656108703E7, 0d}},
        {{207736.26570169188, 2301.786877581064, -2.8266135761251245E7, -2.9708670809082564E7, 0d},
            {-207736.26570169185, -2301.7868775810634, 2.5551636439790208E7, 2.6855639625976335E7, 0d}},
        {{150968.4536836128, 1672.7806502339301, -2.0541886574657995E7, -2.159022199562988E7, 0d},
            {-150968.45368361278, -1672.7806502339297, 1.8618541220083542E7, 1.9568720561055962E7, 0d}},
        {{108265.83237626226, 1199.621411371322, -1.4731451467635466E7, -1.5483256922296228E7, 0d},
            {-108265.83237626226, -1199.621411371322, 1.3367071305279907E7, 1.404924693082686E7, 0d}},
        {{75632.03417681024, 838.0280795214395, -1.0291055048670305E7, -1.0816249143549697E7, 0d},
            {-75632.03417681022, -838.0280795214394, 9344805.446527854, 9821708.603211226, 0d}}};
    double[][] sensiVolExpected = new double[][] {
        {-5.025516305057357E7, 1.8082859268198095E7, 3.7848637419676445E7},
        {-8.04229609556619E7, 2.691701128728243E7, 6.827332775274484E7},
        {-7.800483590880273E7, 2.3429643387621503E7, 8.36325434480649E7},
        {-3.166703791696776E7, 7931926.229232709, 6.935815711371881E7},
        {3.0125245957015857E7, -5156171.807872863, 3.45431452333293E7},
        {6.556642058862141E7, -1640671.5915261465, 2583909.5351274726},
        {5.661792558701583E7, 1.895809930278689E7, -1.0353977516563078E7},
        {1.687962732201374E7, 4.501405948982404E7, -4698106.224131586},
        {-2.72244808288568E7, 6.374739662140611E7, 9024038.131507456},
        {-5.6565929301713035E7, 6.92574749428459E7, 2.066677928197974E7},
        {-6.606871082612002E7, 6.308742719819422E7, 2.569254315927906E7}};
    CurveName eurName = RatesProviderFxDataSets.getCurveName(EUR);
    CurveName usdName = RatesProviderFxDataSets.getCurveName(USD);
    for (int i = 0; i < NB_STRIKES; ++i) {
      // pv
      CurrencyAmount computed = PRICER.presentValue(CALLS[i], RATES_PROVIDER, VOLS);
      assertThat(computed.getAmount()).isCloseTo(expected[i], offset(NOTIONAL * TOL));
      // curve sensitivity
      PointSensitivityBuilder point = PRICER.presentValueSensitivityRatesStickyStrike(CALLS[i], RATES_PROVIDER, VOLS);
      CurrencyParameterSensitivities sensiComputed = RATES_PROVIDER.parameterSensitivity(point.build());
      assertThat(DoubleArrayMath.fuzzyEquals(
          sensiComputed.getSensitivity(eurName, USD).getSensitivity().toArray(),
          sensiExpected[i][0],
          NOTIONAL * TOL)).isTrue();
      assertThat(DoubleArrayMath.fuzzyEquals(
          sensiComputed.getSensitivity(usdName, USD).getSensitivity().toArray(),
          sensiExpected[i][1],
          NOTIONAL * TOL)).isTrue();
      // vol sensitivity
      PointSensitivities pointVol =
          PRICER.presentValueSensitivityModelParamsVolatility(CALLS[i], RATES_PROVIDER, VOLS).build();
      assertThat(pointVol.getSensitivities().get(0).getSensitivity()).isCloseTo(sensiVolExpected[i][2], offset(NOTIONAL * TOL));
      assertThat(pointVol.getSensitivities().get(1).getSensitivity()).isCloseTo(sensiVolExpected[i][1], offset(NOTIONAL * TOL));
      assertThat(pointVol.getSensitivities().get(2).getSensitivity()).isCloseTo(sensiVolExpected[i][0], offset(NOTIONAL * TOL));
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
