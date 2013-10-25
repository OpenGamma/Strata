/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import java.util.Set;

/**
 * Repository for engine functions. This returns the function type that can satisfy a value name for a target type.
 * The
 * TODO better name
 * TODO this is inadequate. need to handle subclassing. ClassMap *might* be adequate but I doubt it
 * 2 different functions
 *   1 - what outputs are available for a target type
 *   2 - what implementations are available for a function interface
 * #1 is for choosing the output nodes (i.e. root node in the Trees)
 * #2 is for building FunctionConfig
 *   choosing the impls at each level (i.e. after the Tree is built for their choice of output and target type)
 *   specifying user arguments for function construction
 *
 * #2 is easy - map(fnInterfaceType -> set(implType))
 * #1 needs to handle subclassing
 */
public interface FunctionRepo {

  // for when the user is configuring a column
  Set<String> getAvailableOutputs(Class<?> targetType);

  // users selects output for a column/type, this gives the output function type (probably interface)
  Class<?> getFunctionType(String valueName, Class<?> targetType);

  // after selecting the output function, this gives the impl types for setting up FunctionConfig
  Set<Class<?>> getFunctionImplementations(Class<?> functionInterface);
}

/*
sequence of events
  user chooses output for type -> function interface type. getOutputs
  Tree is built for function using defaults. getFunctionType
  user can choose a different function impl at any point in the tree -> FunctionConfig update, tree rebuilt. getFunctionImplementations
*/
