package com.opengamma.tools.gradle.release

import groovy.transform.Memoized

trait TaskNamer
{
    String taskNameFor(String baseName)
    {
        return "${baseName}${this.project.name.capitalize()}"
    }
}
