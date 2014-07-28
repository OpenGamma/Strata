/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.Map;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
  public static final ComponentMap EMPTY = new ComponentMap(ImmutableClassToInstanceMap.copyOf(ImmutableMap.of()));

  /**
   * The set of components.
   */
  private final ImmutableClassToInstanceMap<Object> _components;

  //-------------------------------------------------------------------------
  /**
   * Creates a component map.
   * <p>
   * The input must conform to the restrictions of {@link ClassToInstanceMap}.
   * The looser input type is specified for caller convenience.
   * 
   * @param components  the map of components
   * @return the component map
   */
  public static ComponentMap of(Map<Class<?>, Object> components) {
    ArgumentChecker.notNull(components, "components");
    return new ComponentMap(ImmutableClassToInstanceMap.copyOf(components));
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param components  the map of components
   */
  private ComponentMap(ImmutableClassToInstanceMap<Object> components) {
    _components = components;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a component or throws an exception if there is no component available of the required type.
   * 
   * @param <T>  the required component type
   * @param type  the required component type
   * @return a component of the required type, not null
   * @throws DataNotFoundException if there is no component of the specified type
   */
  public <T> T getComponent(Class<T> type) {
    T component = _components.getInstance(ArgumentChecker.notNull(type, "type"));
    if (component == null) {
      throw new DataNotFoundException("No component found of type " + type);
    }
    return component;
  }

  /**
   * Returns a component or null if there is no component available of the required type.
   * 
   * @param <T>  the required component type
   * @param type  the required component type
   * @return a component of the required type or null if there isn't one
   */
  public <T> T findComponent(Class<T> type) {
    return _components.getInstance(ArgumentChecker.notNull(type, "type"));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a new component map with the specified components added.
   * 
   * @param components  the components to add
   * @return the new map
   */
  public ComponentMap with(Map<Class<?>, Object> components) {
    ArgumentChecker.notNull(components, "components");
    ImmutableClassToInstanceMap.Builder<Object> builder = ImmutableClassToInstanceMap.builder();
    return new ComponentMap(builder.putAll(_components).putAll(components).build());
  }

  /**
   * Returns a new component map with the specified component added.
   * 
   * @param type  the component type
   * @param component  the component to add
   * @return the new map
   */
  public <T, U extends T> ComponentMap with(Class<T> type, U component) {
    ArgumentChecker.notNull(type, "type");
    ArgumentChecker.notNull(component, "component");
    ImmutableClassToInstanceMap.Builder<Object> builder = ImmutableClassToInstanceMap.builder();
    return new ComponentMap(builder.putAll(_components).put(type, component).build());
  }

  /**
   * Gets the entire map of components.
   * 
   * @return the components keyed by type
   */
  public ImmutableClassToInstanceMap<Object> getComponents() {
    return _components;
  }

  /**
   * Gets the set of available component types.
   * 
   * @return the types of the available components
   */
  public ImmutableSet<Class<?>> getComponentTypes() {
    return ImmutableSet.copyOf(_components.keySet());
  }

}
