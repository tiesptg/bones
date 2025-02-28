package com.palisand.bones.meta;

public enum Type {
	STRING, INTEGER, LONG, DOUBLE, FLOAT, TIMESTAMP, BOOLEAN, ENUM, OBJECT;
	
	public boolean isNumber() {
		switch (this) {
			case INTEGER: case LONG: case DOUBLE: case FLOAT: return true;
			default: return false;
		}
	}
}
