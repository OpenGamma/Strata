/**
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.UnitParameterSensitivity;

/**
 * A curve based on a parameterized function.
 * <p>
 * This class defines a curve in terms of a function and its parameters.
 */
@BeanDefinition
public final class ParameterizedFunctionalCurve
    implements Curve, ImmutableBean {

  /**
   * The curve metadata.
   * <p>
   * The metadata includes an optional list of parameter metadata.
   * If present, the size of the parameter metadata list will match the number of parameters of this curve.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CurveMetadata metadata;
  /**
   * The array of parameters for the curve function.
   */
  @PropertyDefinition(validate = "notNull")
  private final DoubleArray parameters;
  /**
   * The y-value function.
   * <p>
   * The function takes {@code parameters} and x-value, then returns y-value.
   */
  @PropertyDefinition(validate = "notNull")
  private final BiFunction<DoubleArray, Double, Double> valueFunction;
  /**
   * The derivative function.
   * <p>
   * The function takes {@code parameters} and x-value, then returns the first derivative of y-value with respective to x, 
   * i.e., the gradient of the curve.
   */
  @PropertyDefinition(validate = "notNull")
  private final BiFunction<DoubleArray, Double, Double> derivativeFunction;
  /**
   * The parameter sensitivity function.
   * <p>
   * The function takes {@code parameters} and x-value, then returns the sensitivities of y-value to the parameters.
   */
  @PropertyDefinition(validate = "notNull")
  private final BiFunction<DoubleArray, Double, DoubleArray> sensitivityFunction;
  /**
   * The parameter metadata.
   */
  private final transient List<ParameterMetadata> parameterMetadata;  // derived, not a property

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance.
   * 
   * @param metadata  the metadata
   * @param parameters  the parameters
   * @param valueFunction  the value function
   * @param derivativeFunction  the derivative function
   * @param sensitivityFunction  the parameter sensitivity function
   * @return the instance
   */
  public static ParameterizedFunctionalCurve of(
      CurveMetadata metadata,
      DoubleArray parameters,
      BiFunction<DoubleArray, Double, Double> valueFunction,
      BiFunction<DoubleArray, Double, Double> derivativeFunction,
      BiFunction<DoubleArray, Double, DoubleArray> sensitivityFunction) {

    return ParameterizedFunctionalCurve.builder()
        .metadata(metadata)
        .parameters(parameters)
        .valueFunction(valueFunction)
        .derivativeFunction(derivativeFunction)
        .sensitivityFunction(sensitivityFunction)
        .build();
  }

  // restricted constructor
  @ImmutableConstructor
  private ParameterizedFunctionalCurve(
      CurveMetadata metadata,
      DoubleArray parameters,
      BiFunction<DoubleArray, Double, Double> valueFunction,
      BiFunction<DoubleArray, Double, Double> derivativeFunction,
      BiFunction<DoubleArray, Double, DoubleArray> sensitivityFunction) {

    JodaBeanUtils.notNull(metadata, "metadata");
    JodaBeanUtils.notNull(parameters, "parameters");
    JodaBeanUtils.notNull(valueFunction, "valueFunction");
    JodaBeanUtils.notNull(derivativeFunction, "derivativeFunction");
    JodaBeanUtils.notNull(sensitivityFunction, "sensitivityFunction");
    this.metadata = metadata;
    this.parameters = parameters;
    this.valueFunction = valueFunction;
    this.derivativeFunction = derivativeFunction;
    this.sensitivityFunction = sensitivityFunction;
    this.parameterMetadata = IntStream.range(0, getParameterCount())
        .mapToObj(i -> getParameterMetadata(i))
        .collect(toImmutableList());
  }

  //-------------------------------------------------------------------------
  @Override
  public int getParameterCount() {
    return parameters.size();
  }

  @Override
  public double getParameter(int parameterIndex) {
    return parameters.get(parameterIndex);
  }

  @Override
  public ParameterizedFunctionalCurve withParameter(int parameterIndex, double newValue) {
    return withParameters(parameters.with(parameterIndex, newValue));
  }

  @Override
  public ParameterizedFunctionalCurve withPerturbation(ParameterPerturbation perturbation) {
    int size = parameters.size();
    DoubleArray perturbedValues = DoubleArray.of(
        size, i -> perturbation.perturbParameter(i, parameters.get(i), getParameterMetadata(i)));
    return withParameters(perturbedValues);
  }

  //-------------------------------------------------------------------------
  @Override
  public double yValue(double x) {
    return valueFunction.apply(parameters, x);
  }

  @Override
  public UnitParameterSensitivity yValueParameterSensitivity(double x) {
    return createParameterSensitivity(sensitivityFunction.apply(parameters, x));
  }

  @Override
  public double firstDerivative(double x) {
    return derivativeFunction.apply(parameters, x);
  }

  //-------------------------------------------------------------------------
  @Override
  public ParameterizedFunctionalCurve withMetadata(CurveMetadata metadata) {
    return new ParameterizedFunctionalCurve(metadata, parameters, valueFunction, derivativeFunction, sensitivityFunction);
  }

  /**
   * Returns a copy of the curve with all of the parameters altered. 
   * <p>
   * This instance is immutable and unaffected by this method call.
   * 
   * @param parameters  the new parameters 
   * @return the curve with the parameters altered
   */
  public ParameterizedFunctionalCurve withParameters(DoubleArray parameters) {
    ArgChecker.isTrue(parameters.size() == this.parameters.size(),
        "the new parameters size must be the same as the initial parameter size");
    return new ParameterizedFunctionalCurve(metadata, parameters, valueFunction, derivativeFunction, sensitivityFunction);
  }

  //-------------------------------------------------------------------------
  @Override
  public UnitParameterSensitivity createParameterSensitivity(DoubleArray sensitivities) {
    return UnitParameterSensitivity.of(getName(), parameterMetadata, sensitivities);
  }

  @Override
  public CurrencyParameterSensitivity createParameterSensitivity(Currency currency, DoubleArray sensitivities) {
    return CurrencyParameterSensitivity.of(getName(), parameterMetadata, currency, sensitivities);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ParameterizedFunctionalCurve}.
   * @return the meta-bean, not null
   */
  public static ParameterizedFunctionalCurve.Meta meta() {
    return ParameterizedFunctionalCurve.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ParameterizedFunctionalCurve.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ParameterizedFunctionalCurve.Builder builder() {
    return new ParameterizedFunctionalCurve.Builder();
  }

  @Override
  public ParameterizedFunctionalCurve.Meta metaBean() {
    return ParameterizedFunctionalCurve.Meta.INSTANCE;
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
   * Gets the curve metadata.
   * <p>
   * The metadata includes an optional list of parameter metadata.
   * If present, the size of the parameter metadata list will match the number of parameters of this curve.
   * @return the value of the property, not null
   */
  @Override
  public CurveMetadata getMetadata() {
    return metadata;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the array of parameters for the curve function.
   * @return the value of the property, not null
   */
  public DoubleArray getParameters() {
    return parameters;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the y-value function.
   * <p>
   * The function takes {@code parameters} and x-value, then returns y-value.
   * @return the value of the property, not null
   */
  public BiFunction<DoubleArray, Double, Double> getValueFunction() {
    return valueFunction;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the derivative function.
   * <p>
   * The function takes {@code parameters} and x-value, then returns the first derivative of y-value with respective to x,
   * i.e., the gradient of the curve.
   * @return the value of the property, not null
   */
  public BiFunction<DoubleArray, Double, Double> getDerivativeFunction() {
    return derivativeFunction;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the parameter sensitivity function.
   * <p>
   * The function takes {@code parameters} and x-value, then returns the sensitivities of y-value to the parameters.
   * @return the value of the property, not null
   */
  public BiFunction<DoubleArray, Double, DoubleArray> getSensitivityFunction() {
    return sensitivityFunction;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ParameterizedFunctionalCurve other = (ParameterizedFunctionalCurve) obj;
      return JodaBeanUtils.equal(metadata, other.metadata) &&
          JodaBeanUtils.equal(parameters, other.parameters) &&
          JodaBeanUtils.equal(valueFunction, other.valueFunction) &&
          JodaBeanUtils.equal(derivativeFunction, other.derivativeFunction) &&
          JodaBeanUtils.equal(sensitivityFunction, other.sensitivityFunction);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(metadata);
    hash = hash * 31 + JodaBeanUtils.hashCode(parameters);
    hash = hash * 31 + JodaBeanUtils.hashCode(valueFunction);
    hash = hash * 31 + JodaBeanUtils.hashCode(derivativeFunction);
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivityFunction);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("ParameterizedFunctionalCurve{");
    buf.append("metadata").append('=').append(metadata).append(',').append(' ');
    buf.append("parameters").append('=').append(parameters).append(',').append(' ');
    buf.append("valueFunction").append('=').append(valueFunction).append(',').append(' ');
    buf.append("derivativeFunction").append('=').append(derivativeFunction).append(',').append(' ');
    buf.append("sensitivityFunction").append('=').append(JodaBeanUtils.toString(sensitivityFunction));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ParameterizedFunctionalCurve}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code metadata} property.
     */
    private final MetaProperty<CurveMetadata> metadata = DirectMetaProperty.ofImmutable(
        this, "metadata", ParameterizedFunctionalCurve.class, CurveMetadata.class);
    /**
     * The meta-property for the {@code parameters} property.
     */
    private final MetaProperty<DoubleArray> parameters = DirectMetaProperty.ofImmutable(
        this, "parameters", ParameterizedFunctionalCurve.class, DoubleArray.class);
    /**
     * The meta-property for the {@code valueFunction} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<BiFunction<DoubleArray, Double, Double>> valueFunction = DirectMetaProperty.ofImmutable(
        this, "valueFunction", ParameterizedFunctionalCurve.class, (Class) BiFunction.class);
    /**
     * The meta-property for the {@code derivativeFunction} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<BiFunction<DoubleArray, Double, Double>> derivativeFunction = DirectMetaProperty.ofImmutable(
        this, "derivativeFunction", ParameterizedFunctionalCurve.class, (Class) BiFunction.class);
    /**
     * The meta-property for the {@code sensitivityFunction} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<BiFunction<DoubleArray, Double, DoubleArray>> sensitivityFunction = DirectMetaProperty.ofImmutable(
        this, "sensitivityFunction", ParameterizedFunctionalCurve.class, (Class) BiFunction.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "metadata",
        "parameters",
        "valueFunction",
        "derivativeFunction",
        "sensitivityFunction");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -450004177:  // metadata
          return metadata;
        case 458736106:  // parameters
          return parameters;
        case 636119145:  // valueFunction
          return valueFunction;
        case 1663351423:  // derivativeFunction
          return derivativeFunction;
        case -1353652329:  // sensitivityFunction
          return sensitivityFunction;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ParameterizedFunctionalCurve.Builder builder() {
      return new ParameterizedFunctionalCurve.Builder();
    }

    @Override
    public Class<? extends ParameterizedFunctionalCurve> beanType() {
      return ParameterizedFunctionalCurve.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code metadata} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveMetadata> metadata() {
      return metadata;
    }

    /**
     * The meta-property for the {@code parameters} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleArray> parameters() {
      return parameters;
    }

    /**
     * The meta-property for the {@code valueFunction} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BiFunction<DoubleArray, Double, Double>> valueFunction() {
      return valueFunction;
    }

    /**
     * The meta-property for the {@code derivativeFunction} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BiFunction<DoubleArray, Double, Double>> derivativeFunction() {
      return derivativeFunction;
    }

    /**
     * The meta-property for the {@code sensitivityFunction} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BiFunction<DoubleArray, Double, DoubleArray>> sensitivityFunction() {
      return sensitivityFunction;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -450004177:  // metadata
          return ((ParameterizedFunctionalCurve) bean).getMetadata();
        case 458736106:  // parameters
          return ((ParameterizedFunctionalCurve) bean).getParameters();
        case 636119145:  // valueFunction
          return ((ParameterizedFunctionalCurve) bean).getValueFunction();
        case 1663351423:  // derivativeFunction
          return ((ParameterizedFunctionalCurve) bean).getDerivativeFunction();
        case -1353652329:  // sensitivityFunction
          return ((ParameterizedFunctionalCurve) bean).getSensitivityFunction();
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
   * The bean-builder for {@code ParameterizedFunctionalCurve}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ParameterizedFunctionalCurve> {

    private CurveMetadata metadata;
    private DoubleArray parameters;
    private BiFunction<DoubleArray, Double, Double> valueFunction;
    private BiFunction<DoubleArray, Double, Double> derivativeFunction;
    private BiFunction<DoubleArray, Double, DoubleArray> sensitivityFunction;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ParameterizedFunctionalCurve beanToCopy) {
      this.metadata = beanToCopy.getMetadata();
      this.parameters = beanToCopy.getParameters();
      this.valueFunction = beanToCopy.getValueFunction();
      this.derivativeFunction = beanToCopy.getDerivativeFunction();
      this.sensitivityFunction = beanToCopy.getSensitivityFunction();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -450004177:  // metadata
          return metadata;
        case 458736106:  // parameters
          return parameters;
        case 636119145:  // valueFunction
          return valueFunction;
        case 1663351423:  // derivativeFunction
          return derivativeFunction;
        case -1353652329:  // sensitivityFunction
          return sensitivityFunction;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -450004177:  // metadata
          this.metadata = (CurveMetadata) newValue;
          break;
        case 458736106:  // parameters
          this.parameters = (DoubleArray) newValue;
          break;
        case 636119145:  // valueFunction
          this.valueFunction = (BiFunction<DoubleArray, Double, Double>) newValue;
          break;
        case 1663351423:  // derivativeFunction
          this.derivativeFunction = (BiFunction<DoubleArray, Double, Double>) newValue;
          break;
        case -1353652329:  // sensitivityFunction
          this.sensitivityFunction = (BiFunction<DoubleArray, Double, DoubleArray>) newValue;
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
    public ParameterizedFunctionalCurve build() {
      return new ParameterizedFunctionalCurve(
          metadata,
          parameters,
          valueFunction,
          derivativeFunction,
          sensitivityFunction);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the curve metadata.
     * <p>
     * The metadata includes an optional list of parameter metadata.
     * If present, the size of the parameter metadata list will match the number of parameters of this curve.
     * @param metadata  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder metadata(CurveMetadata metadata) {
      JodaBeanUtils.notNull(metadata, "metadata");
      this.metadata = metadata;
      return this;
    }

    /**
     * Sets the array of parameters for the curve function.
     * @param parameters  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder parameters(DoubleArray parameters) {
      JodaBeanUtils.notNull(parameters, "parameters");
      this.parameters = parameters;
      return this;
    }

    /**
     * Sets the y-value function.
     * <p>
     * The function takes {@code parameters} and x-value, then returns y-value.
     * @param valueFunction  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder valueFunction(BiFunction<DoubleArray, Double, Double> valueFunction) {
      JodaBeanUtils.notNull(valueFunction, "valueFunction");
      this.valueFunction = valueFunction;
      return this;
    }

    /**
     * Sets the derivative function.
     * <p>
     * The function takes {@code parameters} and x-value, then returns the first derivative of y-value with respective to x,
     * i.e., the gradient of the curve.
     * @param derivativeFunction  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder derivativeFunction(BiFunction<DoubleArray, Double, Double> derivativeFunction) {
      JodaBeanUtils.notNull(derivativeFunction, "derivativeFunction");
      this.derivativeFunction = derivativeFunction;
      return this;
    }

    /**
     * Sets the parameter sensitivity function.
     * <p>
     * The function takes {@code parameters} and x-value, then returns the sensitivities of y-value to the parameters.
     * @param sensitivityFunction  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder sensitivityFunction(BiFunction<DoubleArray, Double, DoubleArray> sensitivityFunction) {
      JodaBeanUtils.notNull(sensitivityFunction, "sensitivityFunction");
      this.sensitivityFunction = sensitivityFunction;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("ParameterizedFunctionalCurve.Builder{");
      buf.append("metadata").append('=').append(JodaBeanUtils.toString(metadata)).append(',').append(' ');
      buf.append("parameters").append('=').append(JodaBeanUtils.toString(parameters)).append(',').append(' ');
      buf.append("valueFunction").append('=').append(JodaBeanUtils.toString(valueFunction)).append(',').append(' ');
      buf.append("derivativeFunction").append('=').append(JodaBeanUtils.toString(derivativeFunction)).append(',').append(' ');
      buf.append("sensitivityFunction").append('=').append(JodaBeanUtils.toString(sensitivityFunction));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
