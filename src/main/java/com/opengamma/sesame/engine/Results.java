/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.Map;

import com.google.common.collect.Maps;
import com.opengamma.DataNotFoundException;
import com.opengamma.id.ObjectId;
import com.opengamma.sesame.trace.CallGraph;

/**
 * TODO use linked maps so the column and rows orders reflect the view def
 * TODO including the column names in every row is wildly inefficient, use lists and a separate list/map of columns
 */
public class Results {

  private final Map<ObjectId, Map<String, Object>> _results;

  /* package */ Results(Map<ObjectId, Map<String, Object>> results) {
    // TODO copy into immutable map so can be safely shared
    _results = results;
  }

  /* package */ Map<String, Object> getTargetResults(ObjectId targetId) {
    if (!_results.containsKey(targetId)) {
      throw new DataNotFoundException("No results for target ID " + targetId);
    }
    return _results.get(targetId);
  }

  @Override
  public String toString() {
    return _results.toString();
  }

  // TODO this is most definitely *not* thread safe yet
  /* package */ static class Builder {

    private final Map<ObjectId, Map<String, Object>> _results = Maps.newHashMap();

    /* package */ void add(String columnName, ObjectId targetId, Object result, CallGraph callGraph) {
      // TODO add the call graph to the results
      Map<String, Object> targetResults;
      if (_results.containsKey(targetId)) {
        targetResults = _results.get(targetId);
      } else {
        targetResults = Maps.newHashMap();
        _results.put(targetId, targetResults);
      }
      targetResults.put(columnName, result);
    }

    /* package */ Results build() {
      return new Results(_results);
    }
  }
}
