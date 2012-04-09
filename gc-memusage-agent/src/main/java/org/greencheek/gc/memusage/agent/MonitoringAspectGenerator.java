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

import java.util.Map;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Visitor to add <code>toString</code> method to a class.
 */
public class MonitoringAspectGenerator extends ClassVisitor
{
    private final Map<String,MethodInfo> overrideMethodNames;
    private final boolean hasCinit;
    
    public MonitoringAspectGenerator(ClassWriter cw, Map<String,MethodInfo> props, boolean hasCinit) {
        super(Opcodes.ASM4,cw);
        overrideMethodNames = props;
        this.hasCinit = hasCinit;
    }
    
    /**
     * Visits a method of the class. This method <i>must</i> return a new
     * {@link MethodVisitor} instance (or <tt>null</tt>) each time it is
     * called, i.e., it should not return a previously returned visitor.
     * 
     * This is overriden to:
     * <ol>
     * <li>statically initialising the AtomicLong if a static initialiser method is present in the class</li>
     * <li>Add to the method on with the @RecordGCMemUsage was present, a piece of code that updates the atomic long</li>
     * </ol>
     *
     * @param access the method's access flags (see {@link Opcodes}). This
     *        parameter also indicates if the method is synthetic and/or
     *        deprecated.
     * @param name the method's name.
     * @param desc the method's descriptor (see {@link Type Type}).
     * @param signature the method's signature. May be <tt>null</tt> if the
     *        method parameters, return type and exceptions do not use generic
     *        types.
     * @param exceptions the internal names of the method's exception classes
     *        (see {@link Type#getInternalName() getInternalName}). May be
     *        <tt>null</tt>.
     * @return an object to visit the byte code of the method, or <tt>null</tt>
     *         if this class visitor is not interested in visiting the code of
     *         this method.
     */
    public MethodVisitor visitMethod(
        final int access,
        final String name,
        final String desc,
        final String signature,
        final String[] exceptions)
    {
    	
		MethodVisitor r = super.visitMethod(access, name, desc, signature,
				exceptions);
		
		if ("<clinit>".equals(name)) {
			r = new AddStaticAtomicLongInitializer(Opcodes.ASM4, r,
					overrideMethodNames.values());
		}
		
		
		MethodInfo currentMethod = new MethodInfo(access,name,desc,signature,exceptions,null,null);
		if(overrideMethodNames.containsKey(currentMethod.toString())) {			
			final MethodInfo info = overrideMethodNames.get(currentMethod.toString());
			
			r = new AdviceAdapter(Opcodes.ASM4,r,access,name,desc) {
				protected void onMethodEnter() {
					System.out.println("Aspecting: " + info.getAnnotatedClassName()+ "."+info.toString());
					System.out.println("Increments: " + info.getFieldName());
					super.visitFieldInsn(GETSTATIC, info.getAnnotatedClassName(), info.getFieldName(), "Ljava/util/concurrent/atomic/AtomicLong;");
					super.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicLong", "incrementAndGet", "()J");
					super.visitInsn(POP2);	    
				}		
			};	
		}		
			
		return r;
    	
    }
    
    
    /**
     * Visits a field of the class.
     * The method is overriden to perform the functions of:
     * <ol>
     * <li>Adding a static AtomicLong to the class</li>
     * <li>statically initialising the AtomicLong if a static initialiser isn't already present</li>
     * </ol>
     *
     * @param access the field's access flags (see {@link Opcodes}). This
     *        parameter also indicates if the field is synthetic and/or
     *        deprecated.
     * @param name the field's name.
     * @param desc the field's descriptor (see {@link Type Type}).
     * @param signature the field's signature. May be <tt>null</tt> if the
     *        field's type does not use generic types.
     * @param value the field's initial value. This parameter, which may be
     *        <tt>null</tt> if the field does not have an initial value, must
     *        be an {@link Integer}, a {@link Float}, a {@link Long}, a
     *        {@link Double} or a {@link String} (for <tt>int</tt>,
     *        <tt>float</tt>, <tt>long</tt> or <tt>String</tt> fields
     *        respectively). <i>This parameter is only used for static fields</i>.
     *        Its value is ignored for non static fields, which must be
     *        initialized through bytecode instructions in constructors or
     *        methods.
     * @return a visitor to visit field annotations and attributes, or
     *         <tt>null</tt> if this class visitor is not interested in
     *         visiting these annotations and attributes.
     */
	public void visitEnd() {
		for (MethodInfo method : overrideMethodNames.values()) {
			System.out.println("Adding: public final static AtomicLong " + method.getFieldName());
			FieldVisitor fv = cv.visitField(Opcodes.ACC_PUBLIC
					+ Opcodes.ACC_FINAL + Opcodes.ACC_STATIC,
					method.getFieldName(),
					"Ljava/util/concurrent/atomic/AtomicLong;", null, null);
			fv.visitEnd();
		}

		if (!hasCinit) {
			MethodVisitor mv = cv.visitMethod(Opcodes.ACC_STATIC, "<clinit>",
					"()V", null, null);
			mv.visitCode();
			AddStaticAtomicLongInitializer initialiser = new AddStaticAtomicLongInitializer(
					Opcodes.ASM4, mv, overrideMethodNames.values());
			initialiser.visitCode();
			mv.visitInsn(Opcodes.RETURN);
			mv.visitMaxs(0, 0);
			mv.visitEnd();
		}

		super.visitEnd();

	}
    
  

}