package org.greencheek.gc.memusage.agent;

import java.util.Arrays;

public class MethodInfo {
	private final int access;
	private final String name;
	private final String desc;
	private final String signature;
	private final String[] exceptions;
	private final String toString; 
	private final String fieldName;
	private final String annotatedClassName;
	
	public MethodInfo(int access, String name,
					  String desc,String signature,
					  String[] exceptions, String fieldName,
					  String annotatedClassName) {
		this.access = access;
		this.name = name;
		this.desc = desc;
		this.signature = signature;
		if(exceptions!=null) {
			this.exceptions = Arrays.copyOf(exceptions,exceptions.length);
		} else {
			this.exceptions = null;
		}		
		StringBuilder b = new StringBuilder(access);
		b.append(name).append(desc).append(signature).append(Arrays.toString(exceptions));
		toString = b.toString();
		
		this.fieldName = fieldName;
		this.annotatedClassName = annotatedClassName;
	}

	public int getAccess() {
		return access;
	}

	public String getName() {
		return name;
	}

	public String getDesc() {
		return desc;
	}

	public String getSignature() {
		return signature;
	}

	public String[] getExceptions() {
		if(exceptions!=null)
			return Arrays.copyOf(exceptions,exceptions.length);
		else 
			return null;
	}

	public boolean equals(Object obj) {
		if(obj==null) return false;
		if(obj instanceof MethodInfo ){
			String s= ((MethodInfo)obj).toString();
			if(s.equals(this.toString())) { 
				return true;
			} else {
				return false;
			}
			
		} else {
			return false;
		}
	}
	
	public String toString() {
		return toString;
	}

	public String getAnnotatedClassName() {
		return annotatedClassName;
	}

	public String getFieldName() {
		return fieldName;
	}
	
	
	
}
