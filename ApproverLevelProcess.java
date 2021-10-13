package com.seabank.hrsb.process;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlReturnType;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

import com.seabank.hrsb.base.ReqMes;
import com.seabank.hrsb.base.ResMsg;
import com.seabank.hrsb.constant.ApiConfig;
import com.seabank.hrsb.database.CallRequest;
import com.seabank.hrsb.database.DataSources;
import com.seabank.hrsb.database.StoreParam;
import com.seabank.hrsb.message.GetApproverLevelReq;
import com.seabank.hrsb.message.GetApproverLevelRes;
import com.seabank.hrsb.message.GetFuncApproveBodyRes;
import com.seabank.hrsb.message.GetFuncApproveReq;
import com.seabank.hrsb.message.GetFuncApproveRes;
import com.seabank.hrsb.message.UpdateLastLevelReq;
import com.seabank.hrsb.message.body.GetApproverLevelBodyReq;
import com.seabank.hrsb.message.body.GetApproverLevelBodyRes;
import com.seabank.hrsb.message.body.UpdateLastLevelBodyReq;
import com.seabank.hrsb.model.ApproverLevel;
import com.seabank.hrsb.model.ModuleLastLevel;
import com.seabank.hrsb.model.SeAMail;
import com.seabank.hrsb.model.SeAResult;

import oracle.jdbc.OracleTypes;

public class ApproverLevelProcess extends AbstractProcess {

	@Override
	public void processBody(ReqMes reqMsg, ResMsg resMsg) {
		if (reqMsg instanceof UpdateLastLevelReq) {
			processUpdateLevel(reqMsg, resMsg);
		} else if (reqMsg instanceof GetApproverLevelReq) {
			processGetApprovelLevel(reqMsg, resMsg);
		} else if (reqMsg instanceof GetFuncApproveReq) {
			processGetFuncApprovel(reqMsg, resMsg);
		}
	}

	@SuppressWarnings("unchecked")
	private void processGetFuncApprovel(ReqMes reqMsg, ResMsg resMsg) {
		try {
			SimpleJdbcCall call = new SimpleJdbcCall(DataSources.orclAdapter.getTemplete());
			call.withCatalogName(ApiConfig.cfg.packagePermissionUIName).withFunctionName("get_func_approve");
			call.declareParameters(
					new SqlOutParameter("return", OracleTypes.ARRAY, "nchar_array".toUpperCase(), new SqlReturnType() {

						@Override
						public Object getTypeValue(CallableStatement cs, int i, int sqlType, String typeName)
								throws SQLException {
							List<String> retVal = new ArrayList<>();
							try {
								Array arraySql = cs.getArray(i);
								for (int j = 0; j < ((Object[]) arraySql.getArray()).length; j++) {
									String value = (String) ((Object[]) arraySql.getArray())[j];
									if (value != null) {
										retVal.add(value);
									}
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
							return retVal;
						}
					}));
			Object listFunc = call.executeFunction(Object.class, new HashMap<>());
			if (listFunc != null) {
				GetFuncApproveBodyRes resBody = new GetFuncApproveBodyRes();
				resBody.setApprove_func((List<String>) listFunc);
				((GetFuncApproveRes) resMsg).setBody(resBody);
			} else {
				SeAResult ers = new SeAResult("306");
				ers.setDetail("null Value");
				resMsg.getHeader().setResult(ers);
			}

		} catch (Exception e) {
			SeAMail mail = new SeAMail();
			mail.setSubject("ManageApprover4OrgProcess-processValidMethod-Exception");
			mail.setConten(e.toString());
			mailAdapter.alert(mail);
			SeAResult ers = new SeAResult("306");
			ers.setDetail(e.toString());
			resMsg.getHeader().setResult(ers);
		}

	}

	private void processGetApprovelLevel(ReqMes reqMsg, ResMsg resMsg) {
		try {
			GetApproverLevelBodyReq bodyReq = ((GetApproverLevelReq) reqMsg).getBody();
			GetApproverLevelBodyRes body = new GetApproverLevelBodyRes();
			String machucNang = bodyReq.getFilter().getMa_chuc_nang();
			StringBuilder sql = new StringBuilder("select * from Table(").append(ApiConfig.cfg.packagePermissionUIName)
					.append(".get_approver_last_level(?))");
//			String sql = "select * from Table(cham_cong_permission_ui.get_approver_last_level(?))";
			List<ApproverLevel> approverLevel = DataSources.orclAdapter.getTemplete().query(sql.toString(),
					new Object[] { machucNang }, new int[] { OracleTypes.VARCHAR },
					new ResultSetExtractor<List<ApproverLevel>>() {

						@Override
						public List<ApproverLevel> extractData(ResultSet rs) throws SQLException, DataAccessException {
							// TODO Auto-generated method gstub
							List<ApproverLevel> lst = new ArrayList<>();
							while (rs.next()) {
								ApproverLevel nx = new ApproverLevel();
								nx.load(rs);
								lst.add(nx);
							}
							return lst;
						}
					});
			body.setDatas(approverLevel);
			((GetApproverLevelRes) resMsg).setBody(body);
		} catch (Exception e) {
			SeAResult err = new SeAResult("306");
			err.setDetail(e.getMessage());
			((ResMsg) resMsg).getHeader().setResult(err);
			e.printStackTrace();
			e.printStackTrace();
		}
	}

	private void processUpdateLevel(ReqMes reqMsg, ResMsg resMsg) {
		try {
			UpdateLastLevelBodyReq body = ((UpdateLastLevelReq) reqMsg).getBody();
			List<ModuleLastLevel> updateList = body.getRows();
			CallRequest callRQ = new CallRequest("update_approver_last_level");
			callRQ.setPackageName(ApiConfig.cfg.packagePermissionUIName);
			callRQ.addParam(new StoreParam("level_list", OracleTypes.ARRAY,
					DataSources.creatArraySql("MODULE_LAST_LEVEL_LIST", updateList)));
			orclAdapter.call(callRQ);
			((ResMsg) resMsg).getHeader().setResult(new SeAResult("000"));
		} catch (UncategorizedSQLException sqlEx) {
			SeAResult err = new SeAResult("304");
			err.setDetail(sqlEx.getCause().getLocalizedMessage());
			((ResMsg) resMsg).getHeader().setResult(err);
		} catch (Exception e) {
			SeAResult err = new SeAResult("306");
			err.setDetail(e.getMessage());
			((ResMsg) resMsg).getHeader().setResult(err);
			e.printStackTrace();
		}
	}

}
