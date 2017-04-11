# Materials for the hands-on portion of the NJ Data Science Apache Spark meetup session.

## Getting ready for the workshop

You will need to install Apache Spark and some packages on your laptop. There is two options to do that: using Docker or local install.

### Install Locally (recommended)

Install Apache Spark 2.1.0 on your laptop. Here are some instructions on how to do that:

1) Go to the Spark download page: http://spark.apache.org/downloads.html
select installation from source: http://d3kbcqa49mib13.cloudfront.net/spark-2.1.0.tgz

Download, unpack, and build it with Maven:

```bash
#cd spark-2.1.0/
build/mvn -DskipTests clean package
```

You are going to need to install Maven build tool for that. 
Maven can be downloaded from the web: https://maven.apache.org/download.cgi unpack it, and add to the `PATH`:
```bash
export PATH=<path to your maven>/apache-maven-3.3.9/bin:$PATH
```

2) Update following environmental variables to point to the new Spark location:

```bash
export SPARK_HOME=/home/<your_username>/your_local_spark/spark-2.1.0
export PATH=$SPARK_HOME/sbin:$SPARK_HOME/bin:$PATH
```

these lines should be added to the *.bashrc* file on your laptop, otherwise you would have to export these values each time you log in!

Check-out the git repository with the workshop materials: 

```bash
git clone https://github.com/ASvyatkovskiy/MLMeetup2017
cd MLMeetup2017
```

### Install with Docker (alternative)

[Docker](https://www.docker.com/) is a containerisation engine that makes it much easier to run softwares. It essentially works as a lightweight VM manager, allowing you to spin instances up or down very easily. First step is to [install Docker](https://www.docker.com/community-edition).

Once you have Docker, run the following to download all of the images required to spin up a Spark Notebook:
```bash
docker pull jupyter/all-spark-notebook
```
This will take a little while, but once it's complete you are basically ready to go. Clone this repo, switch to the Spring2017 branch, and run a Docker container with the following commands:
```bash
git clone https://github.com/ASvyatkovskiy/MLMeetup2017 && cd MLMeetup2017
docker run -it --rm -p 8888:8888 -v $(pwd)/:/home/<your netid>/work jupyter/all-spark-notebook
```
Enter the URL that pops up in your terminal into a browser, and you should be good to go.


## Start interactive jupyter notebook

```bash
jupyter notebook
```
and proceed to complete each of the pre-exercises one by one.


## Additional installations

First, ensure you got the right version of jupyter and Java by typing: 

```bash
jupyter --version
``` 
it should print a value >= 4.0, and 

```bash
java -version
```
it should print a value like "1.8.0". If have version 1.7 on your laptops then go to http://download.oracle.com website and download it. Here is the direct link for Mac users: http://download.oracle.com/otn-pub/java/jdk/8u101-b13/jdk-8u101-macosx-x64.dmg

Finally, check the SPARK_HOME environmental variable is set:

```bash
echo $SPARK_HOME
```
(should return a non empty string on the screen)

### Install Apache Toree

If all the prerequisites are in order, proceed to install Apache Toree - it provides a Scala Spark kernel for Jupyter.

```bash
#cd MLMeetup2017
pip install --user Toree/toree-0.2.0.dev1.tar.gz
```

Configure Apache Toree installation with Jupyter:
```bash
jupyter toree install --spark_home=$SPARK_HOME
```
If you get an error message like: `jupyter: 'toree' is not a Jupyter command`, then you would need to set:
```bash
export PATH=~/.local/bin:$PATH
```
and re-issue the command.

Confirm installation:
```bash
jupyter kernelspec list
```
You should see something like:
```bash
Available kernels:
  python2               /Users/alexey/anaconda/envs/.../lib/python2.7/site-packages/ipykernel/resources
  apache_toree_scala    /usr/local/share/jupyter/kernels/apache_toree_scala
```

Next launch of `jupyter notebook` will give you an option to choose Apache Toree kernel from the upper right menu, which supports Scala and Spark.

Finally, add some external dependencies to Apache Toree:
```bash
echo spark.jars.packages=org.diana-hep:histogrammar_2.11:1.0.4,org.diana-hep:histogrammar-sparksql_2.11:1.0.4,org.diana-hep:histogrammar-bokeh_2.11:1.0.4 | sudo tee -a $SPARK_HOME/conf/spark-defaults.conf
```
