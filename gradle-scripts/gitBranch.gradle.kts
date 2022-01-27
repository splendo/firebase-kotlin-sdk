/*
 Copyright 2021 Splendo Consulting B.V. The Netherlands

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */
import org.codehaus.groovy.runtime.ProcessGroovyMethods

val BITRISE_GIT_BRANCH = System.getenv("BITRISE_GIT_BRANCH")
val branchFromGit = run {
    try {
        ProcessGroovyMethods.getText(ProcessGroovyMethods.execute("git rev-parse --abbrev-ref HEAD"))
    } catch (e: Exception) {
        logger.info("Unable to determine current branch through git CLI: ${e.message}")
        "unknown"
    }
}

val branch = (BITRISE_GIT_BRANCH ?: branchFromGit).replace('/', '-').trim().toLowerCase().also {
        if (it == "head")
            logger.warn("Unable to determine current branch: Project is checked out with detached head!")
    }

val branchPostfix = when(branch) {
    "master", "main" -> ""
    "develop" -> "-SNAPSHOT"
    else -> "-$branch-SNAPSHOT"
}

logger.lifecycle("decided branch: '$branch' to postfix '$branchPostfix' (from: BITRISE_GIT_BRANCH env: $BITRISE_GIT_BRANCH, git cli: $branchFromGit)")

extra["branch_postfix"] = branchPostfix
