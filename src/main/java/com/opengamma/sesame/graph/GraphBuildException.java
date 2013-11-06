/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Lists;
import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.sesame.function.Parameter;

/**
 *
 */
/* package */ class GraphBuildException extends OpenGammaRuntimeException {

  /* package */ GraphBuildException(String message) {
    this(Collections.<Parameter>emptyList(), message);
  }

  /* package */ GraphBuildException(List<Parameter> path, String message) {
    super(message + pathString(path));
  }

  private static String pathString(List<Parameter> path) {
    List<PathElement> pathElements = Lists.newArrayList();
    for (Parameter parameter : path) {
      pathElements.add(new PathElement(parameter.getDeclaringClass().getSimpleName(), parameter.getName()));
    }
    if (pathElements.isEmpty()) {
      return "";
    } else {
      return ". path: " + StringUtils.join(pathElements, " / ");
    }
  }

  private static class PathElement {
    private final String _simpleClassName;
    private final String _parameterName;

    private PathElement(String simpleClassName, String parameterName) {
      _simpleClassName = simpleClassName;
      _parameterName = parameterName;
    }

    @Override
    public String toString() {
      return _simpleClassName + "(" + _parameterName + ")";
    }
  }
}
