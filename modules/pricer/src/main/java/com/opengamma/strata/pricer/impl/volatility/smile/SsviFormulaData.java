/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;

import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.TypedMetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * The data bundle for SSVI smile formula.
 * <p>
 * The bundle contains the SSVI model parameters, ATM volatility, rho and eta.
 */
@BeanDefinition(style = "light")
public final class SsviFormulaData
    implements SmileModelData, ImmutableBean, Serializable {

  /**
   * The number of model parameters.
   */
  private static final int NUM_PARAMETERS = 3;

  /**
   * The model parameters.
   * <p>
   * This must be an array of length 3.
   * The parameters in the array are in the order of sigma (ATM volatility), rho and eta.
   * The constraints for the parameters are defined in {@link #isAllowed(int, double)}.
   */
  @PropertyDefinition(validate = "notNull")
  private final DoubleArray parameters;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance of the SSVI formula data.
   * 
   * @param sigma  the sigma parameter, ATM volatility
   * @param rho  the rho parameter
   * @param eta  the eta parameter
   * @return the instance
   */
  public static SsviFormulaData of(double sigma, double rho, double eta) {
    return new SsviFormulaData(DoubleArray.of(sigma, rho, eta));
  }

  /**
   * Obtains an instance of the SSVI formula data.
   * <p>
   * The parameters in the input array should be in the order of sigma (ATM volatility), rho and eta.
   * 
   * @param parameters  the parameters
   * @return the instance
   */
  public static SsviFormulaData of(double[] parameters) {
    ArgChecker.notNull(parameters, "parameters");
    ArgChecker.isTrue(parameters.length == NUM_PARAMETERS, "the number of parameters should be 3");
    return new SsviFormulaData(DoubleArray.copyOf(parameters));
  }

  @ImmutableValidator
  private void validate() {
    for (int i = 0; i < NUM_PARAMETERS; ++i) {
      ArgChecker.isTrue(isAllowed(i, parameters.get(i)), "the {}-th parameter is not allowed", i);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the sigma parameter.
   * 
   * @return the sigma parameter
   */
  public double getSigma() {
    return parameters.get(0);
  }

  /**
   * Gets the rho parameter.
   * 
   * @return the rho parameter
   */
  public double getRho() {
    return parameters.get(1);
  }

  /**
   * Gets the eta parameters.
   * 
   * @return the eta parameter
   */
  public double getEta() {
    return parameters.get(2);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this instance with sigma replaced.
   * 
   * @param sigma  the new sigma
   * @return the new data instance
   */
  public SsviFormulaData withSigma(double sigma) {
    return of(sigma, getRho(), getEta());
  }

  /**
   * Returns a copy of this instance with rho replaced.
   * 
   * @param rho  the new rho
   * @return the new data instance
   */
  public SsviFormulaData withRho(double rho) {
    return of(getSigma(), rho, getEta());
  }

  /**
   * Returns a copy of this instance with eta replaced.
   * 
   * @param eta  the new eta
   * @return the new data instance
   */
  public SsviFormulaData withEta(double eta) {
    return of(getSigma(), getRho(), eta);
  }

  //-------------------------------------------------------------------------
  @Override
  public int getNumberOfParameters() {
    return NUM_PARAMETERS;
  }

  @Override
  public double getParameter(int index) {
    ArgChecker.inRangeExclusive(index, -1, NUM_PARAMETERS, "index");
    return parameters.get(index);
  }

  @Override
  public boolean isAllowed(int index, double value) {
    switch (index) {
      case 0:
        return value > 0;
      case 1:
        return value >= -1 && value <= 1;
      case 2:
        return value > 0;
      default:
        throw new IllegalArgumentException("index " + index + " outside range");
    }
  }

  @Override
  public SsviFormulaData with(int index, double value) {
    ArgChecker.inRange(index, 0, NUM_PARAMETERS, "index");
    double[] paramsCp = parameters.toArray();
    paramsCp[index] = value;
    return of(paramsCp);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code SsviFormulaData}.
   */
  private static final TypedMetaBean<SsviFormulaData> META_BEAN =
      LightMetaBean.of(SsviFormulaData.class, MethodHandles.lookup());

  /**
   * The meta-bean for {@code SsviFormulaData}.
   * @return the meta-bean, not null
   */
  public static TypedMetaBean<SsviFormulaData> meta() {
    return META_BEAN;
  }

  static {
    MetaBean.register(META_BEAN);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private SsviFormulaData(
      DoubleArray parameters) {
    JodaBeanUtils.notNull(parameters, "parameters");
    this.parameters = parameters;
    validate();
  }

  @Override
  public TypedMetaBean<SsviFormulaData> metaBean() {
    return META_BEAN;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the model parameters.
   * <p>
   * This must be an array of length 3.
   * The parameters in the array are in the order of sigma (ATM volatility), rho and eta.
   * The constraints for the parameters are defined in {@link #isAllowed(int, double)}.
   * @return the value of the property, not null
   */
  public DoubleArray getParameters() {
    return parameters;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SsviFormulaData other = (SsviFormulaData) obj;
      return JodaBeanUtils.equal(parameters, other.parameters);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(parameters);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("SsviFormulaData{");
    buf.append("parameters").append('=').append(JodaBeanUtils.toString(parameters));
    buf.append('}');
    return buf.toString();
  }

  //-------------------------- AUTOGENERATED END --------------------------
}
