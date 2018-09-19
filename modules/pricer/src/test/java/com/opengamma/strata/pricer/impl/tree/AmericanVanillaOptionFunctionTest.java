/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.tree;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.product.common.PutCall;

@Test

public class AmericanVanillaOptionFunctionTest {
  private static final TrinomialTree TRINOMIAL_TREE = new TrinomialTree();
  private static final BinomialTree BINOMIAL_TREE = new BinomialTree();
  private static final double SPOT = 52;
  private static final double STRIKE = 50;
  private static final double TIME = 5./12.;
  private static final double INTEREST = 0.1;
  private static final double VOL = 0.40;
  private static final double DIVIDEND = 2.06;
  private static final double DIVIDEND_TIME = 3.5/12.;
  
  private static int NSTEPS = 5;
  
  public void test_trinomialAndBinomialTree() {
    LatticeSpecification lattice = new CoxRossRubinsteinLatticeSpecification();
    OptionFunction function = AmericanVanillaOptionFunction.of(STRIKE, TIME, PutCall.ofPut(true), NSTEPS);
    ValueDerivatives computedTrinomial = TRINOMIAL_TREE.optionPriceAdjoint(function, lattice, SPOT, VOL, INTEREST, 0.);
    double computedBinomial = BINOMIAL_TREE.optionPrice(function, lattice, SPOT, VOL, INTEREST, new double[]{DIVIDEND}, new double[]{DIVIDEND_TIME});
  }
}

