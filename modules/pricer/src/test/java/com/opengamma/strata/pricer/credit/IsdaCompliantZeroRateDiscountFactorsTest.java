/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365L;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.SimpleCurveParameterMetadata;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * Test {@link IsdaCreditDiscountFactors}.
 */
public class IsdaCompliantZeroRateDiscountFactorsTest {

  private static final LocalDate VALUATION = LocalDate.of(2016, 5, 6);
  private static final DoubleArray TIME = DoubleArray.ofUnsafe(new double[] {
      0.09041095890410959, 0.16712328767123288, 0.2547945205479452, 0.5041095890410959, 0.7534246575342466, 1.0054794520547945,
      2.0054794520547947, 3.008219178082192, 4.013698630136987, 5.010958904109589, 6.008219178082192, 7.010958904109589,
      8.01095890410959, 9.01095890410959, 10.016438356164384, 12.013698630136986, 15.021917808219179, 20.01917808219178,
      30.024657534246575});
  private static final DoubleArray RATE = DoubleArray.ofUnsafe(new double[] {
      -0.002078655697855299, -0.001686438401304855, -0.0013445486228483379, -4.237819925898475E-4, 2.5142499469348057E-5,
      5.935063895780138E-4, -3.247081037469503E-4, 6.147182786549223E-4, 0.0019060597240545122, 0.0033125742254568815,
      0.0047766352312329455, 0.0062374324537341225, 0.007639664176639106, 0.008971003650150983, 0.010167545380711455,
      0.012196853322376243, 0.01441082634734099, 0.016236611610989507, 0.01652439910865982});
  private static final DefaultCurveMetadata METADATA = DefaultCurveMetadata.builder()
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.ZERO_RATE)
      .curveName("yieldUsd")
      .dayCount(ACT_365F)
      .build();
  private static final InterpolatedNodalCurve CURVE = InterpolatedNodalCurve.of(METADATA, TIME, RATE,
      CurveInterpolators.PRODUCT_LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.PRODUCT_LINEAR);
  private static final DefaultCurveMetadata METADATA_SINGLE = DefaultCurveMetadata.builder()
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.ZERO_RATE)
      .curveName("yieldUsd")
      .dayCount(ACT_365L)
      .build();
  private static final double TIME_SINGLE = 1.5d;
  private static final double RATE_SINGLE = 0.01;
  private static final ConstantNodalCurve CONST_CURVE = ConstantNodalCurve.of(METADATA_SINGLE, TIME_SINGLE, RATE_SINGLE);
  private static final LocalDate DATE_AFTER = LocalDate.of(2017, 2, 24);

  @Test
  public void test_of_constant() {
    IsdaCreditDiscountFactors test = IsdaCreditDiscountFactors.of(USD, VALUATION, CONST_CURVE);
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getCurve()).isEqualTo(CONST_CURVE);
    assertThat(test.getDayCount()).isEqualTo(ACT_365L);
    assertThat(test.getParameterCount()).isEqualTo(1);
    assertThat(test.getParameter(0)).isEqualTo(RATE_SINGLE);
    assertThat(test.getParameterKeys()).isEqualTo(DoubleArray.of(TIME_SINGLE));
    assertThat(test.getParameterMetadata(0)).isEqualTo(SimpleCurveParameterMetadata.of(METADATA.getXValueType(), TIME_SINGLE));
    assertThat(test.getValuationDate()).isEqualTo(VALUATION);
    assertThat(test.findData(CONST_CURVE.getName())).isEqualTo(Optional.of(CONST_CURVE));
    assertThat(test.findData(CurveName.of("Rubbish"))).isEqualTo(Optional.empty());
    assertThat(test.toDiscountFactors()).isEqualTo(ZeroRateDiscountFactors.of(USD, VALUATION, CONST_CURVE));
    assertThat(test.isIsdaCompliant()).isTrue();
  }

  @Test
  public void test_of() {
    IsdaCreditDiscountFactors test = IsdaCreditDiscountFactors.of(USD, VALUATION, CURVE);
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getCurve()).isEqualTo(CURVE);
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getParameterCount()).isEqualTo(RATE.size());
    assertThat(test.getParameter(3)).isEqualTo(RATE.get(3));
    assertThat(test.getParameter(1)).isEqualTo(RATE.get(1));
    assertThat(test.getParameterKeys()).isEqualTo(TIME);
    assertThat(test.getParameterMetadata(4)).isEqualTo(SimpleCurveParameterMetadata.of(METADATA.getXValueType(), TIME.get(4)));
    assertThat(test.getParameterMetadata(6)).isEqualTo(SimpleCurveParameterMetadata.of(METADATA.getXValueType(), TIME.get(6)));
    assertThat(test.getValuationDate()).isEqualTo(VALUATION);
    assertThat(test.findData(CURVE.getName())).isEqualTo(Optional.of(CURVE));
    assertThat(test.findData(CurveName.of("Rubbish"))).isEqualTo(Optional.empty());
    assertThat(test.toDiscountFactors()).isEqualTo(ZeroRateDiscountFactors.of(USD, VALUATION, CURVE));
    assertThat(test.isIsdaCompliant()).isTrue();
  }

  @Test
  public void test_of_constant_interface() {
    IsdaCreditDiscountFactors test =
        (IsdaCreditDiscountFactors) CreditDiscountFactors.of(USD, VALUATION, CONST_CURVE);
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getCurve()).isEqualTo(CONST_CURVE);
    assertThat(test.getDayCount()).isEqualTo(ACT_365L);
    assertThat(test.getParameterCount()).isEqualTo(1);
    assertThat(test.getParameter(0)).isEqualTo(RATE_SINGLE);
    assertThat(test.getParameterKeys()).isEqualTo(DoubleArray.of(TIME_SINGLE));
    assertThat(test.getParameterMetadata(0)).isEqualTo(SimpleCurveParameterMetadata.of(METADATA.getXValueType(), TIME_SINGLE));
    assertThat(test.getValuationDate()).isEqualTo(VALUATION);
    assertThat(test.findData(CONST_CURVE.getName())).isEqualTo(Optional.of(CONST_CURVE));
    assertThat(test.findData(CurveName.of("Rubbish"))).isEqualTo(Optional.empty());
    assertThat(test.toDiscountFactors()).isEqualTo(ZeroRateDiscountFactors.of(USD, VALUATION, CONST_CURVE));
    assertThat(test.isIsdaCompliant()).isTrue();
  }

  @Test
  public void test_of_interface() {
    IsdaCreditDiscountFactors test =
        (IsdaCreditDiscountFactors) CreditDiscountFactors.of(USD, VALUATION, CURVE);
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getCurve()).isEqualTo(CURVE);
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getParameterCount()).isEqualTo(RATE.size());
    assertThat(test.getParameter(3)).isEqualTo(RATE.get(3));
    assertThat(test.getParameter(1)).isEqualTo(RATE.get(1));
    assertThat(test.getParameterKeys()).isEqualTo(TIME);
    assertThat(test.getParameterMetadata(4)).isEqualTo(SimpleCurveParameterMetadata.of(METADATA.getXValueType(), TIME.get(4)));
    assertThat(test.getParameterMetadata(6)).isEqualTo(SimpleCurveParameterMetadata.of(METADATA.getXValueType(), TIME.get(6)));
    assertThat(test.getValuationDate()).isEqualTo(VALUATION);
    assertThat(test.findData(CURVE.getName())).isEqualTo(Optional.of(CURVE));
    assertThat(test.findData(CurveName.of("Rubbish"))).isEqualTo(Optional.empty());
    assertThat(test.toDiscountFactors()).isEqualTo(ZeroRateDiscountFactors.of(USD, VALUATION, CURVE));
    assertThat(test.isIsdaCompliant()).isTrue();
  }

  @Test
  public void test_ofValue() {
    IsdaCreditDiscountFactors test = IsdaCreditDiscountFactors.of(
        USD, VALUATION, METADATA_SINGLE.getCurveName(), DoubleArray.of(TIME_SINGLE), DoubleArray.of(RATE_SINGLE), ACT_365L);
    IsdaCreditDiscountFactors expected = IsdaCreditDiscountFactors.of(USD, VALUATION, CONST_CURVE);
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_ofValues() {
    IsdaCreditDiscountFactors test = IsdaCreditDiscountFactors.of(
        USD, VALUATION, METADATA.getCurveName(), TIME, RATE, ACT_365F);
    IsdaCreditDiscountFactors expected = IsdaCreditDiscountFactors.of(USD, VALUATION, CURVE);
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_of_fail() {
    DefaultCurveMetadata metadata = DefaultCurveMetadata.builder()
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .curveName("yieldUsd")
        .build();
    InterpolatedNodalCurve curveNoDcc = InterpolatedNodalCurve.of(metadata, TIME, RATE,
        CurveInterpolators.PRODUCT_LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.PRODUCT_LINEAR);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IsdaCreditDiscountFactors.of(USD, VALUATION, curveNoDcc));
    InterpolatedNodalCurve curveWrongLeft = InterpolatedNodalCurve.of(METADATA, TIME, RATE,
        CurveInterpolators.PRODUCT_LINEAR, CurveExtrapolators.PRODUCT_LINEAR, CurveExtrapolators.PRODUCT_LINEAR);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IsdaCreditDiscountFactors.of(USD, VALUATION, curveWrongLeft));
    InterpolatedNodalCurve curveWrongInterp = InterpolatedNodalCurve.of(METADATA, TIME, RATE,
        CurveInterpolators.NATURAL_SPLINE, CurveExtrapolators.FLAT, CurveExtrapolators.PRODUCT_LINEAR);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IsdaCreditDiscountFactors.of(USD, VALUATION, curveWrongInterp));
    InterpolatedNodalCurve curveWrongRight = InterpolatedNodalCurve.of(METADATA, TIME, RATE,
        CurveInterpolators.PRODUCT_LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.FLAT);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IsdaCreditDiscountFactors.of(USD, VALUATION, curveWrongRight));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_discountFactor() {
    IsdaCreditDiscountFactors test = IsdaCreditDiscountFactors.of(USD, VALUATION, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(VALUATION, DATE_AFTER);
    double expected = Math.exp(-relativeYearFraction * CURVE.yValue(relativeYearFraction));
    assertThat(test.discountFactor(DATE_AFTER)).isEqualTo(expected);
  }

  @Test
  public void test_zeroRate() {
    IsdaCreditDiscountFactors test = IsdaCreditDiscountFactors.of(USD, VALUATION, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(VALUATION, DATE_AFTER);
    double discountFactor = test.discountFactor(DATE_AFTER);
    double zeroRate = test.zeroRate(DATE_AFTER);
    assertThat(Math.exp(-zeroRate * relativeYearFraction)).isEqualTo(discountFactor);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_zeroRatePointSensitivity() {
    IsdaCreditDiscountFactors test = IsdaCreditDiscountFactors.of(USD, VALUATION, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(VALUATION, DATE_AFTER);
    double df = Math.exp(-relativeYearFraction * CURVE.yValue(relativeYearFraction));
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(USD, relativeYearFraction, -df * relativeYearFraction);
    assertThat(test.zeroRatePointSensitivity(DATE_AFTER)).isEqualTo(expected);
  }

  @Test
  public void test_zeroRatePointSensitivity_sensitivityCurrency() {
    IsdaCreditDiscountFactors test = IsdaCreditDiscountFactors.of(USD, VALUATION, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(VALUATION, DATE_AFTER);
    double df = Math.exp(-relativeYearFraction * CURVE.yValue(relativeYearFraction));
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(USD, relativeYearFraction, GBP, -df * relativeYearFraction);
    assertThat(test.zeroRatePointSensitivity(DATE_AFTER, GBP)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_unitParameterSensitivity() {
    IsdaCreditDiscountFactors test = IsdaCreditDiscountFactors.of(USD, VALUATION, CURVE);
    ZeroRateSensitivity sens = test.zeroRatePointSensitivity(DATE_AFTER);

    double relativeYearFraction = ACT_365F.relativeYearFraction(VALUATION, DATE_AFTER);
    CurrencyParameterSensitivities expected = CurrencyParameterSensitivities.of(
        CURVE.yValueParameterSensitivity(relativeYearFraction)
            .multipliedBy(sens.getCurrency(), sens.getSensitivity()));
    assertThat(test.parameterSensitivity(sens)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  // proper end-to-end FD tests are in pricer test
  @Test
  public void test_parameterSensitivity() {
    IsdaCreditDiscountFactors test = IsdaCreditDiscountFactors.of(USD, VALUATION, CURVE);
    ZeroRateSensitivity point = ZeroRateSensitivity.of(USD, 1d, 1d);
    assertThat(test.parameterSensitivity(point).size()).isEqualTo(1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createParameterSensitivity() {
    IsdaCreditDiscountFactors test = IsdaCreditDiscountFactors.of(USD, VALUATION, CURVE);
    DoubleArray sensitivities = DoubleArray.of(0.12, 0.1, 0.49, 0.15, 0.56, 0.17, 0.32, 0.118, 0.456, 5.0, 12.0, 0.65,
        0.34, 0.75, 0.12, 0.15, 0.12, 0.15, 0.04);
    CurrencyParameterSensitivities sens = test.createParameterSensitivity(USD, sensitivities);
    assertThat(sens.getSensitivities().get(0)).isEqualTo(CURVE.createParameterSensitivity(USD, sensitivities));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withCurve() {
    IsdaCreditDiscountFactors test =
        IsdaCreditDiscountFactors.of(USD, VALUATION, CURVE).withCurve(CONST_CURVE);
    assertThat(test.getCurve()).isEqualTo(CONST_CURVE);
    assertThat(test.getDayCount()).isEqualTo(ACT_365L);
  }

  @Test
  public void test_withParameter() {
    IsdaCreditDiscountFactors test =
        IsdaCreditDiscountFactors.of(USD, VALUATION, CURVE).withParameter(1, 0.55);
    IsdaCreditDiscountFactors exp =
        IsdaCreditDiscountFactors.of(USD, VALUATION, CURVE.withParameter(1, 0.55));
    assertThat(test).isEqualTo(exp);
  }

  @Test
  public void test_withPerturbation() {
    IsdaCreditDiscountFactors test =
        IsdaCreditDiscountFactors.of(USD, VALUATION, CURVE).withPerturbation((i, v, m) -> v + 1d);
    IsdaCreditDiscountFactors exp =
        IsdaCreditDiscountFactors.of(USD, VALUATION, CURVE.withPerturbation((i, v, m) -> v + 1d));
    assertThat(test).isEqualTo(exp);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    IsdaCreditDiscountFactors test1 = IsdaCreditDiscountFactors.of(USD, VALUATION, CURVE);
    coverImmutableBean(test1);
    IsdaCreditDiscountFactors test2 =
        IsdaCreditDiscountFactors.of(GBP, VALUATION.plusDays(1), CONST_CURVE);
    coverBeanEquals(test1, test2);
  }

}
