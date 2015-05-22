package com.opengamma.tools.gradle.release

trait TaskNamer
{
    String taskNameFor(String baseName)
    {
        return "${baseName}${this.project.name.capitalize()}"
    }
}
