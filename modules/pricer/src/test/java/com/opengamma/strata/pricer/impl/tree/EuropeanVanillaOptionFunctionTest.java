/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.pricer.impl.option.BlackScholesFormulaRepository;
import com.opengamma.strata.product.common.PutCall;

/**
 * Test {@link EuropeanVanillaOptionFunction}.
 */
public class EuropeanVanillaOptionFunctionTest {

  private static final double STRIKE = 130d;
  private static final double TIME_TO_EXPIRY = 0.257;
  private static final int NUM = 35;

  @Test
  public void test_of() {
    EuropeanVanillaOptionFunction test = EuropeanVanillaOptionFunction.of(STRIKE, TIME_TO_EXPIRY, PutCall.PUT, NUM);
    assertThat(test.getSign()).isEqualTo(-1d);
    assertThat(test.getStrike()).isEqualTo(STRIKE);
    assertThat(test.getTimeToExpiry()).isEqualTo(TIME_TO_EXPIRY);
    assertThat(test.getNumberOfSteps()).isEqualTo(NUM);
  }

  @Test
  public void test_optionPrice() {
    double tol = 1.0e-12;
    EuropeanVanillaOptionFunction test = EuropeanVanillaOptionFunction.of(STRIKE, TIME_TO_EXPIRY, PutCall.PUT, NUM);
    double spot = 100d;
    double u = 1.05;
    double d = 0.98;
    double m = Math.sqrt(u * d);
    double up = 0.29;
    double dp = 0.25;
    double mp = 1d - up - dp;
    // test getPayoffAtExpiryTrinomial
    DoubleArray computedPayoff = test.getPayoffAtExpiryTrinomial(spot, d, m);
    int expectedSize = 2 * NUM + 1;
    assertThat(computedPayoff.size()).isEqualTo(expectedSize);
    for (int i = 0; i < expectedSize; ++i) {
      double price = spot * Math.pow(u, 0.5 * i) * Math.pow(d, NUM - 0.5 * i);
      double expectedPayoff = Math.max(STRIKE - price, 0d);
      assertThat(computedPayoff.get(i)).isCloseTo(expectedPayoff, offset(tol));
    }
    // test getNextOptionValues
    double df = 0.92;
    int n = 2;
    DoubleArray values = DoubleArray.of(1.4, 0.9, 0.1, 0.05, 0.0, 0.0, 0.0);
    DoubleArray computedNextValues = test.getNextOptionValues(df, up, mp, dp, values, spot, d, m, n);
    DoubleArray expectedNextValues = DoubleArray.of(
        df * (1.4 * dp + 0.9 * mp + 0.1 * up),
        df * (0.9 * dp + 0.1 * mp + 0.05 * up),
        df * (0.1 * dp + 0.05 * mp),
        df * 0.05 * dp,
        0.0);
    assertThat(DoubleArrayMath.fuzzyEquals(computedNextValues.toArray(), expectedNextValues.toArray(), tol)).isTrue();
  }

  private static final TrinomialTree TRINOMIAL_TREE = new TrinomialTree();
  private static final double SPOT = 105.;
  private static final double[] STRIKES = new double[] {81., 97., 105., 105.1, 114., 128. };
  private static final double TIME = 1.25;
  private static final double[] INTERESTS = new double[] {-0.01, 0.0, 0.05 };
  private static final double[] VOLS = new double[] {0.05, 0.1, 0.5 };
  private static final double[] DIVIDENDS = new double[] {0.0, 0.02 };

  @Test
  public void test_trinomialTree() {
    int nSteps = 135;
    LatticeSpecification[] lattices = new LatticeSpecification[]
    {new CoxRossRubinsteinLatticeSpecification(), new TrigeorgisLatticeSpecification() };
    double tol = 5.0e-3;
    for (boolean isCall : new boolean[] {true, false }) {
      for (double strike : STRIKES) {
        for (double interest : INTERESTS) {
          for (double vol : VOLS) {
            for (double dividend : DIVIDENDS) {
              OptionFunction function = EuropeanVanillaOptionFunction.of(strike, TIME, PutCall.ofPut(!isCall), nSteps);
              double exact =
                  BlackScholesFormulaRepository.price(SPOT, strike, TIME, vol, interest, interest - dividend, isCall);
              for (LatticeSpecification lattice : lattices) {
                double computed = TRINOMIAL_TREE.optionPrice(function, lattice, SPOT, vol, interest, dividend);
                assertThat(computed).isCloseTo(exact, offset(Math.max(exact, 1d) * tol));
              }
            }
          }
        }
      }
    }
  }

}
