/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.opengamma.DataNotFoundException;
import com.opengamma.core.position.Position;
import com.opengamma.core.position.Trade;
import com.opengamma.core.security.Security;
import com.opengamma.sesame.config.ConfigUtils;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

/**
 * Extremely simple {@link FunctionRepo} backed by a map.
 * TODO if this turns out to be a point of contention make it non-synchronized and more complicated
 * TODO can this be split into 2 interfaces, one for the engine and one to generate data to guide the user through configuration?
 */
public final class SimpleFunctionRepo implements FunctionRepo {

  private static final Set<Class<?>> s_defaultInputTypes =
      ImmutableSet.<Class<?>>of(Trade.class, Position.class, Security.class);

  private final Set<Class<?>> _inputTypes;

  /** Output names registered for an input type. */
  private final Map<Class<?>, Set<String>> _outputsByInputType = Maps.newHashMap();

  /**
   * All output names available for a target type. This is lazily populated by walking up the type hierarchy from
   * the target type querying {@link #_outputsByInputType}.
   */
  private final Map<Class<?>, Set<String>> _allOutputsByInputType = Maps.newHashMap();

  /**
   * Map of output name / target type to the function type that provides it.
   */
  // TODO create OutputKey instead of Pair<String, Class>?
  private final Map<Pair<String, Class<?>>, FunctionMetadata> _functionsForOutputs = Maps.newHashMap();

  /**
   * The same as {@link #_functionsForOutputs} but includes the function types for the target type's supertypes.
   * This is lazily populated by walking up the type hierarchy from the target type querying
   * {@link #_functionsForOutputs}.
   */
  private final Map<Pair<String, Class<?>>, FunctionMetadata> _allFunctionsForOutputs = Maps.newHashMap();

  /**
   * Classes that implement registered function interfaces, keyed by the interface type.
   * This is needed to tell the user of the available options when they're configuring the view.
   */
  private final SetMultimap<Class<?>, Class<?>> _interfaceToImplementations = HashMultimap.create();

  public SimpleFunctionRepo(Set<Class<?>> inputTypes) {
    _inputTypes = ImmutableSet.copyOf(ArgumentChecker.notNull(inputTypes, "inputTypes"));
  }

  public SimpleFunctionRepo() {
    this(s_defaultInputTypes);
  }

  @Override
  public Set<Class<?>> getInputTypes(String outputName) {
    // TODO need the reverse of _outputsByInputType
    throw new UnsupportedOperationException("getInputTypes not implemented");
  }

  /**
   * Returns the names of all outputs available for a target type
   * @param inputType The type of the target
   * @return All outputs that can be calculated for the target type
   */
  @Override
  public synchronized Set<String> getAvailableOutputs(Class<?> inputType) {
    if (_allOutputsByInputType.containsKey(inputType)) {
      return _allOutputsByInputType.get(inputType);
    }
    Set<Class<?>> supertypes = ConfigUtils.getSupertypes(inputType);
    Set<String> outputs = Sets.newTreeSet();
    for (Class<?> supertype : supertypes) {
      if (_outputsByInputType.containsKey(supertype)) {
        outputs.addAll(_outputsByInputType.get(supertype));
      }
    }
    _allOutputsByInputType.put(inputType, outputs);
    return Collections.unmodifiableSet(outputs);
  }

  @Override
  public Set<String> getAvailableOutputs() {
    // TODO implement getAvailableOutputs()
    throw new UnsupportedOperationException("getAvailableOutputs not implemented");
  }

  @Override
  public FunctionMetadata getOutputFunction(String outputName) {
    // TODO implement getOutputFunction()
    throw new UnsupportedOperationException("getOutputFunction not implemented");
  }

