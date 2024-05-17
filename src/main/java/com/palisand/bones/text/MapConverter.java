package com.palisand.bones.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class MapConverter implements Converter<Map>{
	private TypedMarkupText container = null;
	
	public void setContainer(TypedMarkupText text) {
		container = text;
	}


	@Override
	public <Y> Y fromYaml(BufferedReader in, Class<Y> cls, String margin) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void writeYaml(Object obj, PrintWriter out, String margin) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
