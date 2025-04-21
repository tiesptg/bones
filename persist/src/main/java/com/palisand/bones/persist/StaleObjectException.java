package com.palisand.bones.persist;

import java.sql.SQLException;

@SuppressWarnings("serial")
public class StaleObjectException extends SQLException {

	public StaleObjectException(String reason) {
		super(reason);
	}

}
