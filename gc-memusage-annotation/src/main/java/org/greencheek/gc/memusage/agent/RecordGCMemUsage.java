package org.greencheek.gc.memusage.agent;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that is to be used in conjunction with the GC Memory
 * Usage Agent.  The annotation signals to the Agent to generate an
 * AtomicLong static field and insert into the method with the annotation
 * an increment on the atomic long.
 * 
 * The long is used as a mechanism on which the gc memory profile can
 * calculate the usage in between invocations (incrementations) of the counter
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
	 * @return The field name that will be generated.  If you do not provide a field name
	 */
	String fieldName();
}
