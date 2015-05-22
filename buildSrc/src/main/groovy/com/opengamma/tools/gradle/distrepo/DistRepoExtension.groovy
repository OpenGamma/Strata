package com.opengamma.tools.gradle.distrepo

import org.gradle.api.Project

class DistRepoExtension
{
    /**
     * Relative to the buildDir, the root directory for the filesystem-based Maven repository
     */
    String repoDirectoryName = "m2_dist"

    /**
     * OPTIONAL
     * The project who will collect the deployed dependencies
     *
     * Defaults to the current project, set to root project to produce one repository containing all artefacts
     */
    Project deployInto
}
