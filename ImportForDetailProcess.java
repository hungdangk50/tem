package com.seabank.hrsb.async.process;

import java.util.Arrays;
import java.util.List;

import org.springframework.jdbc.UncategorizedSQLException;

import com.seabank.hrsb.async.AsyncResult;
import com.seabank.hrsb.async.ImportRequest;
import com.seabank.hrsb.constant.ApiConfig;
import com.seabank.hrsb.database.CallRequest;
import com.seabank.hrsb.database.DataSources;
import com.seabank.hrsb.database.StoreParam;
import com.seabank.hrsb.model.SeAResult;
import com.seabank.hrsb.model.UserUpdateConfigApproveForDetail;
import com.seabank.hrsb.utils.JsonUtils;
import com.seabank.hrsb.utils.StringUtils;

import oracle.jdbc.OracleTypes;

public class ImportForDetailProcess extends AbstractImportProcess {

	public ImportForDetailProcess() {
		super();
	}

	@Override
	public AsyncResult processImport(ImportRequest importReq) {
		AsyncResult retVal = new AsyncResult();
		try {
			String importMsg = importReq.getImport_msg();
			UserUpdateConfigApproveForDetail[] ImportArr = (UserUpdateConfigApproveForDetail[]) JsonUtils
					.stringJson2Object(importMsg, UserUpdateConfigApproveForDetail[].class);
			List<UserUpdateConfigApproveForDetail> updateList = Arrays.asList(ImportArr);
			CallRequest callRQ = new CallRequest("ip_user_cfg_approve_for_detail".toUpperCase());
			callRQ.setPackageName(ApiConfig.cfg.getPackagePermissionUIName());
			callRQ.addParam(new StoreParam("import_list", OracleTypes.ARRAY,
					DataSources.creatArraySql("update_cfg_4_detail_list".toUpperCase(), updateList)));
			callRQ.addParam(new StoreParam("in_user_name", OracleTypes.VARCHAR, importReq.getUser_import()));
			orclAdapter.call(callRQ);
			retVal.setResult(new SeAResult("000"));
			retVal.setDataResponse(new Object());
		} catch (UncategorizedSQLException sqlEx) {
			SeAResult err = new SeAResult("304");
			int errorCode = sqlEx.getSQLException().getErrorCode();
			if (errorCode == 20004) {
				err = new SeAResult("401");
			} else if (errorCode == 20001) {
				err = new SeAResult("304");
			} else if (errorCode == 20002) {
				err = new SeAResult("300");
			} else if (errorCode == 20003) {
				err = new SeAResult("306");
			}
			err.setDetail(StringUtils.getErrorDetail(sqlEx.getSQLException().getMessage()));
			retVal.setResult(err);
		} catch (Exception e) {
			SeAResult err = new SeAResult("306");
			err.setDetail(e.getMessage());
			retVal.setResult(err);
		}
		return retVal;
	}

}
