/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.margin;

import com.opengamma.strata.pricer.impl.tree.BinomialTree;
import com.opengamma.strata.pricer.impl.tree.CoxRossRubinsteinLatticeSpecification;
import com.opengamma.strata.pricer.impl.tree.LatticeSpecification;
import com.opengamma.strata.pricer.impl.tree.TrinomialTree;
import com.opengamma.strata.product.common.PutCall;

public abstract class AmericanOption implements Option{
  
  protected static LatticeSpecification LATTICE = new CoxRossRubinsteinLatticeSpecification();
  protected static final TrinomialTree TRINOMIAL_TREE = new TrinomialTree();
  protected static final BinomialTree BINOMIAL_TREE = new BinomialTree();
  protected static final int STEPS = 50;
  
  private double quantity;
  private double strike;
  private double expiry;
  private PutCall putCall;
  private double notional;
  
  AmericanOption(){}
  
  AmericanOption(
      double quantity,
      double notional,
      double strike,
      double expiry,
      PutCall putCall){
    this.quantity = quantity;
    this.strike = strike;
    this.expiry = expiry;
    this.putCall = putCall;
    this.notional = notional;
  }
  
  public double strike(){ 
    return strike;
  }
  
  public double expiry(){
    return expiry;
  }
  
  public double notional(){
    return notional;
  }
  
  public double quantity(){
    return quantity;
  }
}
