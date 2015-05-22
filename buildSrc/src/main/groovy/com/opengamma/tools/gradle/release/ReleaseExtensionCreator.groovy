package com.opengamma.tools.gradle.release

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownDomainObjectException

trait ReleaseExtensionCreator
{
    private Project target

    /**
     * Add the {@link ReleaseExtension} extension to the this Project iif it has not already been added.
     */
    void createReleaseExtension()
    {
        target = (Project) this.project

        if( ! target.extensions.findByType(ReleaseExtension))
            doCreateReleaseExtension()
    }

    private void doCreateReleaseExtension()
    {
        target.extensions.create(ReleasePlugin.RELEASE_EXTENSION_NAME, ReleaseExtension)
    }
}
