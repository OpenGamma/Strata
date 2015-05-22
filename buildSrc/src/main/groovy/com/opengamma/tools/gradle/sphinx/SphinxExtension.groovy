/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.sphinx

class SphinxExtension
{
    /**
     * The name of the archive that the User Guide should be packaged into
     */
	String userGuideArchiveName

    /**
     * OPTIONAL
     * The required Python packages for building the User Guide.
     *
     * Defaults to the OpenGamma Sphinx/RST stack.
     */
    Set<String> requiredPackages =  [
            "numpy", "numpydoc", "rst2pdf", "pygments", "sphinx", "sphinx-rtd-theme"
    ].toSet()
}
