/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.MapStream;

/**
 * The default calculation functions implementation.
 * <p>
 * This provides the complete set of functions that will be used in a calculation.
 * Each {@link CalculationFunction} handles a specific type of {@link CalculationTarget},
 * thus the functions are keyed in a {@code Map} by the target type {@code Class}.
 */
@BeanDefinition(style = "light")
final class DefaultCalculationFunctions
    implements CalculationFunctions, ImmutableBean, Serializable {

  /**
   * An empty instance.
   */
  static final DefaultCalculationFunctions EMPTY = new DefaultCalculationFunctions(ImmutableMap.of());

  /**
   * The functions, keyed by target type.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Class<?>, CalculationFunction<?>> functions;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified functions.
   * <p>
   * The map will be validated to ensure the {@code Class} is consistent with
   * {@link CalculationFunction#targetType()}.
   * 
   * @param functions  the functions
   * @return the calculation functions
   */
  static DefaultCalculationFunctions of(Map<Class<?>, ? extends CalculationFunction<?>> functions) {
    return new DefaultCalculationFunctions(ImmutableMap.copyOf(functions));
  }

  @ImmutableValidator
  private void validate() {
    for (Entry<Class<?>, CalculationFunction<?>> entry : functions.entrySet()) {
      ArgChecker.isTrue(
          entry.getValue().targetType().isAssignableFrom(entry.getKey()),
          "Invalid map, key and function mismatch: {} and {}", entry.getKey(), entry.getValue().targetType());
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public <T extends CalculationTarget> CalculationFunction<? super T> getFunction(T target) {
    @SuppressWarnings("unchecked")
    CalculationFunction<? super T> function = (CalculationFunction<? super T>) functions.get(target.getClass());
    return function != null ? function : MissingConfigCalculationFunction.INSTANCE;
  }

  @Override
  public <T extends CalculationTarget> Optional<CalculationFunction<? super T>> findFunction(T target) {
    @SuppressWarnings("unchecked")
    CalculationFunction<? super T> function = (CalculationFunction<? super T>) functions.get(target.getClass());
    return Optional.ofNullable(function);
  }

  @Override
  public CalculationFunctions composedWith(DerivedCalculationFunction<?, ?>... derivedFunctions) {
    // Override the default implementation for efficiency.
    // The default implementation uses DerivedCalculationFunctions which creates a function instance for every target.
    // This class can do better and can create a single function instance for each target type.
    Map<Class<?>, List<DerivedCalculationFunction<?, ?>>> functionsByTargetType =
        Arrays.stream(derivedFunctions).collect(groupingBy(fn -> fn.targetType()));

    // The calculation functions wrapped up with the derived functions which use them
    List<CalculationFunction<?>> wrappedFunctions = MapStream.of(functionsByTargetType)
        .map((targetType, fns) -> wrap(targetType, fns))
        .collect(toList());

    Map<Class<?>, CalculationFunction<?>> allFunctions = new HashMap<>(functions);
    wrappedFunctions.forEach(fn -> allFunctions.put(fn.targetType(), fn));
    return CalculationFunctions.of(allFunctions);
  }

  @SuppressWarnings("unchecked")
  private <T extends CalculationTarget, R> CalculationFunction<?> wrap(
      Class<?> targetType,
      List<DerivedCalculationFunction<?, ?>> derivedFunctions) {

    CalculationFunction<? super T> function = (CalculationFunction<? super T>) functions.get(targetType);

    if (function == null) {
      function = MissingConfigCalculationFunction.INSTANCE;
    }
    CalculationFunction<? super T> wrappedFn = function;

    for (DerivedCalculationFunction<?, ?> derivedFn : derivedFunctions) {
      // These casts are necessary because the type information is lost when the functions are stored in the map.
      // They are safe because T is the target type which is is the map key and R isn't actually used
      CalculationFunction<T> wrappedFnCast = (CalculationFunction<T>) wrappedFn;
      DerivedCalculationFunction<T, R> derivedFnCast = (DerivedCalculationFunction<T, R>) derivedFn;
      wrappedFn = new DerivedCalculationFunctionWrapper<>(derivedFnCast, wrappedFnCast);
    }
    return wrappedFn;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code DefaultCalculationFunctions}.
   */
  private static final MetaBean META_BEAN = LightMetaBean.of(DefaultCalculationFunctions.class);

  /**
   * The meta-bean for {@code DefaultCalculationFunctions}.
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

  private DefaultCalculationFunctions(
      Map<Class<?>, CalculationFunction<?>> functions) {
    JodaBeanUtils.notNull(functions, "functions");
    this.functions = ImmutableMap.copyOf(functions);
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
   * Gets the functions, keyed by target type.
   * @return the value of the property, not null
   */
  public ImmutableMap<Class<?>, CalculationFunction<?>> getFunctions() {
    return functions;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      DefaultCalculationFunctions other = (DefaultCalculationFunctions) obj;
      return JodaBeanUtils.equal(functions, other.functions);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(functions);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("DefaultCalculationFunctions{");
    buf.append("functions").append('=').append(JodaBeanUtils.toString(functions));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
