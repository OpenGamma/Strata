/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Extremely simple {@link FunctionRepo} backed by a map.
 * It should be populated from a single thread but can safely be read from multiple threads after initialization.
 * TODO wouldn't be hard to make it thread safe, fix race condition in registerInterface
 */
/* package */ class MapFunctionRepo implements FunctionRepo {

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
    return Collections.unmodifiableSet(outputs);
  }

  @Override
  public Class<?> getFunctionType(String valueName, Class<?> targetType) {
    // TODO implement getFunctionType()
    throw new UnsupportedOperationException("getFunctionType not implemented");
  }

  @Override
  public Set<Class<?>> getFunctionImplementations(Class<?> functionInterface) {
    // TODO implement getFunctionImplementations()
    throw new UnsupportedOperationException("getFunctionImplementations not implemented");
  }

  /* package */ void register(Class<?> type) {
    if (type.isInterface()) {
      registerInterface(type);
    } else {
      registerImplementation(type);
    }
  }

  private void registerInterface(Class<?> type) {
    FunctionMetadata functionMeta = FunctionMetadata.forFunctionInterface(type);
    // TODO register the default impl?
    //Class<?> defaultImplementation = functionMeta.getDefaultImplementation();
    String valueName = functionMeta.getValueName();
    Set<String> valueNames;
    Class<?> targetType = functionMeta.getTargetType();
    if (_valueNamesByType.containsKey(targetType)) {
      // TODO this is a race condition. if anyone cares about multi-threaded registration it could be fixed
      // with a loop and putIfAbsent / replace
      valueNames = ImmutableSet.<String>builder().addAll(_valueNamesByType.get(targetType)).add(valueName).build();
    } else {
      valueNames = ImmutableSet.of(valueName);
    }
    _valueNamesByType.put(targetType, valueNames);
  }

  private void registerImplementation(Class<?> type) {
    // TODO implement MapFunctionRepo.registerImplementation()
    throw new UnsupportedOperationException("registerImplementation not implemented");
  }
}
