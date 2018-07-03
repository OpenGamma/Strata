/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.market.curve.CurveInfoType.JACOBIAN;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveParameterSize;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.JacobianCalibrationMatrix;
import com.opengamma.strata.market.curve.LegalEntityGroup;
import com.opengamma.strata.market.curve.RepoGroup;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.bond.ImmutableLegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.curve.CalibrationDiscountingSimpleEur3Test;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;

/**
 * Test {@link MarketQuoteSensitivityCalculator}.
 * <p>
 * Market quote sensitivity calculations with {@code RatesProvider}, {@code CreditRatesProvider} are tested in other unit tests, 
 * e.g., {@link CalibrationDiscountingSimpleEur3Test}, {@code SpreadSensitivityCalculatorTest}, together with curve calibrations.
 */
@Test
public class MarketQuoteSensitivityCalculatorTest {

  private static final LocalDate DATE = date(2017, 12, 11);
  private static final SecurityId ID_SECURITY = SecurityId.of("OG-Ticker", "Bond-5Y");
  private static final RepoGroup GROUP_REPO_SECURITY = RepoGroup.of("ISSUER1 BND 5Y");
  private static final LegalEntityGroup GROUP_ISSUER = LegalEntityGroup.of("ISSUER1");
  private static final LegalEntityId ID_ISSUER = LegalEntityId.of("OG-Ticker", "Issuer-1");
  private static final MarketQuoteSensitivityCalculator CALC = MarketQuoteSensitivityCalculator.DEFAULT;
  private static final MatrixAlgebra MATRIX_ALGEBRA = new OGMatrixAlgebra();
  // curve data
  private static final CurveName CURVE_NAME_1 = CurveName.of("market1");
  private static final DoubleArray SENSI_1 = DoubleArray.of(1d, 2d, 3d);
  private static final DoubleArray X_VALUES_1 = DoubleArray.of(0.5d, 1d, 3d);
  private static final DoubleArray Y_VALUES_1 = DoubleArray.of(0.05d, 0.04d, 0.03d);
  private static final CurveParameterSize SIZE_1 = CurveParameterSize.of(CURVE_NAME_1, X_VALUES_1.size());
  private static final CurveName CURVE_NAME_2 = CurveName.of("market2");
  private static final DoubleArray SENSI_2 = DoubleArray.of(-3d, -6d, 4d, 2d);
  private static final DoubleArray X_VALUES_2 = DoubleArray.of(1d, 5d, 7d, 10d);
  private static final DoubleArray Y_VALUES_2 = DoubleArray.of(0.1d, 0.05d, -0.08d, -0.01d);
  private static final CurveParameterSize SIZE_2 = CurveParameterSize.of(CURVE_NAME_2, X_VALUES_2.size());
  // interpolated curves
  private static final double[][] MATRIX_1 = new double[][] {
      {1.5d, 0d, 0d, 0d, 0d, 0d, 0d}, {0d, 1.2d, 0d, 0d, 0d, 0d, 0d}, {0d, 0d, 1.3d, 0d, 0d, 0d, 0d}};
  private static final double[][] MATRIX_11 = new double[][] {
      {1.5d, 0d, 0d}, {0d, 1.2d, 0d}, {0d, 0d, 1.3d}};
  private static final double[][] MATRIX_12 = new double[][] {
      {0d, 0d, 0d, 0d}, {0d, 0d, 0d, 0d}, {0d, 0d, 0d, 0d}};
  private static final JacobianCalibrationMatrix JACONIAN_MATRIX_1 =
      JacobianCalibrationMatrix.of(ImmutableList.of(SIZE_1, SIZE_2), DoubleMatrix.copyOf(MATRIX_1));
  private static final CurveMetadata METADATA_1 = Curves.zeroRates(CURVE_NAME_1, ACT_365F).withInfo(JACOBIAN, JACONIAN_MATRIX_1);
  private static final InterpolatedNodalCurve CURVE_1 =
      InterpolatedNodalCurve.of(METADATA_1, X_VALUES_1, Y_VALUES_1, LINEAR);
  private static final double[][] MATRIX_2 = new double[][] {
      {1.5d, 0.5d, 0.1d, 2d, 0d, 0d, 0d}, {0.2d, 1.2d, 0.9d, 0d, 1.5d, 0d, 0d},
      {0.1d, 0.5d, 1.0d, 0d, 0d, 1.1d, 0d}, {0d, 0.2d, 1.2d, 0d, 0d, 0d, 1.1d}};
  private static final double[][] MATRIX_21 = new double[][] {
      {1.5d, 0.5d, 0.1d}, {0.2d, 1.2d, 0.9d}, {0.1d, 0.5d, 1.0d}, {0d, 0.2d, 1.2d}};
  private static final double[][] MATRIX_22 = new double[][] {
      {2d, 0d, 0d, 0d}, {0d, 1.5d, 0d, 0d}, {0d, 0d, 1.1d, 0d}, {0d, 0d, 0d, 1.1d}};
  private static final JacobianCalibrationMatrix JACONIAN_MATRIX_2 =
      JacobianCalibrationMatrix.of(ImmutableList.of(SIZE_1, SIZE_2), DoubleMatrix.copyOf(MATRIX_2));
  private static final CurveMetadata METADATA_2 = Curves.zeroRates(CURVE_NAME_2, ACT_365F).withInfo(JACOBIAN, JACONIAN_MATRIX_2);
  private static final InterpolatedNodalCurve CURVE_2 =
      InterpolatedNodalCurve.of(METADATA_2, X_VALUES_2, Y_VALUES_2, LINEAR);
  // sensitivities and provider
  private static final CurrencyParameterSensitivities PARAMETER_SENSITIVITIES;
  private static final ImmutableLegalEntityDiscountingProvider PROVIDER;
  static {
    CurrencyParameterSensitivity sensi1 = CurrencyParameterSensitivity.of(CURVE_NAME_1, USD, SENSI_1);
    CurrencyParameterSensitivity sensi2 = CurrencyParameterSensitivity.of(CURVE_NAME_2, GBP, SENSI_2);
    ZeroRateDiscountFactors dscIssuer = ZeroRateDiscountFactors.of(USD, DATE, CURVE_1);
    ZeroRateDiscountFactors dscRepo = ZeroRateDiscountFactors.of(GBP, DATE, CURVE_2);
    PARAMETER_SENSITIVITIES = CurrencyParameterSensitivities.of(sensi1, sensi2);
    PROVIDER = ImmutableLegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.of(Pair.of(GROUP_ISSUER, USD), dscIssuer))
        .issuerCurveGroups(ImmutableMap.of(ID_ISSUER, GROUP_ISSUER))
        .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO_SECURITY, GBP), dscRepo))
        .repoCurveSecurityGroups(ImmutableMap.of(ID_SECURITY, GROUP_REPO_SECURITY))
        .build();
  }

  private static final double TOL = 1.0e-14;

  public void test_sensitivity_LegalEntityDiscountingProvider() {
    CurrencyParameterSensitivities computed = CALC.sensitivity(PARAMETER_SENSITIVITIES, PROVIDER);
    assertEquals(computed.getSensitivities().size(), 4);
    DoubleArray expected11 = (DoubleArray) MATRIX_ALGEBRA.multiply(SENSI_1, DoubleMatrix.copyOf(MATRIX_11));
    DoubleArray expected12 = (DoubleArray) MATRIX_ALGEBRA.multiply(SENSI_1, DoubleMatrix.copyOf(MATRIX_12));
    DoubleArray expected21 = (DoubleArray) MATRIX_ALGEBRA.multiply(SENSI_2, DoubleMatrix.copyOf(MATRIX_21));
    DoubleArray expected22 = (DoubleArray) MATRIX_ALGEBRA.multiply(SENSI_2, DoubleMatrix.copyOf(MATRIX_22));
    assertTrue(computed.getSensitivity(CURVE_NAME_1, USD).getSensitivity().equalWithTolerance(expected11, TOL));
    assertTrue(computed.getSensitivity(CURVE_NAME_1, GBP).getSensitivity().equalWithTolerance(expected21, TOL));
    assertTrue(computed.getSensitivity(CURVE_NAME_2, USD).getSensitivity().equalWithTolerance(expected12, TOL));
    assertTrue(computed.getSensitivity(CURVE_NAME_2, GBP).getSensitivity().equalWithTolerance(expected22, TOL));
  }

}
