/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.function.BiFunction;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.TenorParameterMetadata;
import com.opengamma.strata.market.param.UnitParameterSensitivity;

/**
 * Test {@link ParameterizedFunctionalCurve}.
 */
@Test
public class ParameterizedFunctionalCurveTest {
  private static final DoubleArray PARAMETERS = DoubleArray.of(1.2, -10.4, 8.9);
  private static final CurveMetadata METADATA;
  static {
    TenorParameterMetadata param1 = TenorParameterMetadata.of(Tenor.TENOR_1Y);
    TenorParameterMetadata param2 = TenorParameterMetadata.of(Tenor.TENOR_5Y);
    TenorParameterMetadata param3 = TenorParameterMetadata.of(Tenor.TENOR_10Y);
    METADATA = DefaultCurveMetadata.builder()
        .curveName("test")
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .xValueType(ValueType.YEAR_FRACTION)
        .parameterMetadata(param1, param2, param3)
        .build();
  }
  private static final BiFunction<DoubleArray, Double, Double> VALUE_FUNCTION =
      new BiFunction<DoubleArray, Double, Double>() {
        @Override
        public Double apply(DoubleArray t, Double u) {
          return t.get(0) + Math.sin(t.get(1) + t.get(2) * u);
        }
      };
  private static final BiFunction<DoubleArray, Double, Double> DERIVATIVE_FUNCTION =
      new BiFunction<DoubleArray, Double, Double>() {
        @Override
        public Double apply(DoubleArray t, Double u) {
          return t.get(2) * Math.cos(t.get(1) + t.get(2) * u);
        }
      };
  private static final BiFunction<DoubleArray, Double, DoubleArray> SENSITIVITY_FUNCTION =
      new BiFunction<DoubleArray, Double, DoubleArray>() {
        @Override
        public DoubleArray apply(DoubleArray t, Double u) {
          return DoubleArray.of(1d, Math.cos(t.get(1) + t.get(2) * u), u * Math.cos(t.get(1) + t.get(2) * u));
        }
      };

  //-------------------------------------------------------------------------
  public void test_of() {
    ParameterizedFunctionalCurve test = ParameterizedFunctionalCurve.of(
        METADATA, PARAMETERS, VALUE_FUNCTION, DERIVATIVE_FUNCTION, SENSITIVITY_FUNCTION);
    assertEquals(test.getValueFunction(), VALUE_FUNCTION);
    assertEquals(test.getDerivativeFunction(), DERIVATIVE_FUNCTION);
    assertEquals(test.getSensitivityFunction(), SENSITIVITY_FUNCTION);
    assertEquals(test.getMetadata(), METADATA);
    assertEquals(test.getName(), METADATA.getCurveName());
    assertEquals(test.getParameter(2), PARAMETERS.get(2));
    assertEquals(test.getParameterCount(), PARAMETERS.size());
    assertEquals(test.getParameterMetadata(1), METADATA.getParameterMetadata(1));
    assertEquals(test.getParameters(), PARAMETERS);
  }

  //-------------------------------------------------------------------------

  public void test_withParameter() {
    ParameterizedFunctionalCurve base = ParameterizedFunctionalCurve.of(
        METADATA, PARAMETERS, VALUE_FUNCTION, DERIVATIVE_FUNCTION, SENSITIVITY_FUNCTION);
    DoubleArray parameters = DoubleArray.of(1.2, 1d, 8.9);
    ParameterizedFunctionalCurve expected = ParameterizedFunctionalCurve.of(
        METADATA, parameters, VALUE_FUNCTION, DERIVATIVE_FUNCTION, SENSITIVITY_FUNCTION);
    assertEquals(base.withParameter(1, 1d), expected);
  }

  public void test_withPerturbation() {
    ParameterizedFunctionalCurve base = ParameterizedFunctionalCurve.of(
        METADATA, PARAMETERS, VALUE_FUNCTION, DERIVATIVE_FUNCTION, SENSITIVITY_FUNCTION);
    DoubleArray parameters = PARAMETERS.minus(2d);
    ParameterizedFunctionalCurve expected = ParameterizedFunctionalCurve.of(
        METADATA, parameters, VALUE_FUNCTION, DERIVATIVE_FUNCTION, SENSITIVITY_FUNCTION);
    assertEquals(base.withPerturbation((i, v, m) -> v - 2d), expected);
  }

