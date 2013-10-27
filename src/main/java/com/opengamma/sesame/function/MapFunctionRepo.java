/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.opengamma.DataNotFoundException;
import com.opengamma.sesame.config.ConfigUtils;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

/**
 * Extremely simple {@link FunctionRepo} backed by a map.
 * TODO if this turns out to be a point of contention make it non-synchronized
 * TODO can this be split into 2 interfaces, one for the engine and one to generate data to guide the user through configuration?
 */
public final class MapFunctionRepo implements FunctionRepo {

  private final Map<Class<?>, Set<String>> _valueNamesByType = Maps.newHashMap();

  /**
   * Map of output name / target type to the function type that provides it. Only one function type is allowed for
   * each output/type pair but it must implement {@link OutputFunction} and is normally an interface so there
   * can but multiple implementations configured using the normal override mechanism.
   */
  private final Map<Pair<String, Class<?>>, Class<?>> _functionTypesForOutputs = Maps.newHashMap();

  // TODO cache of function for an output/target pair calculated by climbing the type hierarchy and querying _functionTypesForOutputs

  // TODO cache of available outputs for a target type (i.e. cache the result of getAvailableOutputs

  // this is for telling the user how they can configure the view

  /**
   * Returns the names of all outputs available for a target type
   * @param targetType The type of the target
   * @return All outputs that can be calculated for the target type
   */
  @Override
  public synchronized Set<String> getAvailableOutputs(Class<?> targetType) {
    Set<Class<?>> supertypes = ConfigUtils.getSupertypes(targetType);
    Set<String> outputs = Sets.newTreeSet();
    for (Class<?> supertype : supertypes) {
      if (_valueNamesByType.containsKey(supertype)) {
        outputs.addAll(_valueNamesByType.get(supertype));
      }
    }
    // TODO cache in a different map for performance
    // not in the same map because it's impossible to know the difference between the initial values that are only
    // for the key type or the replacement values that include supertype outputs
    return Collections.unmodifiableSet(outputs);
  }

  // this is used to build the graph
  /**
   * Returns the type of the {@link OutputFunction} subtype that provides an output for a target type.
   * This must be an interface annotated with {@link OutputName}.
   * @param outputName The output name
   * @param targetType The type of the target
   * @return The interface or class that can provide the output
   * @throws DataNotFoundException If nothing can provide the requested output for the target type
   */
  @Override
  @SuppressWarnings("unchecked")
  public synchronized Class<? extends OutputFunction<?, ?>> getFunctionType(String outputName, Class<?> targetType) {
    // TODO if this is called frequently it might be better to use nested maps to avoid creating lots of Pairs
    Pair<String, Class<?>> key = (Pair<String, Class<?>>) Pairs.of(outputName, targetType);
    if (_functionTypesForOutputs.containsKey(key)) {
      return (Class<? extends OutputFunction<?, ?>>) _functionTypesForOutputs.get(key);
    } else {
      // TODO walk up the type hierarchy and try each type, cache the result
      throw new DataNotFoundException("No function found for output " + outputName + " and targetType " + targetType.getName());
    }
  }

  // this is to allow the user to choose different implementations of functions when constructing the graph
  /**
   * Returns all known classes that implement an interface
   * @param functionInterface The interface
   * @return A set of classes that implement it TODO empty set or DataNotFoundException if there are none?
   */
  @Override
  public synchronized Set<Class<?>> getFunctionImplementations(Class<?> functionInterface) {
    // TODO implement getFunctionImplementations()
    throw new UnsupportedOperationException("getFunctionImplementations not implemented");
  }

  @SuppressWarnings("unchecked")
  public synchronized void register(Class<?> type) {
    if (OutputFunction.class.isAssignableFrom(type) && type.isInterface()) {
      registerOutput((Class<? extends OutputFunction<?, ?>>) type);
    } else {
      registerImplementation(type);
    }
  }

  private void registerOutput(Class<? extends OutputFunction<?, ?>> type) {
    // TODO register the default impl? it will be found anyway from the annotation. is there any other reason?
    // user will need to know all possible implementations for configuring the view
    String outputName = EngineFunctionUtils.getOutputName(type);
    Set<String> outputNames;
    Class<?> targetType = EngineFunctionUtils.getTargetType(type);
    if (_valueNamesByType.containsKey(targetType)) {
      _valueNamesByType.get(targetType).add(outputName);
    } else {
      outputNames = Sets.newHashSet(outputName);
      _valueNamesByType.put(targetType, outputNames);
    }
    _functionTypesForOutputs.put(Pairs.<String, Class<?>>of(outputName, targetType), type);
  }

  // needed for implementations of interfaces where type isn't named as @DefaultImplementation
  private void registerImplementation(Class<?> type) {
    // TODO store an entry for this class against all interfaces it implements. what about dupes?
    throw new UnsupportedOperationException("registerImplementation not implemented");
  }
}
