/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.runner.function.CalculationSingleFunction;
import com.opengamma.strata.collect.Messages;

/**
 * Configuration of a function that performs a calculation.
 * <p>
 * The configuration includes the function type and may include constructor arguments used when creating
 * function instances. Any constructor arguments not included in the configuration can be provided as
 * an argument to {@link #createFunction(Map)}.
 * <p>
 * Constructor arguments passed to {@code createFunction} are not permitted to override
 * arguments in the configuration. An exception will be thrown if any argument passed to {@code createFunction}
 * has the same name as an argument in the configuration.
 * 
 * @param <T>  the type of the calculation target
 */
@BeanDefinition(builderScope = "private", constructorScope = "package")
public final class FunctionConfig<T extends CalculationTarget> implements ImmutableBean, Serializable {

  private static final Logger log = LoggerFactory.getLogger(FunctionConfig.class);

  /** Configuration used when there is none defined for a calculation. Creates {@link MissingConfigCalculationFunction}. */
  private static final FunctionConfig<? extends CalculationTarget> MISSING =
      FunctionConfig.of(MissingConfigCalculationFunction.class);

// TODO FunctionMetadata instead of function type - includes type and set of calculated measures
  /** The type of the function. */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final Class<? extends CalculationSingleFunction<T, ?>> functionType;

  /** Constructor arguments used for building function instances, keyed by parameter name. */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final Map<String, Object> arguments;

  /**
   * Returns configuration for a function that doesn't contain any constructor arguments.
   * <p>
   * The function must have one public constructor. If the constructor requires arguments, they
   * must be passed to {@link #createFunction}.
   * <p>
   * To create configuration that includes constructor arguments, use a {@linkplain #builder(Class) builder}.
   *
   * @param functionType  the type of the function
   * @return configuration for a function that doesn't contain any constructor arguments
   */
  public static <T extends CalculationTarget> FunctionConfig<T> of(
      Class<? extends CalculationSingleFunction<T, ?>> functionType) {

    return new FunctionConfig<>(functionType, ImmutableMap.of());
  }

  /**
   * Returns a mutable builder for building {@code FunctionConfig}.
   *
   * @param functionType  the type of the function
   * @param <T>  the type of the calculation target
   * @return a mutable builder for building {@code FunctionConfig}
   */
  public static <T extends CalculationTarget> FunctionConfigBuilder<T> builder(
      Class<? extends CalculationSingleFunction<T, ?>> functionType) {

    return new FunctionConfigBuilder<>(functionType);
  }

  /**
   * Returns configuration for a function that is used when no function is configured to calculate a value.
   * <p>
   * The function always returns a failure result.
   *
   * @param <T>  the type of the calculation target
   * @return configuration for a function that always returns a failure
   */
  @SuppressWarnings("unchecked")
  public static <T extends CalculationTarget> FunctionConfig<T> missing() {
    return (FunctionConfig<T>) MISSING;
  }

  // TODO Method returning parameter metadata for the required constructor arguments?

