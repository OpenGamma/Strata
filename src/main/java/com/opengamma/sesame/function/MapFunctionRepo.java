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
 * TODO if this turns out to be a point of contention make it non-synchronized and more complicated
 * TODO can this be split into 2 interfaces, one for the engine and one to generate data to guide the user through configuration?
 */
public final class MapFunctionRepo implements FunctionRepo {

  /** Output names registered for a target type. */
  private final Map<Class<?>, Set<String>> _outputsByType = Maps.newHashMap();

  /**
   * All output names available for a target type. This is lazily populated by walking up the type hierarchy from
   * the target type querying {@link #_outputsByType}.
   */
  private final Map<Class<?>, Set<String>> _allOutputsByType = Maps.newHashMap();

  /**
   * Map of output name / target type to the function type that provides it. Only one function type is allowed for
   * each output/type pair. It must be an interface which extends {@link OutputFunction}.
   */
  private final Map<Pair<String, Class<?>>, Class<?>> _functionTypesForOutputs = Maps.newHashMap();

  /**
   * The same as {@link #_functionTypesForOutputs} but includes the function types for the target type's supertypes.
   * This is lazily populated by walking up the type hierarchy from the target type querying
   * {@link #_functionTypesForOutputs}.
   */
  private final Map<Pair<String, Class<?>>, Class<?>> _allFunctionTypesForOutputs = Maps.newHashMap();

  /**
   * Returns the names of all outputs available for a target type
   * @param targetType The type of the target
   * @return All outputs that can be calculated for the target type
   */
  @Override
  public synchronized Set<String> getAvailableOutputs(Class<?> targetType) {
    if (_allOutputsByType.containsKey(targetType)) {
      return _allOutputsByType.get(targetType);
    }
    Set<Class<?>> supertypes = ConfigUtils.getSupertypes(targetType);
    Set<String> outputs = Sets.newTreeSet();
    for (Class<?> supertype : supertypes) {
      if (_outputsByType.containsKey(supertype)) {
        outputs.addAll(_outputsByType.get(supertype));
      }
    }
    _allOutputsByType.put(targetType, outputs);
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
    Pair<String, Class<?>> targetKey = (Pair<String, Class<?>>) Pairs.of(outputName, targetType);
    if (_allFunctionTypesForOutputs.containsKey(targetKey)) {
      return (Class<? extends OutputFunction<?, ?>>) _allFunctionTypesForOutputs.get(targetKey);
    }
    Set<Class<?>> supertypes = ConfigUtils.getSupertypes(targetType);
    for (Class<?> supertype : supertypes) {
      Pair<String, Class<?>> key = (Pair<String, Class<?>>) Pairs.of(outputName, supertype);
      if (_functionTypesForOutputs.containsKey(key)) {
        Class<? extends OutputFunction<?, ?>> functionType =
            (Class<? extends OutputFunction<?, ?>>) _functionTypesForOutputs.get(key);
        _allFunctionTypesForOutputs.put(targetKey, functionType);
        return functionType;
      }
    }
    return null;
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
    // clear the lazily populated caches which might be out of date after registering a new type
    _allOutputsByType.clear();
    _allFunctionTypesForOutputs.clear();
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
    if (_outputsByType.containsKey(targetType)) {
      _outputsByType.get(targetType).add(outputName);
    } else {
      outputNames = Sets.newHashSet(outputName);
      _outputsByType.put(targetType, outputNames);
    }
    _functionTypesForOutputs.put(Pairs.<String, Class<?>>of(outputName, targetType), type);
  }

  // needed for implementations of interfaces where type isn't named as @DefaultImplementation
  private void registerImplementation(Class<?> type) {
    // TODO store an entry for this class against all interfaces it implements. what about dupes?
    throw new UnsupportedOperationException("registerImplementation not implemented");
  }
}
