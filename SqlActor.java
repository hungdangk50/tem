package com.seabank.hrsb.database.type;

import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

public class SqlActor extends Actor implements SQLData {

	@Override
	public String getSQLTypeName() throws SQLException {
		// TODO Auto-generated method stub
		return "ACTOR_TYPE";
	}

	@Override
	public void readSQL(SQLInput sqlInput, String str) throws SQLException {
		setId(Long.valueOf(sqlInput.readLong()));
		setName(sqlInput.readString());
		setAge(sqlInput.readInt());

	}

	@Override
	public void writeSQL(SQLOutput sqlOutput) throws SQLException {
		sqlOutput.writeLong(getId().longValue());
		sqlOutput.writeString(getName());
		sqlOutput.writeInt(getAge());
	}

}
