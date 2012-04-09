/*
Copyright 2012 Dominic Tootell

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

package org.greencheek.gc.memusage.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Create a transformer
 *
 */
public class TrackGCMemoryUsageAnnotationAgent implements ClassFileTransformer
{
    // transformer interface implementation
    public byte[] transform(ClassLoader loader, String cname, Class clazz,
        ProtectionDomain domain, byte[] bytes)
        throws IllegalClassFormatException {
        try {
            
            // scan class binary format to find fields for toString() method
            ClassReader creader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(creader,ClassWriter.COMPUTE_MAXS);
            cname = cname.replace('.', '/');
            RecordGCMemUsageAnnotationCollector visitor = new RecordGCMemUsageAnnotationCollector(cname);
            creader.accept(visitor, ClassReader.SKIP_CODE);
            Map<String,MethodInfo> overrides = visitor.getMethods();
			if (overrides.size() > 0) {
				System.out.println("Annotation found, Modifying Class :" + cname);
				System.out.println();
				System.out.flush();
				MonitoringAspectGenerator gen = new MonitoringAspectGenerator(
						writer, overrides, visitor.hasCinit());
				creader.accept(gen, 0);
				System.out.println("");
				System.out.flush();
				return writer.toByteArray();

			}
        } catch (IllegalStateException e) {
            throw new IllegalClassFormatException("Error: " + e.getMessage() +
                " on class " + cname);
        }
        return null;
    }
    
    // Required method for instrumentation agent.
    public static void premain(String arglist, Instrumentation inst) {
        inst.addTransformer(new TrackGCMemoryUsageAnnotationAgent());
    }
}