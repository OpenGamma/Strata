/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

/**
 * TODO where are providers registered?
 * for classes it's easy, the type is enough
 * for 'functions' the function repo knows the impl types for interfaces
 * it would be consistent for it to know about providers too
 * but if providers can provide an arbitrary service it doesn't really belong in the function repo
 * should impl types and providers be moved somewhere else?
 * should the repo only have methods that take or return an output name?
 * i.e. move getFunctionImplementations and getDefaultFunctionImplementation to the same place as providers?
 */
public interface Provider<T> {

  T get();
}
