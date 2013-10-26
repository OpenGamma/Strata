/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.opengamma.sesame.config.ConfigUtils;

/**
 * Extremely simple {@link FunctionRepo} backed by a map.
 * It should be populated from a single thread but can safely be read from multiple threads after initialization.
 * TODO wouldn't be hard to make it thread safe, fix race condition in registerOutput
 */
public final class MapFunctionRepo implements FunctionRepo {

  // TODO use a synchronized multimap? this is unlikely to be a performance hot spot
  private final ConcurrentMap<Class<?>, Set<String>> _valueNamesByType = Maps.newConcurrentMap();

  @Override
  public Set<String> getAvailableOutputs(Class<?> targetType) {
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

  @Override
  public Class<? extends OutputFunction<?, ?>> getFunctionType(String outputName, Class<?> targetType) {
    // TODO implement getFunctionType()
    // use getAvailableOutputs to build the result?
    throw new UnsupportedOperationException("getFunctionType not implemented");
  }

  @Override
  public Set<Class<?>> getFunctionImplementations(Class<?> functionInterface) {
    // TODO implement getFunctionImplementations()
    throw new UnsupportedOperationException("getFunctionImplementations not implemented");
  }

  @SuppressWarnings("unchecked")
  public void register(Class<?> type) {
    if (OutputFunction.class.isAssignableFrom(type) && type.isInterface()) {
      registerOutput((Class<? extends OutputFunction<?, ?>>) type);
    } else if (type.isInterface()) {
      registerInterface(type);
    } else {
      registerImplementation(type);
    }
  }

  // TODO is this actually necessary?
  // interfaces are either output functions (in which case they use registerOutput)
  // or they're direct dependencies of other classes in which case they don't need to be registered
  // but what about output names on intermediate results? e.g. for additional outputs. might need to register for that
  private void registerInterface(Class<?> type) {
    // TODO implement MapFunctionRepo.registerInterface()
    throw new UnsupportedOperationException("registerInterface not implemented");
  }

  private void registerOutput(Class<? extends OutputFunction<?, ?>> type) {
    // TODO register the default impl?
    String outputName = EngineFunctionUtils.getOutputName(type);
    Set<String> outputNames;
    Class<?> targetType = EngineFunctionUtils.getTargetType(type);
    if (_valueNamesByType.containsKey(targetType)) {
      // TODO this is a race condition. if anyone cares about multi-threaded registration it could be fixed
      // with a loop and putIfAbsent / replace
      outputNames = ImmutableSet.<String>builder().addAll(_valueNamesByType.get(targetType)).add(outputName).build();
    } else {
      outputNames = ImmutableSet.of(outputName);
    }
    _valueNamesByType.put(targetType, outputNames);
  }

  // needed for implementations of interfaces where type isn't named as @DefaultImplementation
  private void registerImplementation(Class<?> type) {
    // TODO implement MapFunctionRepo.registerImplementation()
    throw new UnsupportedOperationException("registerImplementation not implemented");
  }
}
