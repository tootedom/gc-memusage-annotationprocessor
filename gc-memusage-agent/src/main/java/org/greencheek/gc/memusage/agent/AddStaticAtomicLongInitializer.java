package org.greencheek.gc.memusage.agent;

import java.util.Collection;
import java.util.List;


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
