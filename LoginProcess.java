package com.seabank.hrsb.process;

import java.util.List;
import java.util.Map;

import com.seabank.hrsb.base.ReqMes;
import com.seabank.hrsb.base.ResMsg;
import com.seabank.hrsb.constant.ApiConfig;
import com.seabank.hrsb.database.CallRequest;
import com.seabank.hrsb.database.SFetchExtractor;
import com.seabank.hrsb.database.StoreParam;
import com.seabank.hrsb.message.LoginRes;
import com.seabank.hrsb.message.body.LoginBodyRes;
import com.seabank.hrsb.model.Function;
import com.seabank.hrsb.model.PermissionInfo;
import com.seabank.hrsb.model.SeAMail;
import com.seabank.hrsb.model.SeAResult;
import com.seabank.hrsb.model.UserInfo;
import com.seabank.hrsb.utils.StringUtils;

import oracle.jdbc.OracleTypes;

public class LoginProcess extends AbstractProcess {

	@SuppressWarnings("unchecked")
	@Override
	public void processBody(ReqMes reqMsg, ResMsg resMsg) {
		try {
			LoginBodyRes bodyRes = new LoginBodyRes();
			CallRequest callRQ = new CallRequest("login");
			callRQ.setPackageName(ApiConfig.cfg.packagePermissionName);
			callRQ.addParam(new StoreParam("user_login", OracleTypes.VARCHAR, reqMsg.getHeader().getUserID()));
			callRQ.addParam(new StoreParam("user_infor_out", OracleTypes.CURSOR, null,
					new SFetchExtractor<UserInfo>(UserInfo.class)));
			callRQ.addParam(new StoreParam("org_ids_out", OracleTypes.VARCHAR, false));
			callRQ.addParam(new StoreParam("functions_out", OracleTypes.CURSOR, null,
					new SFetchExtractor<Function>(Function.class)));
			Map<String, Object> ret = orclAdapter.call(callRQ);
			List<UserInfo> users = (List<UserInfo>) ret.get("user_infor_out");
			String orgids = (String) ret.get("org_ids_out");
			List<Function> functions = (List<Function>) ret.get("functions_out");
			if (users.size() > 0) {
				bodyRes.setInfomation(users.get(0));
				PermissionInfo per = new PermissionInfo();
				per.setOrg_ids(StringUtils.Trim(orgids, ","));
				per.setFunctions(functions);
				bodyRes.setPermission(per);
				((LoginRes) resMsg).setBody(bodyRes);
				((LoginRes) resMsg).getHeader().setResult(new SeAResult("000"));
			} else {
				SeAResult ers = new SeAResult("307");
				ers.setDetail(reqMsg.getHeader().getUserID());
				resMsg.getHeader().setResult(ers);
			}
		} catch (Exception e) {
			SeAMail mail = new SeAMail();
			mail.setSubject("LoginProcess-processBody-Exception");
			mail.setConten(e.toString());
			mailAdapter.alert(mail);
			SeAResult ers = new SeAResult("306");
			ers.setDetail(e.toString());
			resMsg.getHeader().setResult(ers);
		}
	}
}
