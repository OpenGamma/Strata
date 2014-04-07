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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.opengamma.core.position.Position;
import com.opengamma.core.position.Trade;
import com.opengamma.core.security.Security;
import com.opengamma.sesame.OutputName;
import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

/**
 *
 */
public class AvailableOutputsImpl implements AvailableOutputs {

  private static final Set<Class<?>> s_defaultInputTypes =
      ImmutableSet.<Class<?>>of(Trade.class, Position.class, Security.class);

  private final Set<Class<?>> _inputTypes;

  /** Output names registered for an input type. */
  private final Map<Class<?>, Set<OutputName>> _outputsByInputType = Maps.newHashMap();

  /** Map of output name / target type to the function type that provides it. */
  private final Map<Pair<OutputName, Class<?>>, FunctionMetadata> _functionsForOutputs = Maps.newHashMap();

  /**
   * The same as {@link #_functionsForOutputs} but includes the function types for the target type's supertypes.
   * This is lazily populated by walking up the type hierarchy from the target type querying
   * {@link #_functionsForOutputs}.
   */
  private final Map<Pair<OutputName, Class<?>>, FunctionMetadata> _allFunctionsForOutputs = Maps.newHashMap();

  /**
   * All output names available for a target type. This is lazily populated by walking up the type hierarchy from
   * the target type querying {@link #_outputsByInputType}.
   */
  private final Map<Class<?>, Set<OutputName>> _allOutputsByInputType = Maps.newHashMap();

  private final Map<OutputName, FunctionMetadata> _nonPortfolioFunctions = Maps.newHashMap();

  /**
   * Creates an instance that expects {@link Trade}, {@link Position} or {@link Security} instances as inputs.
   */
  public AvailableOutputsImpl() {
    this(s_defaultInputTypes);
  }

  /**
   * Creates an instance that expects the specified types as inputs.
   * @param inputTypes the expected input types
   */
  public AvailableOutputsImpl(Class<?>... inputTypes) {
    _inputTypes = ImmutableSet.copyOf(inputTypes);
  }

  /**
   * Creates an instance that expects the specified types as inputs.
   * @param inputTypes the expected input types
   */
  public AvailableOutputsImpl(Set<Class<?>> inputTypes) {
    _inputTypes = ImmutableSet.copyOf(ArgumentChecker.notNull(inputTypes, "inputTypes"));
  }

  @Override
  public Set<Class<?>> getInputTypes(OutputName outputName) {
    // TODO need the reverse of _outputsByInputType
    throw new UnsupportedOperationException("getInputTypes not implemented");
  }

  /**
   * Returns the names of all outputs available for a target type
   * @param inputType The type of the target
   * @return All outputs that can be calculated for the target type
   */
  @Override
  public synchronized Set<OutputName> getAvailableOutputs(Class<?> inputType) {
    if (_allOutputsByInputType.containsKey(inputType)) {
      return _allOutputsByInputType.get(inputType);
    }
    Set<Class<?>> supertypes = EngineUtils.getSupertypes(inputType);
    Set<OutputName> outputs = Sets.newTreeSet();
    for (Class<?> supertype : supertypes) {
      if (_outputsByInputType.containsKey(supertype)) {
        outputs.addAll(_outputsByInputType.get(supertype));
      }
    }
    _allOutputsByInputType.put(inputType, outputs);
    return Collections.unmodifiableSet(outputs);
  }

  @Override
  public Set<OutputName> getAvailableOutputs() {
    // TODO implement getAvailableOutputs()
    throw new UnsupportedOperationException("getAvailableOutputs not implemented");
  }

  @Override
  public synchronized FunctionMetadata getOutputFunction(OutputName outputName, Class<?> inputType) {
    ArgumentChecker.notNull(outputName, "outputName");
    ArgumentChecker.notNull(inputType, "inputType");

    Pair<OutputName, Class<?>> targetKey = Pairs.<OutputName, Class<?>>of(outputName, inputType);
    if (_allFunctionsForOutputs.containsKey(targetKey)) {
      return _allFunctionsForOutputs.get(targetKey);
    }
    Set<Class<?>> supertypes = EngineUtils.getSupertypes(inputType);
    for (Class<?> supertype : supertypes) {
      Pair<OutputName, Class<?>> key = Pairs.<OutputName, Class<?>>of(outputName, supertype);
      if (_functionsForOutputs.containsKey(key)) {
        FunctionMetadata function = _functionsForOutputs.get(key);
        _allFunctionsForOutputs.put(targetKey, function);
        return function;
      }
    }
    return null;
  }

  @Override
  public synchronized FunctionMetadata getOutputFunction(OutputName outputName) {
    return _nonPortfolioFunctions.get(outputName);
  }

  @Override
  public synchronized void register(Class<?>... functionInterfaces) {
    for (Class<?> functionInterface : functionInterfaces) {
      List<FunctionMetadata> functions = getOutputFunctions(functionInterface);
      for (FunctionMetadata function : functions) {
        OutputName outputName = function.getOutputName();
        Set<OutputName> outputNames;
        Class<?> targetType = function.getInputType();

        if (targetType != null) { // portfolio output
          if (_outputsByInputType.containsKey(targetType)) {
            _outputsByInputType.get(targetType).add(outputName);
          } else {
            outputNames = Sets.newHashSet(outputName);
            _outputsByInputType.put(targetType, outputNames);
          }
          _functionsForOutputs.put(Pairs.<OutputName, Class<?>>of(outputName, targetType), function);
        } else { // non-portfolio output
          _nonPortfolioFunctions.put(outputName, function);
        }
      }
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
