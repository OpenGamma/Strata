/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.product.swap.SwapIndices.EUR_EURIBOR_1100_5Y;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.pricer.model.SabrInterestRateParameters;
import com.opengamma.strata.pricer.model.SabrVolatilityFormula;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;

/**
 * Data sets for testing SABR model for swaptions.
 */
public class SwaptionSabrRateVolatilityDataSet {
  /*
   * Interpolators
   */
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);

  /*
   * Data set used to test the pricers for physical delivery swaption.
   */
  static final FixedIborSwapConvention SWAP_CONVENTION_USD = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M;
  private static final double[] TIME_DSC_USD = new double[] {0.0027397260273972603, 0.005479452054794521,
      0.0958904109589041, 0.1726027397260274, 0.26301369863013696, 0.5123287671232877, 0.7643835616438356,
      1.0164383561643835, 2.0135040047907777, 3.010958904109589, 4.010958904109589, 5.016438356164383, 6.016236245227937,
      7.013698630136986, 8.01095890410959, 9.01095890410959, 10.010771764353619};
  private static final double[] RATE_DSC_USD = new double[] {0.0017743012430444162, 0.0016475657039787027,
      8.00944979276571E-4, 7.991342366517293E-4, 7.769429292812209E-4, 8.011052753850106E-4, 8.544769819435054E-4,
      0.0010101196182894087, 0.0025295133435066005, 0.005928027386129847, 0.009984669002766438, 0.013910233828705014,
      0.017362472692574276, 0.02026566836808523, 0.02272069332675379, 0.024782351990410997, 0.026505391310201288};
  private static final CurveName NAME_DSC_USD = CurveName.of("USD-DSCON");
  static final CurveMetadata META_DSC_USD = Curves.zeroRates(NAME_DSC_USD, ACT_ACT_ISDA);
  static final InterpolatedNodalCurve CURVE_DSC_USD = InterpolatedNodalCurve.of(
      META_DSC_USD, DoubleArray.copyOf(TIME_DSC_USD), DoubleArray.copyOf(RATE_DSC_USD), INTERPOLATOR);
  private static final double[] TIME_FWD_USD = new double[] {0.25205479452054796, 0.5013698630136987, 0.7534246575342466,
      1.010958904109589, 2.0107717643536196, 3.0054794520547947, 4.005479452054795, 5.005479452054795, 7.010958904109589,
      10.005307283479302, 12.01095890410959, 15.005479452054795, 20.005479452054793, 25.008219178082193, 30.01077176435362};
  private static final double[] RATE_FWD_USD = new double[] {0.002377379439054076, 0.002418692953929592,
      0.002500627386941208, 0.002647539893522339, 0.0044829589913700256, 0.008123927669512542, 0.012380488135102518,
      0.01644838699856555, 0.023026212753825423, 0.02933978147314773, 0.03208786808445587, 0.03475307015968317,
      0.03689179443401795, 0.03776622232525561, 0.03810645431268746};
  private static final CurveName NAME_FWD_USD = CurveName.of("USD-LIBOR3M");
  static final CurveMetadata META_FWD_USD = Curves.zeroRates(NAME_FWD_USD, ACT_ACT_ISDA);
  static final InterpolatedNodalCurve CURVE_FWD_USD = InterpolatedNodalCurve.of(
      META_FWD_USD, DoubleArray.copyOf(TIME_FWD_USD), DoubleArray.copyOf(RATE_FWD_USD), INTERPOLATOR);

  private static final double[] EXPIRY_NODE_USD = new double[] {
      0.0, 0.0, 0.0, 0.5, 0.5, 0.5, 1, 1, 1, 2, 2, 2, 5, 5, 5, 10, 10, 10};
  private static final double[] TENOR_NODE_USD = new double[] {
      1, 5, 10, 1, 5, 10, 1, 5, 10, 1, 5, 10, 1, 5, 10, 1, 5, 10};
  private static final double[] ALPHA_NODE_USD = new double[] {
      0.05, 0.05, 0.06, 0.05, 0.05, 0.06, 0.05, 0.05, 0.06, 0.05, 0.05, 0.06, 0.05, 0.05, 0.06, 0.05, 0.05, 0.06};
  private static final double[] BETA_NODE_USD = new double[] {
      0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5};
  private static final double[] RHO_NODE_USD = new double[] {
      -0.25, -0.25, -0.25, -0.25, -0.25, -0.25, -0.25, -0.25, 0, -0.25, -0.25, 0, -0.25, -0.25, 0, -0.25, -0.25, 0};
  private static final double[] NU_NODE_USD = new double[] {
      0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.3, 0.5, 0.5, 0.3, 0.5, 0.5, 0.3, 0.5, 0.5, 0.3};
  static final SwaptionVolatilitiesName NAME = SwaptionVolatilitiesName.of("Test-SABR");
  static final SurfaceMetadata META_ALPHA =
      Surfaces.sabrParameterByExpiryTenor("Test-SABR-Alpha", ACT_ACT_ISDA, ValueType.SABR_ALPHA);
  private static final InterpolatedNodalSurface SURFACE_ALPHA_USD = InterpolatedNodalSurface.of(
      META_ALPHA,
      DoubleArray.copyOf(EXPIRY_NODE_USD),
      DoubleArray.copyOf(TENOR_NODE_USD),
      DoubleArray.copyOf(ALPHA_NODE_USD),
      INTERPOLATOR_2D);
  private static final List<ParameterMetadata> PARAMETER_META_LIST_USD;
  static {
    int n = EXPIRY_NODE_USD.length;
    PARAMETER_META_LIST_USD = new ArrayList<ParameterMetadata>(n);
    for (int i = 0; i < n; ++i) {
      PARAMETER_META_LIST_USD.add(SwaptionSurfaceExpiryTenorParameterMetadata.of(EXPIRY_NODE_USD[i], TENOR_NODE_USD[i]));
    }
  }

  static final SurfaceMetadata META_BETA_USD =
      Surfaces.sabrParameterByExpiryTenor("Test-SABR-Beta", ACT_ACT_ISDA, ValueType.SABR_BETA)
          .withParameterMetadata(PARAMETER_META_LIST_USD);
  private static final InterpolatedNodalSurface SURFACE_BETA_USD = InterpolatedNodalSurface.of(
      META_BETA_USD,
      DoubleArray.copyOf(EXPIRY_NODE_USD),
      DoubleArray.copyOf(TENOR_NODE_USD),
      DoubleArray.copyOf(BETA_NODE_USD),
      INTERPOLATOR_2D);
  static final SurfaceMetadata META_RHO =
      Surfaces.sabrParameterByExpiryTenor("Test-SABR-Rho", ACT_ACT_ISDA, ValueType.SABR_RHO);
  private static final InterpolatedNodalSurface SURFACE_RHO_USD = InterpolatedNodalSurface.of(
      META_RHO,
      DoubleArray.copyOf(EXPIRY_NODE_USD),
      DoubleArray.copyOf(TENOR_NODE_USD),
      DoubleArray.copyOf(RHO_NODE_USD),
      INTERPOLATOR_2D);
  static final SurfaceMetadata META_NU =
      Surfaces.sabrParameterByExpiryTenor("Test-SABR-Nu", ACT_ACT_ISDA, ValueType.SABR_NU);
  private static final InterpolatedNodalSurface SURFACE_NU_USD = InterpolatedNodalSurface.of(
      META_NU,
      DoubleArray.copyOf(EXPIRY_NODE_USD),
      DoubleArray.copyOf(TENOR_NODE_USD),
      DoubleArray.copyOf(NU_NODE_USD),
      INTERPOLATOR_2D);
  static final SabrInterestRateParameters SABR_PARAM_USD = SabrInterestRateParameters.of(
      SURFACE_ALPHA_USD, SURFACE_BETA_USD, SURFACE_RHO_USD, SURFACE_NU_USD, SabrVolatilityFormula.hagan());

  static final double SHIFT = 0.025;
  private static final DefaultSurfaceMetadata META_SHIFT = DefaultSurfaceMetadata.of("Test-SABR-Shift");
  private static final ConstantSurface SURFACE_SHIFT_USD = ConstantSurface.of(META_SHIFT, SHIFT);
  static final SabrInterestRateParameters SABR_PARAM_SHIFT_USD = SabrInterestRateParameters.of(
      SURFACE_ALPHA_USD,
      SURFACE_BETA_USD,
      SURFACE_RHO_USD,
      SURFACE_NU_USD,
      SURFACE_SHIFT_USD,
      SabrVolatilityFormula.hagan());

  /**
   * Obtains {@code ImmutableRatesProvider} for specified valuation date.
   * 
   * @param valuationDate  the valuation date
   * @return the rates provider
   */
  public static ImmutableRatesProvider getRatesProviderUsd(LocalDate valuationDate) {
    return ImmutableRatesProvider.builder(valuationDate)
        .discountCurve(USD, CURVE_DSC_USD)
        .iborIndexCurve(USD_LIBOR_3M, CURVE_FWD_USD)
        .build();
  }

  /**
   * Obtains {@code SABRVolatilitySwaptionProvider} for specified valuation date.
   * 
   * @param valuationDate  the valuation date
   * @param shift  nonzero shift if true, zero shift otherwise
   * @return the volatility provider
   */
  public static SabrParametersSwaptionVolatilities getVolatilitiesUsd(LocalDate valuationDate, boolean shift) {
    ZonedDateTime dateTime = valuationDate.atStartOfDay(ZoneOffset.UTC);
    return shift ? SabrParametersSwaptionVolatilities.of(NAME, SWAP_CONVENTION_USD, dateTime, SABR_PARAM_SHIFT_USD)
        : SabrParametersSwaptionVolatilities.of(NAME, SWAP_CONVENTION_USD, dateTime, SABR_PARAM_USD);
  }

  /*
   * Data set used to test the pricers for cash settled swaption.
   */
  static final FixedIborSwapConvention SWAP_CONVENTION_EUR = FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M;
  private static final double[] TIME_DSC_EUR = new double[] {0.0, 0.5, 1.0, 2.0, 5.0, 10.0};
  private static final double[] RATE_DSC_EUR = new double[] {0.0150, 0.0125, 0.0150, 0.0175, 0.0150, 0.0150};
  private static final CurveName NAME_DSC_EUR = CurveName.of("EUR Dsc");
  static final CurveMetadata META_DSC_EUR = Curves.zeroRates(NAME_DSC_EUR, ACT_ACT_ISDA);
  public static final InterpolatedNodalCurve CURVE_DSC_EUR = InterpolatedNodalCurve.of(
      META_DSC_EUR, DoubleArray.copyOf(TIME_DSC_EUR), DoubleArray.copyOf(RATE_DSC_EUR), INTERPOLATOR);
  private static final double[] TIME_FWD_EUR = new double[] {0.0, 0.5, 1.0, 2.0, 5.0, 10.0};
  private static final double[] RATE_FWD_EUR = new double[] {0.0150, 0.0125, 0.0150, 0.0175, 0.0150, 0.0150};
  private static final CurveName NAME_FWD_EUR = CurveName.of("EUR EURIBOR 6M");
  static final CurveMetadata META_FWD_EUR = Curves.zeroRates(NAME_FWD_EUR, ACT_ACT_ISDA);
  public static final InterpolatedNodalCurve CURVE_FWD_EUR = InterpolatedNodalCurve.of(
      META_FWD_EUR, DoubleArray.copyOf(TIME_FWD_EUR), DoubleArray.copyOf(RATE_FWD_EUR), INTERPOLATOR);

  private static final double[] BETA_EXPIRY_NODE_EUR = new double[] {
      0, 0, 0, 0, 0.5, 0.5, 0.5, 0.5, 1, 1, 1, 1, 2, 2, 2, 2, 5, 5, 5, 5, 10, 10, 10, 10, 100, 100, 100, 100};
  private static final double[] BETA_TENOR_NODE_EUR = new double[] {
      0, 1, 10, 100, 0, 1, 10, 100, 0, 1, 10, 100, 0, 1, 10, 100, 0, 1, 10, 100, 0, 1, 10, 100, 0, 1, 10, 100};
  private static final InterpolatedNodalSurface SURFACE_ALPHA_EUR = InterpolatedNodalSurface.of(
      Surfaces.sabrParameterByExpiryTenor("Test-SABR-Alpha", ACT_ACT_ISDA, ValueType.SABR_ALPHA),
      DoubleArray.of(0, 0, 0, 0, 0.5, 0.5, 0.5, 0.5, 1, 1, 1, 1, 2, 2, 2, 2, 5, 5, 5, 5, 10, 10, 10, 10),
      DoubleArray.of(0, 1, 10, 100, 0, 1, 10, 100, 0, 1, 10, 100, 0, 1, 10, 100, 0, 1, 10, 100, 0, 1, 10, 100),
      DoubleArray.of(
          0.05, 0.05, 0.06, 0.06, 0.05, 0.05, 0.06, 0.06, 0.05, 0.05, 0.06, 0.06,
          0.05, 0.05, 0.06, 0.06, 0.05, 0.05, 0.06, 0.06, 0.05, 0.05, 0.06, 0.06),
      INTERPOLATOR_2D);
  private static final List<ParameterMetadata> PARAMETER_META_LIST_EUR;
  static {
    int n = BETA_TENOR_NODE_EUR.length;
    PARAMETER_META_LIST_EUR = new ArrayList<ParameterMetadata>(n);
    for (int i = 0; i < n; ++i) {
      PARAMETER_META_LIST_EUR.add(SwaptionSurfaceExpiryTenorParameterMetadata.of(BETA_EXPIRY_NODE_EUR[i],
          BETA_TENOR_NODE_EUR[i]));
    }
  }

  static final SurfaceMetadata META_BETA_EUR =
      Surfaces.sabrParameterByExpiryTenor("Test-SABR-Beta", ACT_ACT_ISDA, ValueType.SABR_BETA)
          .withParameterMetadata(PARAMETER_META_LIST_EUR);
  private static final InterpolatedNodalSurface SURFACE_BETA_EUR = InterpolatedNodalSurface.of(
      META_BETA_EUR,
      DoubleArray.copyOf(BETA_EXPIRY_NODE_EUR),
      DoubleArray.copyOf(BETA_TENOR_NODE_EUR),
      DoubleArray.copyOf(new double[] {0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5,
          0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5}),
      INTERPOLATOR_2D);
  private static final InterpolatedNodalSurface SURFACE_RHO_EUR = InterpolatedNodalSurface.of(
      Surfaces.sabrParameterByExpiryTenor("Test-SABR-Rho", ACT_ACT_ISDA, ValueType.SABR_RHO),
      DoubleArray.of(
          0, 0, 0, 0, 0.5, 0.5, 0.5, 0.5, 1, 1, 1, 1, 2, 2, 2, 2, 5, 5, 5, 5, 10, 10, 10, 10, 100, 100, 100, 100),
      DoubleArray.of(
          0, 1, 10, 100, 0, 1, 10, 100, 0, 1, 10, 100, 0, 1, 10, 100, 0, 1, 10, 100, 0, 1, 10, 100, 0, 1, 10, 100),
      DoubleArray.of(
          -0.25, -0.25, 0, 0,
          -0.25, -0.25, 0, 0,
          -0.25, -0.25, 0, 0,
          -0.25, -0.25, 0, 0,
          -0.25, -0.25, 0, 0,
          -0.25, -0.25, 0, 0,
          -0.25, -0.25, 0, 0),
      INTERPOLATOR_2D);
  private static final InterpolatedNodalSurface SURFACE_NU_EUR = InterpolatedNodalSurface.of(
      Surfaces.sabrParameterByExpiryTenor("Test-SABR-Nu", ACT_ACT_ISDA, ValueType.SABR_NU),
      DoubleArray.of(
          0, 0, 0, 0, 0.5, 0.5, 0.5, 0.5, 1, 1, 1, 1, 2, 2, 2, 2, 5, 5, 5, 5, 10, 10, 10, 10, 100, 100, 100, 100),
      DoubleArray.of(
          0, 1, 10, 100, 0, 1, 10, 100, 0, 1, 10, 100, 0, 1, 10, 100, 0, 1, 10, 100, 0, 1, 10, 100, 0, 1, 10, 100),
      DoubleArray.of(
          0.50, 0.50, 0.30, 0.30,
          0.50, 0.50, 0.30, 0.30,
          0.50, 0.50, 0.30, 0.30,
          0.50, 0.50, 0.30, 0.30,
          0.50, 0.50, 0.30, 0.30,
          0.50, 0.50, 0.30, 0.30,
          0.50, 0.50, 0.30, 0.30),
      INTERPOLATOR_2D);
  private static final ConstantSurface SURFACE_SHIFT_EUR = ConstantSurface.of(META_SHIFT, SHIFT);

  static final SabrInterestRateParameters SABR_PARAM_EUR = SabrInterestRateParameters.of(
      SURFACE_ALPHA_EUR, SURFACE_BETA_EUR, SURFACE_RHO_EUR, SURFACE_NU_EUR, SabrVolatilityFormula.hagan());
  static final SabrInterestRateParameters SABR_PARAM_SHIFT_EUR = SabrInterestRateParameters.of(SURFACE_ALPHA_EUR,
      SURFACE_BETA_EUR, SURFACE_RHO_EUR, SURFACE_NU_EUR, SURFACE_SHIFT_EUR, SabrVolatilityFormula.hagan());

  /**
   * Obtains {@code ImmutableRatesProvider} for specified valuation date.
   * 
   * @param valuationDate  the valuation date
   * @return the rates provider
   */
  public static ImmutableRatesProvider getRatesProviderEur(LocalDate valuationDate) {
    return ImmutableRatesProvider.builder(valuationDate)
        .discountCurve(EUR, CURVE_DSC_EUR)
        .iborIndexCurve(EUR_EURIBOR_6M, CURVE_FWD_EUR)
        .build();
  }

  /**
   * Obtains {@code ImmutableRatesProvider} for specified valuation date and time series of swap index.
   * 
   * @param valuationDate  the valuation date
   * @param timeSeries  the time series
   * @return the rates provider
   */
  public static ImmutableRatesProvider getRatesProviderEur(LocalDate valuationDate, LocalDateDoubleTimeSeries timeSeries) {
    return ImmutableRatesProvider.builder(valuationDate)
        .discountCurve(EUR, CURVE_DSC_EUR)
        .iborIndexCurve(EUR_EURIBOR_6M, CURVE_FWD_EUR)
        .timeSeries(EUR_EURIBOR_1100_5Y, timeSeries)
        .build();
  }

  /**
   * Obtains {@code SABRVolatilitySwaptionProvider} for specified valuation date.
   * 
   * @param valuationDate  the valuation date
   * @param shift  nonzero shift if true, zero shift otherwise
   * @return the volatility provider
   */
  public static SabrParametersSwaptionVolatilities getVolatilitiesEur(LocalDate valuationDate, boolean shift) {
    ZonedDateTime dateTime = valuationDate.atStartOfDay(ZoneOffset.UTC);
    return shift ? SabrParametersSwaptionVolatilities.of(NAME, SWAP_CONVENTION_EUR, dateTime, SABR_PARAM_SHIFT_EUR)
        : SabrParametersSwaptionVolatilities.of(NAME, SWAP_CONVENTION_EUR, dateTime, SABR_PARAM_EUR);
  }
}
