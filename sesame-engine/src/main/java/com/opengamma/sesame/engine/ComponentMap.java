/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.opengamma.DataNotFoundException;
import com.opengamma.util.ArgumentChecker;

/**
 * A map of components keyed by their type.
 */
public final class ComponentMap {
  // TODO rename ComponentLookup or create interface and have this implement it
  // TODO would it be better to compose by delegation/chaining instead of with()?

  /**
   * The empty component map.
   */
  public static final ComponentMap EMPTY = new ComponentMap(Collections.<Class<?>, Object>emptyMap());
  private final ImmutableMap<Class<?>, Object> _components;

  private ComponentMap(Map<Class<?>, Object> components) {
    _components = ImmutableMap.copyOf(components);
  }

  /**
   * Returns a component or throws an exception if there is no component available of the required type.
   * @param type The required component type
   * @param <T> The required component type
   * @return A component of the required type, not null
   * @throws DataNotFoundException If there is no component of the specified type
   */
  @SuppressWarnings("unchecked")
  public <T> T getComponent(Class<T> type) {
    T component = (T) _components.get(ArgumentChecker.notNull(type, "type"));
    if (component == null) {
      throw new DataNotFoundException("No component found of type " + type);
    }
    return component;
  }

  /**
   * Returns a component or null if there is no component available of the required type.
   * @param type The required component type
   * @param <T> The required component type
   * @return A component of the required type or null if there isn't one
   */
  @SuppressWarnings("unchecked")
  public <T> T findComponent(Class<T> type) {
    return (T) _components.get(ArgumentChecker.notNull(type, "type"));
  }

  public ComponentMap with(Map<Class<?>, Object> components) {
    ArgumentChecker.notNull(components, "components");
    ImmutableMap.Builder<Class<?>, Object> builder = ImmutableMap.builder();
    return new ComponentMap(builder.putAll(_components).putAll(components).build());
  }

  public <T, U extends T> ComponentMap with(Class<T> type, U component) {
    ArgumentChecker.notNull(type, "type");
    ArgumentChecker.notNull(component, "component");
    ImmutableMap.Builder<Class<?>, Object> builder = ImmutableMap.builder();
    return new ComponentMap(builder.putAll(_components).put(type, component).build());
  }

  public static ComponentMap of(Map<Class<?>, Object> components) {
    ArgumentChecker.notNull(components, "components");
    return new ComponentMap(ImmutableMap.copyOf(components));
  }

  /**
   * @return The components keyed by type
   */
  public Map<Class<?>, Object> getComponents() {
    return _components;
  }

  /**
   * @return The types of the available components
   */
  public Set<Class<?>> getComponentTypes() {
    return _components.keySet();
  }
}
