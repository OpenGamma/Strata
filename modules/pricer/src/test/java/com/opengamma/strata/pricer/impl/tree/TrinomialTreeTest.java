/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.tree;

import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.pricer.fxopt.RecombiningTrinomialTreeData;
import com.opengamma.strata.product.common.PutCall;

/**
 * Test {@link TrinomialTree}.
 * <p>
 * Further tests are done for implementations of {@code OptionFunction}. See their test classes.
 */
@Test
public class TrinomialTreeTest {

  private static final TrinomialTree TRINOMIAL_TREE = new TrinomialTree();
  private static final double SPOT = 105.;
  private static final double[] STRIKES = new double[] {81., 97., 105., 105.1, 114., 128. };
  private static final double TIME = 1.25;
  private static final double[] INTERESTS = new double[] {-0.01, 0.0, 0.05 };
  private static final double[] VOLS = new double[] {0.05, 0.1, 0.5 };
  private static final double[] DIVIDENDS = new double[] {0.0, 0.02 };

  /**
   * Test consistency between price methods, and Greek via finite difference.
   */
  public void test_trinomialTree() {
    int nSteps = 135;
    double dt = TIME / nSteps;
    LatticeSpecification lattice = new CoxRossRubinsteinLatticeSpecification();
    double fdEps = 1.0e-4;
    for (boolean isCall : new boolean[] {true, false }) {
      for (double strike : STRIKES) {
        for (double interest : INTERESTS) {
          for (double vol : VOLS) {
            for (double dividend : DIVIDENDS) {
              OptionFunction function = EuropeanVanillaOptionFunction.of(strike, TIME, PutCall.ofPut(!isCall), nSteps);
              double[] params = lattice.getParametersTrinomial(vol, interest - dividend, dt).toArray();
              DoubleArray time = DoubleArray.of(nSteps + 1, i -> dt * i);
              DoubleArray df = DoubleArray.of(nSteps, i -> Math.exp(-interest * dt));
              double[][] stateValue = new double[nSteps + 1][];
              stateValue[0] = new double[] {SPOT };
              List<DoubleMatrix> prob = new ArrayList<DoubleMatrix>();
              double[] probs = new double[] {params[5], params[4], params[3] };
              for (int i = 0; i < nSteps; ++i) {
                int index = i;
                stateValue[i + 1] = DoubleArray.of(2 * i + 3,
                    j -> SPOT * Math.pow(params[2], index + 1 - j) * Math.pow(params[1], j)).toArray();
                double[][] probMatrix = new double[2 * i + 1][];
                Arrays.fill(probMatrix, probs);
                prob.add(DoubleMatrix.ofUnsafe(probMatrix));
              }
              RecombiningTrinomialTreeData treeData =
                  RecombiningTrinomialTreeData.of(DoubleMatrix.ofUnsafe(stateValue), prob, df, time);
              double priceData = TRINOMIAL_TREE.optionPrice(function, treeData);
              double priceParams = TRINOMIAL_TREE.optionPrice(function, lattice, SPOT, vol, interest, dividend);
              assertEquals(priceData, priceParams);
              ValueDerivatives priceDeriv = TRINOMIAL_TREE.optionPriceAdjoint(function, treeData);
              assertEquals(priceDeriv.getValue(), priceData);
              double priceUp = TRINOMIAL_TREE.optionPrice(function, lattice, SPOT + fdEps, vol, interest, dividend);
              double priceDw = TRINOMIAL_TREE.optionPrice(function, lattice, SPOT - fdEps, vol, interest, dividend);
              double fdDelta = 0.5 * (priceUp - priceDw) / fdEps;
              assertEquals(priceDeriv.getDerivative(0), fdDelta, 3.0e-2);
            }
          }
        }
      }
    }
  }

}
