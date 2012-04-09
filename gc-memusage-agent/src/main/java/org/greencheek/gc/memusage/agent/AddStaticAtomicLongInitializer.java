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

import java.util.Collection;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class AddStaticAtomicLongInitializer extends MethodVisitor
{
	private final Collection<MethodInfo> annotatedMeasurableMethods; 
	
	public AddStaticAtomicLongInitializer(int api, MethodVisitor cw, Collection<MethodInfo> methods) {
		super(api,cw);
		annotatedMeasurableMethods = methods;// TODO Auto-generated constructor stub
	}
	
	@Override
	public void visitCode() {
		super.visitCode();
		// build my static initializer by calling
		// visitFieldInsn(int opcode, String owner, String name, String desc)
		// or the
		for(MethodInfo method : annotatedMeasurableMethods) {
			System.out.println("Adding static initialiser for: " +  method.getAnnotatedClassName() +"."+ method.getFieldName());
			mv.visitTypeInsn(Opcodes.NEW, "java/util/concurrent/atomic/AtomicLong");
			mv.visitInsn(Opcodes.DUP);
			mv.visitInsn(Opcodes.LCONST_0);
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/concurrent/atomic/AtomicLong", "<init>", "(J)V");
			mv.visitFieldInsn(Opcodes.PUTSTATIC, method.getAnnotatedClassName(), method.getFieldName(), "Ljava/util/concurrent/atomic/AtomicLong;");
		}
		System.out.println();
	}

	
}
