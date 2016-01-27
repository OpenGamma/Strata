/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * The data bundle for SSVI smile formula. 
 * <p>
 * The bundle contains the SSVI model parameters, ATM volatility, rho and eta. 
 */
@BeanDefinition(builderScope = "private")
public final class SsviFormulaData
    implements SmileModelData, ImmutableBean, Serializable {
  /**
   * The number of model parameters. 
   */
  private static final int NUM_PARAMETERS = 3;
  /**
   * The model parameters. 
   * <p>
   * This should be initialized as an array with length 3.
   * The parameters in the array are in the order of ATM volatility, rho and eta.
   * The constraints for the parameters are defined in {@link #isAllowed(int, double)}.
   */
  @PropertyDefinition(validate = "notNull")
  private final DoubleArray parameters;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance of the SABR formula data. 
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
   * Obtains an instance of the SABR formula data. 
   * <p>
   * The parameters in the input array should be in the order of alpha, beta, rho and nu.  
   * 
   * @param parameters  the parameters
   * @return  the instance
   */
  public static SsviFormulaData of(double[] parameters) {
    ArgChecker.notNull(parameters, "parameters");
    ArgChecker.isTrue(parameters.length == NUM_PARAMETERS, "the number of parameters should be 3");
    return new SsviFormulaData(DoubleArray.copyOf(parameters));
  }

  @ImmutableValidator
  private void validate() {
    for (int i = 0; i < 3; ++i) {
      ArgChecker.isTrue(isAllowed(i, parameters.get(i)), "the {}-th parameter is not allowed", i);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the sigma parameter. 
   * 
   * @return the sigma parameter
   */
  public double getSigma() {
    return parameters.get(0);
  }

  /**
   * Obtains the rho parameter. 
   * 
   * @return the rho parameter
   */
  public double getRho() {
    return parameters.get(1);
  }

  /**
   * Obtains the eta parameters.
   * 
   * @return the eta parameter
   */
  public double getEta() {
    return parameters.get(2);
  }

  /**
   * Obtains a new SSVI formula data bundle with sigma replaced. 
   * 
   * @param sigma  the new sigma
   * @return the new bundle
   */
  public SsviFormulaData withSigma(double sigma) {
    return of(sigma, getRho(), getEta());
  }

  /**
   * Obtains a new SSVI formula data bundle with rho replaced. 
   * 
   * @param rho  the new rho
   * @return the new bundle
   */
  public SsviFormulaData withRho(double rho) {
    return of(getSigma(), rho, getEta());
  }

  /**
   * Obtains a new SSVI formula data bundle with eta replaced. 
   * 
   * @param eta  the new eta
   * @return the new bundle
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
    ArgChecker.inRangeExclusive(index, -1, NUM_PARAMETERS, "index");
    double[] paramsCp = parameters.toArray();
    paramsCp[index] = value;
    return of(paramsCp);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SsviFormulaData}.
   * @return the meta-bean, not null
   */
  public static SsviFormulaData.Meta meta() {
    return SsviFormulaData.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SsviFormulaData.Meta.INSTANCE);
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
  public SsviFormulaData.Meta metaBean() {
    return SsviFormulaData.Meta.INSTANCE;
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
   * This should be initialized as an array with length 3.
   * The parameters in the array are in the order of ATM volatility, rho and eta.
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

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SsviFormulaData}.
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
        this, "parameters", SsviFormulaData.class, DoubleArray.class);
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
    public BeanBuilder<? extends SsviFormulaData> builder() {
      return new SsviFormulaData.Builder();
    }

    @Override
    public Class<? extends SsviFormulaData> beanType() {
      return SsviFormulaData.class;
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
          return ((SsviFormulaData) bean).getParameters();
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
   * The bean-builder for {@code SsviFormulaData}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<SsviFormulaData> {

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
    public SsviFormulaData build() {
      return new SsviFormulaData(
          parameters);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("SsviFormulaData.Builder{");
      buf.append("parameters").append('=').append(JodaBeanUtils.toString(parameters));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
