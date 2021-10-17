package com.seabank.hrsb.database.type;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.sql.rowset.serial.SQLInputImpl;

import org.springframework.jdbc.core.SqlReturnType;

public class SeAReturnType<T extends SQLData> implements SqlReturnType {
	private Class<T> typeItem;

	public SeAReturnType(Class<T> classItem) {
		this.typeItem = classItem;
	}

	@Override
	public List<T> getTypeValue(CallableStatement cs, int i, int sqlType, String typeName) throws SQLException {
		// TODO Auto-generated method stub
		List<T> retVal = new ArrayList<>();
		try {
			if (typeItem != null) {
				Array arraySql = cs.getArray(i);
				for (int j = 0; j < ((Object[]) arraySql.getArray()).length; j++) {
					Struct struc = (Struct) ((Object[]) arraySql.getArray())[j];
					if (struc != null) {
						SQLInputImpl inputstream = new SQLInputImpl(struc.getAttributes(), new HashMap<>());
						T item = typeItem.newInstance();
						item.readSQL(inputstream, struc.getSQLTypeName());
						retVal.add(item);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return retVal;
	}

}
