/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile.function;

import java.io.Serializable;
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
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * The data bundle for SABR formula. 
 * <p>
 * The bundle contains the SABR model parameters, alpha, beta, rho and nu, as an array. 
 */
@BeanDefinition(builderScope = "private")
public final class SabrFormulaData
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
   * @return  the instance
   */
  public static SabrFormulaData of(double[] parameters) {
    ArgChecker.notNull(parameters, "parameters");
    ArgChecker.isTrue(parameters.length == NUM_PARAMETERS, "the number of parameters should be 4");
    return new SabrFormulaData(DoubleArray.copyOf(parameters));
  }

  @ImmutableValidator
  private void validate() {
    for (int i = 0; i < 4; ++i) {
      ArgChecker.isTrue(isAllowed(i, parameters.get(i)), "the {}-th parameter is not allowed", i);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the alpha parameter. 
   * 
   * @return the alpha parameter
   */
  public double getAlpha() {
    return parameters.get(0);
  }

  /**
   * Obtains the beta parameter. 
   * 
   * @return the beta parameter
   */
  public double getBeta() {
    return parameters.get(1);
  }

  /**
   * Obtains the rho parameter. 
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

  /**
   * Obtains a new SABR formula data bundle with alpha replaced. 
   * 
   * @param alpha  the new alpha
   * @return the new bundle
   */
  public SabrFormulaData withAlpha(double alpha) {
    return of(alpha, getBeta(), getRho(), getNu());
  }

  /**
   * Obtains a new SABR formula data bundle with beta replaced. 
   * 
   * @param beta  the new beta
   * @return the new bundle
   */
  public SabrFormulaData withBeta(double beta) {
    return of(getAlpha(), beta, getRho(), getNu());
  }

  /**
   * Obtains a new SABR formula data bundle with rho replaced. 
   * 
   * @param rho  the new rho
   * @return the new bundle
   */
  public SabrFormulaData withRho(double rho) {
    return of(getAlpha(), getBeta(), rho, getNu());
  }

  /**
   * Obtains a new SABR formula data bundle with nu replaced. 
   * 
   * @param nu  the new nu
   * @return the new bundle
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
    ArgChecker.inRangeExclusive(index, -1, NUM_PARAMETERS, "index");
    double[] paramsCp = parameters.toArray();
    paramsCp[index] = value;
    return of(paramsCp);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SabrFormulaData}.
   * @return the meta-bean, not null
   */
  public static SabrFormulaData.Meta meta() {
    return SabrFormulaData.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SabrFormulaData.Meta.INSTANCE);
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
  public SabrFormulaData.Meta metaBean() {
    return SabrFormulaData.Meta.INSTANCE;
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
    buf.append("SabrFormulaData{");
    buf.append("parameters").append('=').append(JodaBeanUtils.toString(getParameters()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SabrFormulaData}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code parameters} property.
     */
    private final MetaProperty<DoubleArray> parameters = DirectMetaProperty.ofImmutable(
        this, "parameters", SabrFormulaData.class, DoubleArray.class);
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
    public BeanBuilder<? extends SabrFormulaData> builder() {
      return new SabrFormulaData.Builder();
    }

    @Override
    public Class<? extends SabrFormulaData> beanType() {
      return SabrFormulaData.class;
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
    public MetaProperty<DoubleArray> parameters() {
      return parameters;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 458736106:  // parameters
          return ((SabrFormulaData) bean).getParameters();
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
   * The bean-builder for {@code SabrFormulaData}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<SabrFormulaData> {

    private DoubleArray parameters;

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
          this.parameters = (DoubleArray) newValue;
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
    public SabrFormulaData build() {
      return new SabrFormulaData(
          parameters);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("SabrFormulaData.Builder{");
      buf.append("parameters").append('=').append(JodaBeanUtils.toString(parameters));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
