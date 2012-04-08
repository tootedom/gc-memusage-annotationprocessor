package org.greencheek.gc.memusage.agent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Visitor implementation to collect field annotation information from class.
 */
public class RecordGCMemUsageAnnotationCollector extends ClassVisitor
{
	private final Map<String,MethodInfo> matchingMethods = new ConcurrentHashMap<String,MethodInfo>(); 
	private final String className;
	private volatile boolean cinitAvailable = false;
	
	
	public RecordGCMemUsageAnnotationCollector(String className) {
		super(Opcodes.ASM4);
		this.className = className;
	}
	
	public Map<String,MethodInfo> getMethods() {
		return matchingMethods;
	}
	
	public boolean hasCinit() {
		return cinitAvailable;
	}

	/**
	 * Creates the signature used to create the static fields later on.
	 * 
	 * @param access
	 * @param name
	 * @param desc
	 * @param signature
	 * @param exceptions
	 * @return
	 */
	private MethodInfo createMatchingMethod(int access, String name,
										String desc,String signature,
										String[] exceptions, String fieldName,
										String annotatedClassName) {
		MethodInfo info = new MethodInfo(access,name,desc,signature,exceptions,fieldName,annotatedClassName);
		matchingMethods.put(info.toString(),info);
		return info;
		
	}
    
    @Override
    public MethodVisitor visitMethod(
        final int access,
        final String name,
        final String desc,
        final String signature,
        final String[] exceptions)
    {
    	
    	if("<clinit>".equals(name)) {
    		cinitAvailable = true;
    	}
    	
        return new MethodVisitor(Opcodes.ASM4) {
        	
        	
            @Override
            public AnnotationVisitor visitAnnotation(
                final String sig,
                boolean visible)
            {
            	if(sig.equals("Lorg/greencheek/gc/memusage/agent/RecordGCMemUsage;")) {

            		return new AnnotationVisitor(Opcodes.ASM4) {            			            		
	            		private String fieldName = name;		            		
	            		
	            		public void visit(String name, Object value) {
	            			if(name.equals("fieldName"))
	            				fieldName = value.toString();
	            	    }
	            		
	                    @Override
	                    public void visitEnd()	                       
	                    {
	                    	if(sig.equals("Lorg/greencheek/gc/memusage/agent/RecordGCMemUsage;")) {
	                    		createMatchingMethod(access,name,desc,signature,exceptions,fieldName,className);
	                    	}  
	                    	super.visitEnd();
	                    }
	                    
	                    
	            	};
            	} else {
            		return null;
            	}
            }
            
           

        };
    }
}