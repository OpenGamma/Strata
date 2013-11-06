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
    if (path.isEmpty()) {
      return "";
    } else {
      List<String> pathElements = Lists.newArrayListWithCapacity(path.size());
      for (Parameter parameter : path) {
        pathElements.add("\t\t" + parameter.getDeclaringClass().getSimpleName() + "(" + parameter.getName() + ": " +
                             parameter.getType().getSimpleName() + ")");
      }
      return "\n\n\tpath:\n" + StringUtils.join(pathElements, "\n") + "\n";
    }
  }
}
