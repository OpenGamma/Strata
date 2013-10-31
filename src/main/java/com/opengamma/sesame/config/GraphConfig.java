/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import com.opengamma.sesame.function.Provider;

/**
 * TODO interface?
 * the hard thing is knowing how much context is available at each call site
 * will the caller know the column name? input type? what level does this need to be scoped at?
 *
 * needs to compose multiple levels of defaults and multiple levels of type hierarchy
 *
 * where is this needed?
 *   FunctionModel.createNode
 *     providers
 *     function impl
 *     constructor args
 *   Engine
 *     function args
 *
 * or should they be separate? similar logic but no overlap
 *   GraphConfig / BuildConfig
 *   RunConfig / Arguments / ArgumentConfig
 * should the fn repo be at one level of the lookup? it knows about default impls if there's only 1 impl of an interface
 *
 * TODO start with an impl that ignores subtyping and just works with exact types? less of a headache and enough for now
 */
public class GraphConfig {

  // needs to walk up the config hierarchy but only for the specific type. constructor args are specific to a class
  public Object getConstructorArgument(Class<?> objectType, Class<?> parameterType, String name) {
    throw new UnsupportedOperationException();
  }

  // arguments for the method that implements the function and returns the output.
  // need to walk up the hierarchy to include everything that implements the function interface
  // go up the config hierarchy first, then the type hierarchy. a param for the exact type at a higher level in the
  // config should be used in preference to a less specific type at a closer level in the config TODO confirm this
  public Object getFunctionArgument(Class<?> objectType, Class<?> parameterType, String name) {
    throw new UnsupportedOperationException();
  }

  // walk up the config hierarchy
  public Class<?> getImplementation(Class<?> interfaceType) {
    throw new UnsupportedOperationException();
  }

  // walk up the config hierarchy looking for mappings for the exact type
  // check the fn repo for impls? can't look for impls directly because that would mean going down the type hierarchy
  // only the repo has encoded the downwards hierarchy, can't do that via introspection
  public Provider<?> getProvider(Class<?> type) {
    throw new UnsupportedOperationException();
  }
}
