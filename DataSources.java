package com.seabank.hrsb.database;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.seabank.hrsb.adapter.DBAdapter;

import oracle.jdbc.driver.OracleConnection;

public class DataSources {

	public static DBAdapter orclAdapter;
	public static DBAdapter pgAdapter;

	public DataSources() {
		System.out.println("DataSources init");
	}

	@Autowired
	@Qualifier("orclAdapter")
	public void setOrclAdapter(DBAdapter orclAdapter) {
		DataSources.orclAdapter = orclAdapter;
	}

	@Autowired
	@Qualifier("pgAdapter")
	public void setPgAdapter(DBAdapter pgAdapter) {
		DataSources.pgAdapter = pgAdapter;
	}

	public static Array creatArraySql(String paramName, List<? extends Object> data) throws SQLException {
		Array retVal = null;
		Connection connPool = null;
		try {
			connPool = orclAdapter.getTemplete().getDataSource().getConnection();
			if (connPool instanceof OracleConnection) {
				retVal = ((OracleConnection) connPool).createARRAY(paramName, data.toArray());
			} else {
				OracleConnection orc = null;
				orc = connPool.unwrap(OracleConnection.class);
				retVal = orc.createARRAY(paramName, data.toArray());
			}
		} finally {
			if (connPool != null) {
				connPool.close();
			}
		}

		return retVal;
	}

	public static void freeArray(Array array) {
		if (array != null) {
			try {
				array.free();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

}
