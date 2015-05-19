package com.opengamma.tools.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional

/**
 * Created by poundera on 11/05/15.
 */
class RunApp extends DefaultTask
{
    @Input @Optional
    String applicationName

    @Input
    String mainClass


}
