package com.palisand.bones.meta;

public enum Type {
	STRING, INTEGER, DOUBLE, TIMESTAMP, BOOLEAN, ENUM, OBJECT;
	
	public boolean isNumber() {
		switch (this) {
			case INTEGER: case DOUBLE: return true;
			default: return false;
		}
	}
}