  // this is used to build the graph
  /**
   * Returns metadata for the function that provides an output for an input type.
   * An output is provided by a method annotated with {@link Output}.
   *
   * @param outputName The output name
   * @param inputType The type of the input
   * @return The function that can provide the output
   * @throws DataNotFoundException If nothing can provide the requested output for the target type
   */
  @Override
  @SuppressWarnings("unchecked")
  public synchronized FunctionMetadata getOutputFunction(String outputName, Class<?> inputType) {
    Pair<String, Class<?>> targetKey = (Pair<String, Class<?>>) Pairs.of(outputName, inputType);
    if (_allFunctionsForOutputs.containsKey(targetKey)) {
      return _allFunctionsForOutputs.get(targetKey);
    }
    Set<Class<?>> supertypes = ConfigUtils.getSupertypes(inputType);
    for (Class<?> supertype : supertypes) {
      Pair<String, Class<?>> key = (Pair<String, Class<?>>) Pairs.of(outputName, supertype);
      if (_functionsForOutputs.containsKey(key)) {
        FunctionMetadata function = _functionsForOutputs.get(key);
        _allFunctionsForOutputs.put(targetKey, function);
        return function;
      }
    }
    return null;
  }

  // this is to allow the user to choose different implementations of functions when constructing the graph
  /**
   * Returns all known classes that implement an interface
   * @param interfaceType The interface
   * @return A set of classes that implement it TODO empty set or DataNotFoundException if there are none?
   */
  @Override
  public synchronized Set<Class<?>> getImplementationTypes(Class<?> interfaceType) {
    return _interfaceToImplementations.get(interfaceType);
  }

  /**
   * Returns the implementation type for an interface is there is only one implementation, null is there are
   * zero or multiple known implementations.
   * @param interfaceType An interface
   * @return The implementation type for an interface is there is only one implementation, null is there are
   * zero or multiple known implementations
   */
  @Override
  public synchronized Class<?> getDefaultImplementation(Class<?> interfaceType) {
    Set<Class<?>> impls = _interfaceToImplementations.get(interfaceType);
    if (impls.size() == 1) {
      return impls.iterator().next();
    }
    return null;
  }

  // type must have at least one method annotated with @Output with a parameter annotated with @Target
  @SuppressWarnings("unchecked")
  public synchronized void register(Class<?> type) {
    // clear the lazily populated caches which might be out of date after registering a new type
    _allOutputsByInputType.clear();
    _allFunctionsForOutputs.clear();
    registerOutputs(type);
    registerImplementation(type);
  }

  public synchronized void register(Class<?>... types) {
    for (Class<?> type : types) {
      register(type);
    }
  }

  // TODO I think reflections can tell us all impls for an interface so this might be efficient
  // void register(Class<?> interfaceType, Class<?>... implTypes)

  /**
   * Registers the outputs that can be produced by a function type.
   * @param type The
   */
  private void registerOutputs(Class<?> type) {
    List<FunctionMetadata> outputs = getOutputFunctions(type);
    for (FunctionMetadata function : outputs) {
      String outputName = function.getOutputName();
      Set<String> outputNames;
      Class<?> targetType = function.getInputType();
      if (_outputsByInputType.containsKey(targetType)) {
        _outputsByInputType.get(targetType).add(outputName);
      } else {
        outputNames = Sets.newHashSet(outputName);
        _outputsByInputType.put(targetType, outputNames);
      }
      _functionsForOutputs.put(Pairs.<String, Class<?>>of(outputName, targetType), function);
    }
  }

  // TODO this is only necessary for informing the user of the options if they don't specify an implementation for an interface
  private void registerImplementation(Class<?> type) {
    if (type.isInterface() || type.isPrimitive()) {
      return;
    }
    Set<Class<?>> interfaces = ConfigUtils.getInterfaces(type);
    for (Class<?> iface : interfaces) {
      _interfaceToImplementations.put(iface, type);
    }
  }

  private List<FunctionMetadata> getOutputFunctions(Class<?> type) {
    List<FunctionMetadata> functions = Lists.newArrayList();
    for (Method method : type.getMethods()) {
      if (method.isAnnotationPresent(Output.class)) {
        FunctionMetadata function = new FunctionMetadata(method, _inputTypes);
        functions.add(function);
      }
    }
    // TODO check that there aren't any clashes between constructor and method param names
    // shouldn't matter if two methods have the same param names, the engine will only be calling one
    return functions;
  }
}
