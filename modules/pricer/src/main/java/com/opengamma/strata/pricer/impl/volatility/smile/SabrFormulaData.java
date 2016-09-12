/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile;

import java.io.Serializable;
import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * The data bundle for SABR formula.
 * <p>
 * The bundle contains the SABR model parameters, alpha, beta, rho and nu, as an array.
 */
@BeanDefinition(style = "light")
public final class SabrFormulaData
    implements SmileModelData, ImmutableBean, Serializable {

  /**
   * The number of model parameters.
   */
  private static final int NUM_PARAMETERS = 4;

  /**
   * The model parameters.
   * <p>
   * This must be an array of length 4.
   * The parameters in the array are in the order of alpha, beta, rho and nu.
   * The constraints for the parameters are defined in {@link #isAllowed(int, double)}.
   */
  @PropertyDefinition(validate = "notNull")
  private final DoubleArray parameters;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance of the SABR formula data.
   * 
   * @param alpha  the alpha parameter
   * @param beta  the beta parameter
   * @param rho  the rho parameter
   * @param nu  the nu parameter
   * @return the instance
   */
  public static SabrFormulaData of(double alpha, double beta, double rho, double nu) {
    return new SabrFormulaData(DoubleArray.of(alpha, beta, rho, nu));
  }

  /**
   * Obtains an instance of the SABR formula data.
   * <p>
   * The parameters in the input array should be in the order of alpha, beta, rho and nu.
   * 
   * @param parameters  the parameters
   * @return the instance
   */
  public static SabrFormulaData of(double[] parameters) {
    ArgChecker.notNull(parameters, "parameters");
    ArgChecker.isTrue(parameters.length == NUM_PARAMETERS, "the number of parameters should be 4");
    return new SabrFormulaData(DoubleArray.copyOf(parameters));
  }

  @ImmutableValidator
  private void validate() {
    for (int i = 0; i < NUM_PARAMETERS; ++i) {
      ArgChecker.isTrue(isAllowed(i, parameters.get(i)), "the {}-th parameter is not allowed", i);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the alpha parameter.
   * 
   * @return the alpha parameter
   */
  public double getAlpha() {
    return parameters.get(0);
  }

  /**
   * Gets the beta parameter.
   * 
   * @return the beta parameter
   */
  public double getBeta() {
    return parameters.get(1);
  }

  /**
   * Gets the rho parameter.
   * 
   * @return the rho parameter
   */
  public double getRho() {
    return parameters.get(2);
  }

  /**
   * Obtains the nu parameters.
   * 
   * @return the nu parameter
   */
  public double getNu() {
    return parameters.get(3);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this instance with alpha replaced.
   * 
   * @param alpha  the new alpha
   * @return the new data instance
   */
  public SabrFormulaData withAlpha(double alpha) {
    return of(alpha, getBeta(), getRho(), getNu());
  }

  /**
   * Returns a copy of this instance with beta replaced.
   * 
   * @param beta  the new beta
   * @return the new data instance
   */
  public SabrFormulaData withBeta(double beta) {
    return of(getAlpha(), beta, getRho(), getNu());
  }

  /**
   * Returns a copy of this instance with rho replaced.
   * 
   * @param rho  the new rho
   * @return the new data instance
   */
  public SabrFormulaData withRho(double rho) {
    return of(getAlpha(), getBeta(), rho, getNu());
  }

  /**
   * Returns a copy of this instance with nu replaced.
   * 
   * @param nu  the new nu
   * @return the new data instance
   */
  public SabrFormulaData withNu(double nu) {
    return of(getAlpha(), getBeta(), getRho(), nu);
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
      case 1:
      case 3:
        return value >= 0;
      case 2:
        return value >= -1 && value <= 1;
      default:
        throw new IllegalArgumentException("index " + index + " outside range");
    }
  }

  @Override
  public SabrFormulaData with(int index, double value) {
    ArgChecker.inRange(index, 0, NUM_PARAMETERS, "index");
    double[] paramsCp = parameters.toArray();
    paramsCp[index] = value;
    return of(paramsCp);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SabrFormulaData}.
   */
  private static MetaBean META_BEAN = LightMetaBean.of(SabrFormulaData.class);

  /**
   * The meta-bean for {@code SabrFormulaData}.
   * @return the meta-bean, not null
   */
  public static MetaBean meta() {
    return META_BEAN;
  }

  static {
    JodaBeanUtils.registerMetaBean(META_BEAN);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private SabrFormulaData(
      DoubleArray parameters) {
    JodaBeanUtils.notNull(parameters, "parameters");
    this.parameters = parameters;
    validate();
  }

  @Override
  public MetaBean metaBean() {
    return META_BEAN;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the model parameters.
   * <p>
   * This must be an array of length 4.
   * The parameters in the array are in the order of alpha, beta, rho and nu.
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
      SabrFormulaData other = (SabrFormulaData) obj;
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
    buf.append("SabrFormulaData{");
    buf.append("parameters").append('=').append(JodaBeanUtils.toString(parameters));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
