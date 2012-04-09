GC MemUsage Annotation and Agent.
---------------------------------

A method annotation, and associated agent, to be used in conjunction 
with the twitter gcprof (https://github.com/tootedom/jvmgcprof).
The gcprof application can tell you the amount of ram that has been allocated 
between subsequent calls to a method; via watching an incrementing counter.  
For example, one use case would be to see how much ram is used to service a 
single REST request on a servlet.  In order to monitor such a request, 
you would need to change your code to add something like the following:

```java
	public final static AtomicInteger requests = new AtomicInteger(0);
        ....
        ....
        ....
	@RequestMapping(value = "/5mb", method = RequestMethod.GET)
	public ModelAndView FiveMB(HttpServletRequest request,HttpServletResponse resp)
	{
		requests.incrementAndGet();
		byte[] fivemb = new byte[(1024*1024)*5];
		return null;
	}
```

You would then run the gcprof application to watch the "<b>requests</b>" static variable.  

Having the above counter in a request processing method is good; and simple way to provide a
monitoring counter that you could use and leave in your application as a means to monitor 
the number of requests a REST endpoint has received.  However, you might have another library 
that you are use that does this for you, registering the monitoring value with your monitoring 
system; etc, and introducing another counter is just extra code.  

You might also just wish to perform this gc profiling operation in development; and not 
have to proliferate your code base with development code that updates counter increments.
In other words you don't want the code active in production.  Within a C application this would
be akin to preprocessor code that is removed at compile time when building for production.

Therefore, this project provides a simple Method Level annotation as follows:

```java
	@RecordGCMemUsage(fieldName = "ANY_VALID_JAVA_VARNAME")
```

For example:

```java
    @RecordGCMemUsage(fieldName = "noOfFiveMBClassRequests")
	@RequestMapping(value = "/5mb", method = RequestMethod.GET)
	public ModelAndView FiveMB(HttpServletRequest request,HttpServletResponse resp)
	{
		byte[] fivemb = new byte[(1024*1024)*5];
		return null;
	}
```

and a <b>-javaagent</b> is provided that used that @RecordGCMemUsage annotation.  
The javaagent is a java.lang.instrument.ClassFileTransformer that notices the 
above annotation, and modifies the bytecode of the class at run time when the class is loaded.
The agent introduces aa AtomicLong and an increment of that AtomicLong at the beginning of the
method, on which the annotation is present.  For Example, in a code sense it would look something
like, it you had written the code yourself:

```java
	public static final AtomicLong noOfFiveMBClassRequests = new AtomicLong(0)
   
	@RequestMapping(value = "/5mb", method = RequestMethod.GET)
	public ModelAndView FiveMB(HttpServletRequest request,HttpServletResponse resp)
	{
		noOfFiveMBClassRequests.incrementAndGet();
		byte[] fivemb = new byte[(1024*1024)*5];
		return null;
	}
```
 
Without the javaagent, the client code is just an annotation that has no use other than documents that
in development you used it to monitor gc use.  In other works it is a Simple java method annotation 
that can be used in conjunction with the <b>gc-memusage-agent</b> to indicate the you wish to 
monitor the gc usage of every subsequent calls to that method.  The annotation does not modify the java
class; and therefore has no performance impact on production code.  It is only when the -javagent is 
used does the class, and method on which the annotation is declared, change at runtime via bytecode weaving;
to introduce the static increment per method invocation.  

The name of the static variable created is taken from the <b>fieldName</b> attribute of the annotation.

The presence of the annotation does not do anything unless the -javaagent is present.  The java agent looks for the
@RecordGCMemUsage annotation and changes the bytecode of the class to introduce a static java.util.concurrent.AtomicLong 
and add an increment of the AtomicLong on Entry to the method on which the annotation is present. 

The annotation is found in the following package.  It is only this dependency that you need to include in your
application.  The agent is NOT needed by your web application; you just need the annotation:

```xml
		<dependency>
			<groupId>org.greencheek</groupId>
  			<artifactId>gc-memusage-annotation</artifactId>
  			<version>0.0.1</version>
		</dependency> 
```

The javaagent is found in the following package.  This is not required by your application code:

```xml
		<dependency>
			<groupId>org.greencheek</groupId>
  			<artifactId>gc-memusage-agent</artifactId>
  			<version>0.0.1</version>
  			<classifier>relocated-shade</classifier>
		</dependency> 
```

The javaagent use ASM (http://asm.ow2.org/) to preform the ByteCode weaving and relocates the packages from
org.objectweb.asm to org.greencheek.asm; as to not conflict with any ASM library running in your application

The libraries are available in the mvn repo at:

	https://raw.github.com/tootedom/tootedom-mvn-repo/master/releases/
	https://raw.github.com/tootedom/tootedom-mvn-repo/master/snapshots/

For example:

	https://raw.github.com/tootedom/tootedom-mvn-repo/master/releases/org/greencheek/gc-memusage-agent/0.0.1/gc-memusage-agent-0.0.1-relocated-shade.jar
	
## Install
	
In order for the gc profiler to be of any use you need to download the gcprof application and build it https://github.com/tootedom/jvmgcprof:

	make clean install
	
The Makefile assumes some default locations for the Java JDK Install location.  The sdk is needed as the 
gcprof uses a native instrumentation to monitor the memory usage, and as a result needs to compile against the 
headers of the Java install.  The default install location is /tmp/gcusage  The install will produce the following

```
	$ ls -l /tmp/gcusage/
	total 152
	-rwxr-xr-x  1 dominictootell  wheel   2139  8 Apr 16:30 GcProf$1.class
	-rwxr-xr-x  1 dominictootell  wheel   5712  8 Apr 16:30 GcProf.class
	-rwxr-xr-x  1 dominictootell  wheel    599  8 Apr 16:30 gcprof
	-rwxr-xr-x  1 dominictootell  wheel  54048  8 Apr 16:30 libgcprof.jnilib
	lrwxr-xr-x  1 dominictootell  wheel     29  8 Apr 16:30 libgcprof.so -> /tmp/gcusage/libgcprof.jnilib
```

With the gcprof agent from twitter, install all you need to go is download the agent, and configure your jvm to
run with the gcprof and the -javaagent.   The modified version of the gcprof at: https://github.com/tootedom/jvmgcprof has
changes that allow it to work with webapp container that has multiple classloaders.  This agent does not terminate
if it cannot find the required static AtomicLong in the System Classloader; and waits for the AtomicLong to be created
by any classloader.

With the gcprof installed in /tmp/gcusage, all you need is to download the agent 

  https://raw.github.com/tootedom/tootedom-mvn-repo/master/releases/org/greencheek/gc-memusage-agent/0.0.1/gc-memusage-agent-0.0.1-relocated-shade.jar

and modify your jvm to startup with the correct parameters.  I.e. for tomcat, create a bin/setenv.sh like as follows

```
	gclibdir=/tmp/gcusage

	OS=$(uname -s | tr '[A-Z]' '[a-z]')
	echo $OS
	if [ "x$OS" == "xdarwin" ]; then
        export DYLD_LIBRARY_PATH="$gclibdir:$DYLD_LIBRARY_PATH"
	fi

	if [ "x$OS" == "xlinux" ]; then
        export LD_LIBRARY_PATH="$gclibdir:$LD_LIBRARY_PATH"
	fi

	#This is the name of the static variable created by @RecordGCMemUsage(fieldName = "noOfFiveMBClassRequests")
	export COUNTER="org.greencheek.domt.hello.web.HelloController:noOfFiveMBClassRequests"
	export ASPECT_AGENT="/Users/dominictootell/Downloads/gc-memusage-agent-0.0.1-relocated-shade.jar"

	export JAVA_OPTS="-Xbootclasspath/a:${gclibdir} -agentlib:gcprof -Dgcprof.period=1 -Dgcprof.nwork=${COUNTER} -javaagent:${ASPECT_AGENT} ${JAVA_OPTS}"
```

The above example is working on the fact that you have a Class named <b>org.greencheek.domt.hello.web.HelloController</b>
and that you annotated a method in that controller with 

```java
	@RecordGCMemUsage(fieldName = "noOfFiveMBClassRequests")
```


When the class is being loaded, the agent will output to stdout, that is it annotating any found classes:

```
	Annotation found, Modifying Class :org/greencheek/domt/hello/web/HelloController

	Aspecting: org/greencheek/domt/hello/web/HelloController.ClassFicMB(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)Lorg/springframework/web/servlet/ModelAndView;nullnull
	Increments: noOfFiveMBClassRequests
	Adding static initialiser for: org/greencheek/domt/hello/web/HelloController.noOfFiveMBClassRequests

	Adding: public final static AtomicLong noOfFiveMBClassRequests
```

When you start hitting the application with requests, the twitter gcprof will output to stdout the amount of ram that
is being consumed/allocated per subsequent increment of the counter (i.e. the amount of ram that has been incremented
between counter increments)

```
	85MB w=17 (0MB/s 5136kB/w)
	50.00%	6	1
	90.00%	9	1
	95.00%	10	2
	99.00%	10	2
	99.90%	25	5
	99.99%	35	7
``` 