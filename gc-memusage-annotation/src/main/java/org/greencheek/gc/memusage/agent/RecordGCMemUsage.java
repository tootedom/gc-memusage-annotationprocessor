package org.greencheek.gc.memusage.agent;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that is to be used in conjunction with the GC Memory
 * Usage Agent (https://github.com/tootedom/jvmgcprof).  
 * This annotation is used in conjuction with a custom javaagent;
 * which generates a static AtomicLong, and aspects the methods
 * on which the annotation is present with an increment of the long.
 * 
 * The long can than be subsequently used as a mechanism on which the gc memory profile
 * (https://github.com/tootedom/jvmgcprof) can calculate the memory usage in 
 * in between invocations (incrementations) of the counter.
 * 
 * @author  Dominic Tootell
 * @version %I%, %G%
 * @since 1.5
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RecordGCMemUsage {
	/**
	 * The name of the generated field that is used to count the number of time
	 * the method has been called
	 * 
	 * @return The field name that will be generated.  
	 */
	String fieldName();
}
