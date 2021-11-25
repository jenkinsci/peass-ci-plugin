Peass-CI
===================

The Peass-CI-Plugin enables a continuous performance measurement for Java projects in an Jenkins server. By integrating Peass-CI in your build process, you will get performance measurements of each unit test and a measurement of call tree nodes, which pinpoints the root cause of your performance changes. Therefore, the following steps are executed:
- Regression Test Selection: The unit tests which may have changed performance based on the current commit will be selected by a combination of static and dynamic code analysis.
- Performance Measurement: The selected tests will be executed (repeating them inside a VM and starting the JVM, as often as you specify it) to identify performance changes.
- Root Cause Analysis: For every identified performance change, the measurement will be repeated with additional instrumentation of your call tree to identify the method call(s) which cause your performance change (optional).

Peass-CI is still under development. Please file issues if problems occur.

# Building

First get peass by running `git clone https://github.com/DaGeRe/peass.git && cd peass && mvn clean install -DskipTests -P buildStarter` (to build the full Peass project, and not only the basic libraries, the profile `buildStarter` needs to be built). Then, execute `mvn clean package`.

# Running

For installing Peass-CI to your Jenkins installation, you may either upload it through the website (Manage Jenkins -> Manage Plugins -> Advanced -> Upload Plugin) or stop Jenkins, copy `target/peass-ci.hpi` to `~/.jenkins/plugins` (or wherever your Jenkins home is) and restart Jenkins. Afterwards, when configuring your project, the `Measure Version Performance` step is available. Peass-CI is currently not available in the plugin repository.

If you want to include Peass-CI in your Jenkins Pipeline, you may configure it like this:

```groovy
pipeline {
    agent any
    stages {
        stage('build') {
            steps {
                sh 'mvn clean package'
            }
        }
        stage('measurement') {
            steps {
                measure VMs: 30, iterations: 10, warmup: 10, repetitions: 100000
            }
        }
    }
}
```
See the [Wiki entry for measurement process configuration](https://github.com/DaGeRe/peass/wiki/Configuration-of-Measurement-Processes) for starting points for configuring the measurement step for your project.

For testing, run `mvn hpi:run` and access `localhost:8080/jenkins`. 

# Example

After successfull experiment execution, you'll get an overview over performance measurements (and especially the detected changes) like this:
![Overview over Performance Measurements](graphs/demo1.png)

For every change, you get a call tree:
![Example Call Tree](graphs/demo2.png)

And in the call tree, you can view the measurements for individual call tree nodes and the source change of these nodes:
![Example Source Diff](graphs/demo3.png)

# Known Problems
- Peass only works if you use the latest version of JUnit, i.e. 4.13.x or 5.8.x. If you import an older version of JUnit (or it is imported by plugins you use, e.g. spring boot), please update your JUnit dependency.

# License

Peass-CI is **licensed** under the **[MIT License]** and **[AGPL License]**.

[MIT License]: https://github.com/DaGeRe/peass-ci/blob/main/LICSENSE.MIT
[AGPL License]: https://github.com/DaGeRe/peass-ci/blob/main/LICENSE.AGPL