  /**
   * Returns a function instance created using the specified constructor arguments.
   * <p>
   * Throws an exception if the function requires constructor arguments that have not been provided
   * or if any of the supplied arguments have the same name as the arguments in the configuration.
   *
   * @param arguments  constructor arguments for the function instance
   * @return a function instance created using the specified constructor arguments
   * @throws IllegalArgumentException if the function requires constructor arguments that have not been provided
   *   or if any of the supplied arguments have the same name as the arguments in the configuration
   */
  @SuppressWarnings("unchecked")
  public CalculationSingleFunction<T, ?> createFunction(Map<String, Object> arguments) {
    Map<String, Object> mergedArguments = mergedArguments(arguments);
    Constructor<?> constructor = constructor(functionType);
    Object[] argumentArray = constructorArguments(constructor, mergedArguments);

    try {
      return (CalculationSingleFunction<T, ?>) constructor.newInstance(argumentArray);
    } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
      log.warn("Failed to create engine function", e);
      return (CalculationSingleFunction<T, ?>) new MissingConfigCalculationFunction();
    }
  }

  /**
   * Returns a function instance created using the constructor arguments from the configuration.
   * <p>
   * Throws an exception if the function requires constructor arguments that are not available in the configuration.
   *
   * @return a function instance
   * @throws IllegalArgumentException if the function requires constructor arguments that have not been provided
   */
  @SuppressWarnings("unchecked")
  public CalculationSingleFunction<T, ?> createFunction() {
    return createFunction(ImmutableMap.of());
  }

  private Map<String, Object> mergedArguments(Map<String, Object> arguments) {
    Set<String> intersection = Sets.intersection(this.arguments.keySet(), arguments.keySet());

    if (!intersection.isEmpty()) {
      throw new IllegalArgumentException(
          Messages.format(
              "Built-in function arguments cannot be overridden: {}",
              intersection));
    }
    return ImmutableMap.<String, Object>builder()
        .putAll(this.arguments)
        .putAll(arguments)
        .build();
  }

  /**
   * Returns a constructor for creating a function instance.
   * <p>
   * The function type must have one public constructor.
   *
   * @param functionType  a function type
   * @return a constructor for creating a function instance
   * @throws IllegalArgumentException if the function doesn't have exactly one public constructor
   */
  private static Constructor<?> constructor(Class<?> functionType) {
    Constructor<?>[] constructors = functionType.getConstructors();

    if (constructors.length == 1) {
      return constructors[0];
    } else {
      throw new IllegalArgumentException(
          Messages.format(
              "Functions must have one public constructor, {} has {}",
              functionType,
              constructors.length));
    }
  }

  /**
   * Takes a map of constructor arguments keyed by parameter name and returns an array of arguments suitable
   * for passing to {@code Constructor.newInstance}.
   *
   * @param constructor  a constructor
   * @param arguments  arguments for the constructor, keyed by parameter name
   * @return an array of arguments suitable for passing to {@code Constructor.newInstance}
   * @throws IllegalArgumentException if the constructor requires arguments that aren't in the map, or
   *   if an argument type is not compatible with the parameter type
   */
  private static Object[] constructorArguments(Constructor<?> constructor, Map<String, Object> arguments) {
    Parameter[] parameters = constructor.getParameters();
    Object[] argumentArray = new Object[parameters.length];

    for (int i = 0; i < parameters.length; i++) {
      Parameter parameter = parameters[i];
      String parameterName = parameter.getName();
      Class<?> parameterType = parameter.getType();
      Object argument = arguments.get(parameterName);

      if (argument == null) {
        throw new IllegalArgumentException(
            Messages.format(
                "No argument found with name '{}'",
                parameterName));
      }
      if (!isArgumentCompatible(parameterType, argument.getClass())) {
        throw new IllegalArgumentException(
            Messages.format(
                "Argument is not compatible with the parameter type. name={}, value={}, type={}. Parameter type={}, " +
                    "constructor={}",
                parameterName,
                argument,
                argument.getClass().getName(),
                parameterType.getName(),
                constructor));
      }
      argumentArray[i] = argument;
    }
    return argumentArray;
  }

  /**
   * Returns true if the argument type is compatible with the parameter type.
   * <p>
   * An argument type is compatible if the parameter type is assignable from the argument type. If the
   * parameter type is primitive, they are compatible if the argument type is the boxed type for the primitive.
   *
   * @param parameterType  the type of a constructor parameter
   * @param argumentType  the type of the constructor argument
   * @return true if the argument type is compatible with the parameter type
   */
  private static boolean isArgumentCompatible(Class<?> parameterType, Class<?> argumentType) {
    if (parameterType.isAssignableFrom(argumentType)) {
      return true;
    }
    // If the parameter type isn't primitive and they're not assignable then they're definitely not compatible
    if (!parameterType.isPrimitive()) {
      return false;
    }
    return isArgumentCompatible(Primitives.wrap(parameterType), argumentType);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FunctionConfig}.
   * @return the meta-bean, not null
   */
  @SuppressWarnings("rawtypes")
  public static FunctionConfig.Meta meta() {
    return FunctionConfig.Meta.INSTANCE;
  }

  /**
   * The meta-bean for {@code FunctionConfig}.
   * @param <R>  the bean's generic type
   * @param cls  the bean's generic type
   * @return the meta-bean, not null
   */
  @SuppressWarnings("unchecked")
  public static <R extends CalculationTarget> FunctionConfig.Meta<R> metaFunctionConfig(Class<R> cls) {
    return FunctionConfig.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FunctionConfig.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an instance.
   * @param functionType  the value of the property, not null
   * @param arguments  the value of the property, not null
   */
  FunctionConfig(
      Class<? extends CalculationSingleFunction<T, ?>> functionType,
      Map<String, Object> arguments) {
    JodaBeanUtils.notNull(functionType, "functionType");
    JodaBeanUtils.notNull(arguments, "arguments");
    this.functionType = functionType;
    this.arguments = ImmutableMap.copyOf(arguments);
  }

  @SuppressWarnings("unchecked")
  @Override
  public FunctionConfig.Meta<T> metaBean() {
    return FunctionConfig.Meta.INSTANCE;
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
   * Gets the type of the function.
   * @return the value of the property, not null
   */
  private Class<? extends CalculationSingleFunction<T, ?>> getFunctionType() {
    return functionType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets constructor arguments used for building function instances, keyed by parameter name.
   * @return the value of the property, not null
   */
  private Map<String, Object> getArguments() {
    return arguments;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      FunctionConfig<?> other = (FunctionConfig<?>) obj;
      return JodaBeanUtils.equal(functionType, other.functionType) &&
          JodaBeanUtils.equal(arguments, other.arguments);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(functionType);
    hash = hash * 31 + JodaBeanUtils.hashCode(arguments);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("FunctionConfig{");
    buf.append("functionType").append('=').append(functionType).append(',').append(' ');
    buf.append("arguments").append('=').append(JodaBeanUtils.toString(arguments));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FunctionConfig}.
   * @param <T>  the type
   */
  public static final class Meta<T extends CalculationTarget> extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    @SuppressWarnings("rawtypes")
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code functionType} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<Class<? extends CalculationSingleFunction<T, ?>>> functionType = DirectMetaProperty.ofImmutable(
        this, "functionType", FunctionConfig.class, (Class) Class.class);
    /**
     * The meta-property for the {@code arguments} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<Map<String, Object>> arguments = DirectMetaProperty.ofImmutable(
        this, "arguments", FunctionConfig.class, (Class) Map.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "functionType",
        "arguments");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -211170510:  // functionType
          return functionType;
        case -2035517098:  // arguments
          return arguments;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends FunctionConfig<T>> builder() {
      return new FunctionConfig.Builder<T>();
    }

    @SuppressWarnings({"unchecked", "rawtypes" })
    @Override
    public Class<? extends FunctionConfig<T>> beanType() {
      return (Class) FunctionConfig.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code functionType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Class<? extends CalculationSingleFunction<T, ?>>> functionType() {
      return functionType;
    }

    /**
     * The meta-property for the {@code arguments} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Map<String, Object>> arguments() {
      return arguments;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -211170510:  // functionType
          return ((FunctionConfig<?>) bean).getFunctionType();
        case -2035517098:  // arguments
          return ((FunctionConfig<?>) bean).getArguments();
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
   * The bean-builder for {@code FunctionConfig}.
   * @param <T>  the type
   */
  private static final class Builder<T extends CalculationTarget> extends DirectFieldsBeanBuilder<FunctionConfig<T>> {

    private Class<? extends CalculationSingleFunction<T, ?>> functionType;
    private Map<String, Object> arguments = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -211170510:  // functionType
          return functionType;
        case -2035517098:  // arguments
          return arguments;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder<T> set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -211170510:  // functionType
          this.functionType = (Class<? extends CalculationSingleFunction<T, ?>>) newValue;
          break;
        case -2035517098:  // arguments
          this.arguments = (Map<String, Object>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder<T> set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder<T> setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder<T> setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder<T> setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public FunctionConfig<T> build() {
      return new FunctionConfig<T>(
          functionType,
          arguments);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("FunctionConfig.Builder{");
      buf.append("functionType").append('=').append(JodaBeanUtils.toString(functionType)).append(',').append(' ');
      buf.append("arguments").append('=').append(JodaBeanUtils.toString(arguments));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
