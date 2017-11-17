# Apache Spark on Nomad

This repository is a fork of Apache Spark that natively supports using HashiCorp's
[Nomad](https://www.nomadproject.io/intro/) as Spark's cluster manager  (as an
alternative to Hadoop YARN and Mesos). When running on Nomad, the Spark executors
 that run tasks for your Spark application, and optionally the application
 driver itself, run as Nomad tasks in a Nomad job.

Sample `spark-submit` command when using Nomad:

```bash
spark-submit \
  --class org.apache.spark.examples.JavaSparkPi \
  --master nomad \
  --deploy-mode cluster \
  --conf spark.executor.instances=4 \
  --conf spark.nomad.sparkDistribution=https://s3.amazonaws.com/nomad-spark/spark-2.1.0-bin-nomad.tgz \
  https://s3.amazonaws.com/nomad-spark/spark-examples_2.11-2.1.0-SNAPSHOT.jar 100
 ```

The ultimate goal is to integrate Nomad into Spark directly, either natively or
via a backend/scheduler plugin interface.

## Benefits of Spark on Nomad

Nomad's design is heavily inspired by Google's work on both
[Borg](https://research.google.com/pubs/pub43438.html) and
[Omega](https://research.google.com/pubs/pub41684.html). This has enabled a set
of features that make Nomad well-suited to run analytical applications.
Particularly relevant are its native support for
[batch workloads](https://www.nomadproject.io/docs/runtime/schedulers.html#batch)
 and parallelized, [high throughput scheduling](https://www.hashicorp.com/c1m/)
 (more on scheduler internals [here](https://www.nomadproject.io/docs/internals/scheduling.html)).

Nomad is easy to set up and use. It consists of a single binary/process, has a
simple and intuitive data model, utilizes a
[declarative job specification](https://www.nomadproject.io/docs/job-specification/index.html)
 and supports high availability and
[multi-datacenter federation](https://www.nomadproject.io/guides/cluster/federation.html)
 out-of-the-box. Nomad also integrates seamlessly with HashiCorp's other runtime
 tools: [Consul](https://www.nomadproject.io/docs/service-discovery/index.html)
and [Vault](https://www.nomadproject.io/docs/vault-integration/index.html).

## Getting Started

To get started, see Nomad's official
[Apache Spark Integration Guide](https://www.nomadproject.io/guides/spark/spark.html).
 You can also use Nomad's [example Terraform configuration](https://github.com/hashicorp/nomad/tree/master/terraform)
and [embedded Spark quickstart](https://github.com/hashicorp/nomad/tree/master/terraform/examples/spark)
to give the integration a test drive on AWS or Azure.

### Nomad 0.6 builds

- [Spark 2.1.0](https://s3.amazonaws.com/nomad-spark/spark-2.1.0-bin-nomad.tgz)
- [Spark 2.1.1](https://s3.amazonaws.com/nomad-spark/spark-2.1.1-bin-nomad.tgz)
- [Spark 2.2.0](https://s3.amazonaws.com/nomad-spark/spark-2.2.0-bin-nomad.tgz)

### Nomad 0.7 builds

- [Spark 2.1.0](https://s3.amazonaws.com/nomad-spark/spark-2.1.0-bin-nomad-0.7.0.tgz)
- [Spark 2.1.1](https://s3.amazonaws.com/nomad-spark/spark-2.1.1-bin-nomad-0.7.0.tgz)
- [Spark 2.1.2](https://s3.amazonaws.com/nomad-spark/spark-2.1.2-bin-nomad-0.7.0.tgz)
- [Spark 2.2.0](https://s3.amazonaws.com/nomad-spark/spark-2.2.0-bin-nomad-0.7.0.tgz)

### Creating Your Own Build

You can create a Nomad-enabled Spark distribution using Spark's standard `make-distribution.sh` script,
and enabling the `nomad` profile. E.g.:

~~~
./dev/make-distribution.sh --name nomad --tgz -Pnomad -Psparkr -Phive -Phadoop-2.7 -Phive-thriftserver -DskipTests
~~~

---

*The standard Spark README follows below.*

# Apache Spark

Spark is a fast and general cluster computing system for Big Data. It provides
high-level APIs in Scala, Java, Python, and R, and an optimized engine that
supports general computation graphs for data analysis. It also supports a
rich set of higher-level tools including Spark SQL for SQL and DataFrames,
MLlib for machine learning, GraphX for graph processing,
and Spark Streaming for stream processing.

<http://spark.apache.org/>


## Online Documentation

You can find the latest Spark documentation, including a programming
guide, on the [project web page](http://spark.apache.org/documentation.html).
This README file only contains basic setup instructions.

## Building Spark

Spark is built using [Apache Maven](http://maven.apache.org/).
To build Spark and its example programs, run:

    build/mvn -DskipTests clean package

(You do not need to do this if you downloaded a pre-built package.)

You can build Spark using more than one thread by using the -T option with Maven, see ["Parallel builds in Maven 3"](https://cwiki.apache.org/confluence/display/MAVEN/Parallel+builds+in+Maven+3).
More detailed documentation is available from the project site, at
["Building Spark"](http://spark.apache.org/docs/latest/building-spark.html).

For general development tips, including info on developing Spark using an IDE, see ["Useful Developer Tools"](http://spark.apache.org/developer-tools.html).

## Interactive Scala Shell

The easiest way to start using Spark is through the Scala shell:

    ./bin/spark-shell

Try the following command, which should return 1000:

    scala> sc.parallelize(1 to 1000).count()

## Interactive Python Shell

Alternatively, if you prefer Python, you can use the Python shell:

    ./bin/pyspark

And run the following command, which should also return 1000:

    >>> sc.parallelize(range(1000)).count()

## Example Programs

Spark also comes with several sample programs in the `examples` directory.
To run one of them, use `./bin/run-example <class> [params]`. For example:

    ./bin/run-example SparkPi

will run the Pi example locally.

You can set the MASTER environment variable when running examples to submit
examples to a cluster. This can be a mesos:// or spark:// URL,
"yarn" to run on YARN, and "local" to run
locally with one thread, or "local[N]" to run locally with N threads. You
can also use an abbreviated class name if the class is in the `examples`
package. For instance:

    MASTER=spark://host:7077 ./bin/run-example SparkPi

Many of the example programs print usage help if no params are given.

## Running Tests

Testing first requires [building Spark](#building-spark). Once Spark is built, tests
can be run using:

    ./dev/run-tests

Please see the guidance on how to
[run tests for a module, or individual tests](http://spark.apache.org/developer-tools.html#individual-tests).

## A Note About Hadoop Versions

Spark uses the Hadoop core library to talk to HDFS and other Hadoop-supported
storage systems. Because the protocols have changed in different versions of
Hadoop, you must build Spark against the same version that your cluster runs.

Please refer to the build documentation at
["Specifying the Hadoop Version"](http://spark.apache.org/docs/latest/building-spark.html#specifying-the-hadoop-version)
for detailed guidance on building for a particular distribution of Hadoop, including
building for particular Hive and Hive Thriftserver distributions.

## Configuration

Please refer to the [Configuration Guide](http://spark.apache.org/docs/latest/configuration.html)
in the online documentation for an overview on how to configure Spark.

## Contributing

Please review the [Contribution to Spark guide](http://spark.apache.org/contributing.html)
for information on how to get started contributing to the project.
