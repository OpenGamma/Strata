package com.opengamma.tools.gradle.distrepo.task

import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Upload

class DeployLocal extends Upload
{
    @OutputDirectory
    File deployIntoDir
}
