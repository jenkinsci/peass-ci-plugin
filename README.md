Peass-CI
===================

The Peass-CI-Plugin enables a continuous performance measurement for Java projects in an Jenkins server. Peass-CI currently supports the following workload types:
- JUnit tests (which are transformed into performance unit tests)
- JMH benchmarks.

Currently, JUnit tests can be measured for maven and Gradle projects and JMH benchmarks only for maven projects.

By integrating Peass-CI in your build process, you will get performance measurements of each JUnit test or JMH benchmark and hints when regressions have occured. Furthermore, Peass-CI creates a call tree of the unit test or benchmark, which pinpoints the root cause of your performance changes. Therefore, the following steps are executed:
- Regression Test Selection: The unit tests which may have changed performance based on the current commit will be selected by a combination of static and dynamic code analysis.
- Performance Measurement: The selected tests will be executed (repeating them inside a VM and starting the JVM, as often as you specify it) to identify performance changes.
- Root Cause Analysis: For every identified performance change, the measurement will be repeated with additional instrumentation of your call tree to identify the method call(s) which cause your performance change (optional).

# Usage

## Configuration
After installing Peass-CI in your Jenkins, you'll have the measurement step available in your build process. 

If you are using pipeline jobs, you may add a performance measurement step like this:

```
stage('measure performance') {
   steps {
      measure VMs: 100, iterations: 10, warmup: 10, repetitions: 1000
   }
}
```

After you added this stage, each build will contain performance measurements (if a code that is called by a unit test or benchmark is changed - there will be no measurements if only documentation changes).  See the [Wiki entry for measurement process configuration](https://github.com/DaGeRe/peass/wiki/Configuration-of-Measurement-Processes) for starting points for configuring the measurement step for your project.

## Example

After successfull experiment execution, you'll get an overview over performance measurements (and especially the detected changes) like this:
![Overview over Performance Measurements](graphs/demo1.png)

For every change, you get a call tree:
![Example Call Tree](graphs/demo2.png)

And in the call tree, you can view the measurements for individual call tree nodes and the source change of these nodes:
![Example Source Diff](graphs/demo3.png)

# Known Problems
- Peass only works if you use the latest version of JUnit, i.e. 4.13.x or 5.8.x, or JMH, i.e. 1.33. If you import an older version of JUnit (or it is imported by plugins you use, e.g. spring boot), please update your JUnit dependency. It is currently not possible to maintain and check the compatibility with older versions of the build tools. 

# Development

Building and updating to the latest Peass version from git is only required if you need the latest changes, e.g. if you want to change something yourself or you want to check whether a bug has been fixed. Otherwise, just use the release.

## Building

Peass-CI relies on the Peass-libraries. To build them, get peass by running `git clone https://github.com/DaGeRe/peass.git && cd peass && mvn clean install -DskipTests -P buildStarter` (to build the full Peass project, and not only the basic libraries, the profile `buildStarter` needs to be built). Then, execute `mvn clean package`.

For testing, run `mvn hpi:run` and access `localhost:8080/jenkins`. 

## Development Version Running

For installing latest Peass-CI to your Jenkins installation, you may either upload it through the website (Manage Jenkins -> Manage Plugins -> Advanced -> Upload Plugin) or stop Jenkins, copy `target/peass-ci.hpi` to `~/.jenkins/plugins` (or wherever your Jenkins home is) and restart Jenkins. Afterwards, when configuring your project, the `measure`-step is available. 

# License

Peass-CI is **licensed** under the **[MIT License]** and **[AGPL License]**.

[MIT License]: https://github.com/DaGeRe/peass-ci/blob/main/LICSENSE.MIT
[AGPL License]: https://github.com/DaGeRe/peass-ci/blob/main/LICENSE.AGPL
