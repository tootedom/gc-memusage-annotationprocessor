GC MemUsage Annotation and Agent.
---------------------------------

This class (annotation), and associated agent, is to be used in conjunction 
with the twitter gcprof (https://github.com/tootedom/jvmgcprof).
The gcprof application can tell you the amount of ram that has been allocated 
between subsequent calls to a method; via watching an incrementing counter.  
For example, one use case would be to see how much ram is used to service a REST request
on a servlet.  In order to monitor such a request, you need to change your code to add something
like the following:

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

You would then run the gcprof application to watch the "requests" static variable.  

Having the above counter in a request processing method is good; and is a simple 
monitoring counter that you could use and leave in your application, to monitor the number
of requests a REST endpoint has received.  However, you might have another library you use
that does this for you, and introducing another counter is extra code.  Also; you might just 
want to perform this profiling operation in development; and not proliferate your code base 
with dev code; that you don't want in production.

Therefore, this app provides an annotation:

```java
	@RecordGCMemUsage(fieldName = "noOfFiveMBClassRequests")
	@RequestMapping(value = "/5mb", method = RequestMethod.GET)
	public ModelAndView FiveMB(HttpServletRequest request,HttpServletResponse resp)
	{
		byte[] fivemb = new byte[(1024*1024)*5];
		return null;
	}
```

and a -javaagent.  The javaagent is a java.lang.instrument.ClassFileTransformer that notices the 
above annotation, and modifies the bytecode of the class to introduce a AtomicLong and an increment of the
AtomicLong at the beginning of the method, on which the annotation is present:

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
that can be used in conjunction with the gc-memusage-agent to indicate the you wish to 
monitor the gc usage of every subsequent calls to that method.  The annotation does not modify the java
class.  Only when the -javagent is used does the class, and method, on which the annotation is declare 
change at runtime via bytecode weaving.  The name of the static variable created is taken from the 
fieldName attribute of the annotation.

The presence of the annotation does not do anything unless the -javaagent is present.  The java agent looks for the
@RecordGCMemUsage annotation and changes the bytecode of the class to introduce a static java.util.concurrent.AtomicLong 
and add an increment of the AtomicLong on Entry to the method on which the annotation is present. 

The annotation is found in the following package:

```xml
		<dependency>
			<groupId>org.greencheek</groupId>
  			<artifactId>gc-memusage-annotation</artifactId>
  			<version>0.0.1</version>
		</dependency> 
```

The javaagent is found in the following package:

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

	if [ "x$OS" == "linux" ]; then
        export LD_LIBRARY_PATH="$gclibdir:$LD_LIBRARY_PATH"
	fi

	#This is the name of the static variable created by @RecordGCMemUsage(fieldName = "noOfFiveMBClassRequests")
	export COUNTER="bbc.forge.domt.hello.web.HelloController:noOfFiveMBClassRequests"
	export ASPECT_AGENT="/Users/dominictootell/Downloads/gc-memusage-agent-0.0.1-relocated-shade.jar"

	export JAVA_OPTS="-Xbootclasspath/a:${gclibdir} -agentlib:gcprof -Dgcprof.period=1 -Dgcprof.nwork=${COUNTER} -javaagent:${ASPECT_AGENT} ${JAVA_OPTS}"
```