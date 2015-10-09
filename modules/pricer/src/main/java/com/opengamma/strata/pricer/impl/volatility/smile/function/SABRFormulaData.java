/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile.function;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.collect.ArgChecker;

/**
 * The data bundle for SABR formula. 
 * <p>
 * The bundle contains the SABR model parameters, alpha, beta, rho and nu, as an array. 
 */
@BeanDefinition(builderScope = "private")
public final class SABRFormulaData
    implements SmileModelData, ImmutableBean, Serializable {
  /**
   * The number of model parameters. 
   */
  private static final int NUM_PARAMETERS = 4;
  /**
   * The model parameters. 
   * <p>
   * This should be initialized as an array with length 4.
   * The parameters in the array are in the order of alpha, beta, rho and nu.
   * The constraints for the parameters are defined in {@link #isAllowed(int, double)}.
   */
  @PropertyDefinition(validate = "notNull")
  private final double[] parameters;

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
  public static SABRFormulaData of(double alpha, double beta, double rho, double nu) {
    return new SABRFormulaData(new double[] {alpha, beta, rho, nu });
  }

  /**
   * Obtains an instance of the SABR formula data. 
   * <p>
   * The parameters in the input array should be in the order of alpha, beta, rho and nu.  
   * 
   * @param parameters  the parameters
   * @return  the instance
   */
  public static SABRFormulaData of(double[] parameters) {
    ArgChecker.notNull(parameters, "parameters");
    ArgChecker.isTrue(parameters.length == 4, "the number of parameters should be 4");
    return new SABRFormulaData(Arrays.copyOf(parameters, NUM_PARAMETERS));
  }

  @ImmutableValidator
  private void validate() {
    for (int i = 0; i < 4; ++i) {
      ArgChecker.isTrue(isAllowed(i, parameters[i]), "the {}-th parameter is not allowed", i);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the alpha parameter. 
   * 
   * @return the alpha parameter
   */
  public double getAlpha() {
    return parameters[0];
  }

  /**
   * Obtains the beta parameter. 
   * 
   * @return the beta parameter
   */
  public double getBeta() {
    return parameters[1];
  }

  /**
   * Obtains the rho parameter. 
   * 
   * @return the rho parameter
   */
  public double getRho() {
    return parameters[2];
  }

  /**
   * Obtains the nu parameters.
   * 
   * @return the nu parameter
   */
  public double getNu() {
    return parameters[3];
  }

  /**
   * Obtains a new SABR formula data bundle with alpha replaced. 
   * 
   * @param alpha  the new alpha
   * @return the new bundle
   */
  public SABRFormulaData withAlpha(double alpha) {
    return of(alpha, getBeta(), getRho(), getNu());
  }

  /**
   * Obtains a new SABR formula data bundle with beta replaced. 
   * 
   * @param beta  the new beta
   * @return the new bundle
   */
  public SABRFormulaData withBeta(final double beta) {
    return of(getAlpha(), beta, getRho(), getNu());
  }

  /**
   * Obtains a new SABR formula data bundle with rho replaced. 
   * 
   * @param rho  the new rho
   * @return the new bundle
   */
  public SABRFormulaData withRho(final double rho) {
    return of(getAlpha(), getBeta(), rho, getNu());
  }

  /**
   * Obtains a new SABR formula data bundle with nu replaced. 
   * 
   * @param nu  the new nu
   * @return the new bundle
   */
  public SABRFormulaData withNu(final double nu) {
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
    return parameters[index];
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
  public SABRFormulaData with(int index, double value) {
    ArgChecker.inRangeExclusive(index, -1, NUM_PARAMETERS, "index");
    double[] paramsCp = Arrays.copyOf(parameters, NUM_PARAMETERS);
    paramsCp[index] = value;
    return of(paramsCp);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SABRFormulaData}.
   * @return the meta-bean, not null
   */
  public static SABRFormulaData.Meta meta() {
    return SABRFormulaData.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SABRFormulaData.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private SABRFormulaData(
      double[] parameters) {
    JodaBeanUtils.notNull(parameters, "parameters");
    this.parameters = parameters.clone();
    validate();
  }

  @Override
  public SABRFormulaData.Meta metaBean() {
    return SABRFormulaData.Meta.INSTANCE;
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
   * This should be initialized as an array with length 4.
   * The parameters in the array are in the order of alpha, beta, rho and nu.
   * The constraints for the parameters are defined in {@link #isAllowed(int, double)}.
   * @return the value of the property, not null
   */
  public double[] getParameters() {
    return (parameters != null ? parameters.clone() : null);
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SABRFormulaData other = (SABRFormulaData) obj;
      return JodaBeanUtils.equal(getParameters(), other.getParameters());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getParameters());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("SABRFormulaData{");
    buf.append("parameters").append('=').append(JodaBeanUtils.toString(getParameters()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SABRFormulaData}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code parameters} property.
     */
    private final MetaProperty<double[]> parameters = DirectMetaProperty.ofImmutable(
        this, "parameters", SABRFormulaData.class, double[].class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "parameters");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 458736106:  // parameters
          return parameters;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends SABRFormulaData> builder() {
      return new SABRFormulaData.Builder();
    }

    @Override
    public Class<? extends SABRFormulaData> beanType() {
      return SABRFormulaData.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code parameters} property.
     * @return the meta-property, not null
     */
    public MetaProperty<double[]> parameters() {
      return parameters;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 458736106:  // parameters
          return ((SABRFormulaData) bean).getParameters();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code SABRFormulaData}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<SABRFormulaData> {

    private double[] parameters;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 458736106:  // parameters
          return parameters;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 458736106:  // parameters
          this.parameters = (double[]) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public SABRFormulaData build() {
      return new SABRFormulaData(
          parameters);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("SABRFormulaData.Builder{");
      buf.append("parameters").append('=').append(JodaBeanUtils.toString(parameters));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
