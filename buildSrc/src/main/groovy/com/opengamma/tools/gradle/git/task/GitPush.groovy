/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.git.task

import com.opengamma.tools.gradle.simpleexec.SimpleExec
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

class GitPush extends BaseGitTask
{
    @Input
    File repositoryLocation

    @Input @Optional
    boolean all = false

    @Input @Optional
    boolean tags = false

    @Input @Optional
    String upstream

    @TaskAction
    void doPush()
    {
        List<String> gitCommand = ["git", "push"]
        if(all)
            gitCommand << "--all"
        if(tags)
            gitCommand << "--tags"
        if(upstream)
            gitCommand.addAll(["--set-upstream", upstream])

        Task doPush = project.tasks.create("execGitPush", SimpleExec)
        doPush.configure {
            command gitCommand
            workingDirectory repositoryLocation
        }

        doPush.execute()
    }
}