  public void test_withMetadata() {
    ParameterizedFunctionalCurve base = ParameterizedFunctionalCurve.of(
        METADATA, PARAMETERS, VALUE_FUNCTION, DERIVATIVE_FUNCTION, SENSITIVITY_FUNCTION);
    CurveMetadata metadata = DefaultCurveMetadata.builder()
        .curveName("test")
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .xValueType(ValueType.YEAR_FRACTION)
        .build();
    ParameterizedFunctionalCurve expected = ParameterizedFunctionalCurve.of(
        metadata, PARAMETERS, VALUE_FUNCTION, DERIVATIVE_FUNCTION, SENSITIVITY_FUNCTION);
    assertEquals(base.withMetadata(metadata), expected);
  }

  public void test_withParameters() {
    ParameterizedFunctionalCurve base = ParameterizedFunctionalCurve.of(
        METADATA, PARAMETERS, VALUE_FUNCTION, DERIVATIVE_FUNCTION, SENSITIVITY_FUNCTION);
    DoubleArray parameters = DoubleArray.of(1d, 2d, 3d);
    ParameterizedFunctionalCurve expected = ParameterizedFunctionalCurve.of(
        METADATA, parameters, VALUE_FUNCTION, DERIVATIVE_FUNCTION, SENSITIVITY_FUNCTION);
    assertEquals(base.withParameters(parameters), expected);
  }

  //-------------------------------------------------------------------------
  public void test_values() {
    ParameterizedFunctionalCurve test = ParameterizedFunctionalCurve.of(
        METADATA, PARAMETERS, VALUE_FUNCTION, DERIVATIVE_FUNCTION, SENSITIVITY_FUNCTION);
    double x = 5.2;
    assertEquals(test.yValue(x), VALUE_FUNCTION.apply(PARAMETERS, x));
    assertEquals(test.firstDerivative(x), DERIVATIVE_FUNCTION.apply(PARAMETERS, x));
    assertEquals(test.yValueParameterSensitivity(x), UnitParameterSensitivity.of(
        METADATA.getCurveName(), METADATA.getParameterMetadata().get(), SENSITIVITY_FUNCTION.apply(PARAMETERS, x)));
  }

  public void test_sensitivities() {
    ParameterizedFunctionalCurve test = ParameterizedFunctionalCurve.of(
        METADATA, PARAMETERS, VALUE_FUNCTION, DERIVATIVE_FUNCTION, SENSITIVITY_FUNCTION);
    DoubleArray sensiVal = DoubleArray.of(1d, 2d, 3d);
    assertEquals(test.createParameterSensitivity(sensiVal),
        UnitParameterSensitivity.of(METADATA.getCurveName(), METADATA.getParameterMetadata().get(), sensiVal));
    assertEquals(test.createParameterSensitivity(USD, sensiVal), 
        CurrencyParameterSensitivity.of(METADATA.getCurveName(), METADATA.getParameterMetadata().get(), USD, sensiVal));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ParameterizedFunctionalCurve test1 = ParameterizedFunctionalCurve.of(
        METADATA, PARAMETERS, VALUE_FUNCTION, DERIVATIVE_FUNCTION, SENSITIVITY_FUNCTION);
    coverImmutableBean(test1);

    DoubleArray params = DoubleArray.of(1.2);
    CurveMetadata metadata = DefaultCurveMetadata.builder()
        .curveName("test")
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .xValueType(ValueType.YEAR_FRACTION)
        .build();
    BiFunction<DoubleArray, Double, Double> value =
        new BiFunction<DoubleArray, Double, Double>() {
          @Override
          public Double apply(DoubleArray t, Double u) {
            return t.get(0) * u;
          }
        };
    BiFunction<DoubleArray, Double, Double> deriv =
        new BiFunction<DoubleArray, Double, Double>() {
          @Override
          public Double apply(DoubleArray t, Double u) {
            return t.get(0);
          }
        };
    BiFunction<DoubleArray, Double, DoubleArray> sensi =
        new BiFunction<DoubleArray, Double, DoubleArray>() {
          @Override
          public DoubleArray apply(DoubleArray t, Double u) {
            return DoubleArray.of(u);
          }
        };
    ParameterizedFunctionalCurve test2 = ParameterizedFunctionalCurve.of(
        metadata, params, value, deriv, sensi);
    coverBeanEquals(test1, test2);
  }

}
