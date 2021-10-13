package com.seabank.hrsb.process;

import java.sql.Array;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.postgresql.util.PGobject;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.SqlInOutParameter;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

import com.seabank.hrsb.base.ReqMes;
import com.seabank.hrsb.base.ResMsg;
import com.seabank.hrsb.constant.ApiConfig;
import com.seabank.hrsb.constant.ImportEnum;
import com.seabank.hrsb.database.CallRequest;
import com.seabank.hrsb.database.DataSources;
import com.seabank.hrsb.database.StoreParam;
import com.seabank.hrsb.database.type.SeAReturnType;
import com.seabank.hrsb.message.AsyncProcessRes;
import com.seabank.hrsb.message.CreatConfigApproveForDetailReq;
import com.seabank.hrsb.message.GetConfigApproveForDetailExportReq;
import com.seabank.hrsb.message.GetConfigApproveForDetailExportRes;
import com.seabank.hrsb.message.GetConfigApproveForDetailReq;
import com.seabank.hrsb.message.GetConfigApproveForDetailRes;
import com.seabank.hrsb.message.ImportAsyncReq;
import com.seabank.hrsb.message.RemoveConfigApproveReq;
import com.seabank.hrsb.message.UpdateConfigApproveForDetailReq;
import com.seabank.hrsb.message.ValidImportConfigApproveForDetailReq;
import com.seabank.hrsb.message.ValidImportConfigApproveForDetailRes;
import com.seabank.hrsb.message.body.AsyncProcessBodyRes;
import com.seabank.hrsb.message.body.GetConfigApproveForDetailBodyReq;
import com.seabank.hrsb.message.body.GetConfigApproveForDetailBodyRes;
import com.seabank.hrsb.message.body.GetConfigApproveForDetailExportBodyRes;
import com.seabank.hrsb.message.body.RemoveConfigApproveBodyReq;
import com.seabank.hrsb.message.body.UpdateConfigApproveForDetailBodyReq;
import com.seabank.hrsb.message.body.ValidImportConfigApproveForDetailBodyRes;
import com.seabank.hrsb.message.body.ValidImportConfigApproveforDetailBodyReq;
import com.seabank.hrsb.model.AsyncProcessState;
import com.seabank.hrsb.model.SeAMail;
import com.seabank.hrsb.model.SeAResult;
import com.seabank.hrsb.model.UserConfigApproverForDetailExport;
import com.seabank.hrsb.model.UserConfigApproverForDetal;
import com.seabank.hrsb.model.UserUpdateConfigApproveForDetail;
import com.seabank.hrsb.model.ValidUserConfigApproverForDetail;
import com.seabank.hrsb.utils.KeysUtils;
import com.seabank.hrsb.utils.StringUtils;

import oracle.jdbc.OracleTypes;

public class ManageApprover4DetailProcess extends AbstractProcess {

	@Override
	public void processBody(ReqMes reqMsg, ResMsg resMsg) {
		if (reqMsg instanceof GetConfigApproveForDetailReq) {
			processGetMethod(reqMsg, resMsg);
		} else if (reqMsg instanceof UpdateConfigApproveForDetailReq) {
			processUpdateMethod(reqMsg, resMsg);
		} else if (reqMsg instanceof CreatConfigApproveForDetailReq) {
			processCreatMethod(reqMsg, resMsg);
		} else if (reqMsg instanceof RemoveConfigApproveReq) {
			processRemoveMethod(reqMsg, resMsg);
		} else if (reqMsg instanceof ValidImportConfigApproveForDetailReq) {
			processValidMethod(reqMsg, resMsg);
		} else if (reqMsg instanceof ImportAsyncReq) {
			processImportMethod(reqMsg, resMsg);
		} else if (reqMsg instanceof GetConfigApproveForDetailExportReq) {
			processGetForExport(reqMsg, resMsg);
		}
	}

	@SuppressWarnings("unchecked")
	private void processGetForExport(ReqMes reqMsg, ResMsg resMsg) {
		try {
			GetConfigApproveForDetailBodyReq bodyReq = ((GetConfigApproveForDetailExportReq) reqMsg).getBody();
			SimpleJdbcCall call = new SimpleJdbcCall(DataSources.orclAdapter.getTemplete());
			call.withCatalogName(ApiConfig.cfg.packagePermissionUIName)
					.withProcedureName("fetch_cfg_approve_4_detail_ex")
					.addDeclaredParameter(new SqlInOutParameter("in_id_don_vi", OracleTypes.VARCHAR));
			call.addDeclaredParameter(new SqlInOutParameter("in_ma_module", OracleTypes.VARCHAR));
			call.addDeclaredParameter(new SqlInOutParameter("in_staff_sb", OracleTypes.VARCHAR));
			call.addDeclaredParameter(new SqlInOutParameter("in_staff_name", OracleTypes.VARCHAR));
			call.addDeclaredParameter(new SqlOutParameter("out_config_4detail_export", OracleTypes.ARRAY,
					"user_cfg_4_detail_export_list".toUpperCase(),
					new SeAReturnType<UserConfigApproverForDetailExport>(UserConfigApproverForDetailExport.class)));
			Map<String, Object> in = new HashMap<>();
			in.put("in_id_don_vi", StringUtils.getString(bodyReq.getFilter().getId_don_vi()));
			in.put("in_ma_module", bodyReq.getFilter().getMa_chuc_nang());
			in.put("in_staff_sb", bodyReq.getFilter().getStaff_sb());
			in.put("in_staff_name", bodyReq.getFilter().getStaff_name());
			Map<String, Object> retMap = call.execute(in);
			Object listConfig = retMap.get("out_config_4detail_export");
			GetConfigApproveForDetailExportBodyRes body = new GetConfigApproveForDetailExportBodyRes();
			if (listConfig != null) {
				body.setRows((List<UserConfigApproverForDetailExport>) listConfig);
			}
			((GetConfigApproveForDetailExportRes) resMsg).setBody(body);
			resMsg.getHeader().setResult(new SeAResult("000"));
		} catch (Exception e) {
			SeAMail mail = new SeAMail();
			mail.setSubject("ManageApprover4DetailProcess-processBody-Exception");
			mail.setConten(e.toString());
			mailAdapter.alert(mail);
			SeAResult ers = new SeAResult("306");
			ers.setDetail(e.toString());
			resMsg.getHeader().setResult(ers);
		}

	}

	private void processImportMethod(ReqMes reqMsg, ResMsg resMsg) {
		try {
			String sql = "SELECT f_q_import(?,?,?,?,?)";
			String requestID = KeysUtils.generateKey("reqID", "per", 20);
			String node = ApiConfig.cfg.getNode();
			String status = ImportEnum.RECIEVER.getValue();
			String command = reqMsg.getCommand();
			PGobject jsonObject = new PGobject();
			jsonObject.setType("json");
			jsonObject.setValue(StringUtils.object2json(reqMsg));
			pgAdapter.getTemplete().queryForRowSet(sql, new Object[] { requestID, node, status, command, jsonObject },
					new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.OTHER });
			AsyncProcessBodyRes body = new AsyncProcessBodyRes();
			AsyncProcessState state = new AsyncProcessState();
			state.setRequest_id(requestID);
			state.setState(ImportEnum.PROCESSING.getValue());
			state.setDescription("");
			body.setProcess(state);
			body.setResponse_data(new Object());
			((AsyncProcessRes) resMsg).setBody(body);
			resMsg.getHeader().setResult(new SeAResult("000"));
		} catch (Exception e) {
			SeAMail mail = new SeAMail();
			mail.setSubject("ManageApprover4DetailProcess-processImportMethod-Exception");
			mail.setConten(e.toString());
			mailAdapter.alert(mail);
			SeAResult ers = new SeAResult("306");
			ers.setDetail(e.toString());
			resMsg.getHeader().setResult(ers);
		}
	}

	@SuppressWarnings("unchecked")
	private void processValidMethod(ReqMes reqMsg, ResMsg resMsg) {
		Array sqlArr = null;
		try {
			ValidImportConfigApproveforDetailBodyReq reqbody = ((ValidImportConfigApproveForDetailReq) reqMsg)
					.getBody();
			List<UserUpdateConfigApproveForDetail> importList = reqbody.getRows();
			List<ValidUserConfigApproverForDetail> validList = new ArrayList<>();
			List<UserUpdateConfigApproveForDetail> validToDb = new ArrayList<>();
			for (UserUpdateConfigApproveForDetail vl : importList) {
				ValidUserConfigApproverForDetail validTmp = new ValidUserConfigApproverForDetail(vl);
				if (!validTmp.getValid().status) {
					validList.add(validTmp);
				} else {
					validToDb.add(vl);
				}
			}

			if (validToDb.size() > 0) {
				sqlArr = DataSources.creatArraySql("update_cfg_4_detail_list".toUpperCase(), validToDb);
				SimpleJdbcCall call = new SimpleJdbcCall(DataSources.orclAdapter.getTemplete());
				call.withCatalogName(ApiConfig.cfg.packagePermissionUIName)
						.withFunctionName("vl_import_cfg_approve_4_detail");
				call.addDeclaredParameter(new SqlParameter("import_list", OracleTypes.ARRAY));
				call.declareParameters(new SqlOutParameter("return", OracleTypes.ARRAY,
						"valid_cfg_4_detail_list".toUpperCase(),
						new SeAReturnType<ValidUserConfigApproverForDetail>(ValidUserConfigApproverForDetail.class)));
				Map<String, Object> param = new HashMap<>();
				param.put("import_list", sqlArr);
				List<ValidUserConfigApproverForDetail> validListRet = (List<ValidUserConfigApproverForDetail>) call
						.executeFunction(Object.class, param);
				validList.addAll(validListRet);
			}
			SeAResult mainRs = getMainResult(validList);
			ValidImportConfigApproveForDetailBodyRes resBody = new ValidImportConfigApproveForDetailBodyRes();
			String session = saveImportRequest(reqMsg.getCommand(), importList, reqbody.getImport_session(), mainRs,
					reqMsg.getHeader().getUserID());
			resBody.setImport_session(session);
			resBody.setRows((List<ValidUserConfigApproverForDetail>) validList);
			resBody.setValid(mainRs);
			((ValidImportConfigApproveForDetailRes) resMsg).setBody(resBody);
			resMsg.getHeader().setResult(new SeAResult("000"));

		} catch (Exception e) {
			SeAMail mail = new SeAMail();
			mail.setSubject("ManageApprover4DetailProcess-processBody-Exception");
			mail.setConten(e.toString());
			mailAdapter.alert(mail);
			SeAResult ers = new SeAResult("306");
			ers.setDetail(e.toString());
			resMsg.getHeader().setResult(ers);
		} finally {
			DataSources.freeArray(sqlArr);
		}

	}

	private SeAResult getMainResult(List<ValidUserConfigApproverForDetail> valids) {
		SeAResult retVal = new SeAResult("000");
		if (valids != null) {
			for (ValidUserConfigApproverForDetail validItem : (List<ValidUserConfigApproverForDetail>) valids) {
				if (!validItem.getValid().status) {
					retVal = validItem.getValid();
					break;
				}
			}
		} else {
			SeAResult ers = new SeAResult("306");
			ers.setDetail("null valids");
		}
		return retVal;
	}

	private String saveImportRequest(String Command, List<UserUpdateConfigApproveForDetail> validList,
			String import_session, SeAResult mainResult, String userName) {
		// TODO Auto-generated method stub
		String sesion = import_session;
		if (StringUtils.isEmpty(sesion)) {
			sesion = KeysUtils.generateKey("per-import", "for-detail", 10);
		}
		try {
			String sql = "SELECT f_q_valid_import(?,?,?,?,?,?)";
			PGobject jsonObject = new PGobject();
			jsonObject.setType("json");
			jsonObject.setValue("{}");
			String importData = StringUtils.object2json(validList);
			ImportEnum state = mainResult.status ? ImportEnum.VALID : ImportEnum.NOTVALID;
			pgAdapter.getTemplete().queryForRowSet(sql,
					new Object[] { sesion, Command, importData, state.getValue(), "22", jsonObject }, new int[] {
							Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.OTHER });
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

		return sesion;
	}

	@SuppressWarnings("unchecked")
	private void processGetMethod(ReqMes reqMsg, ResMsg resMsg) {
		try {
			GetConfigApproveForDetailBodyReq bodyReq = ((GetConfigApproveForDetailReq) reqMsg).getBody();
			SimpleJdbcCall call = new SimpleJdbcCall(DataSources.orclAdapter.getTemplete());
			call.withCatalogName(ApiConfig.cfg.packagePermissionUIName)
					.withProcedureName("fetch_config_approve_4_detail")
					.addDeclaredParameter(new SqlInOutParameter("in_id_don_vi", OracleTypes.VARCHAR));
			call.addDeclaredParameter(new SqlInOutParameter("in_ma_module", OracleTypes.VARCHAR));
			call.addDeclaredParameter(new SqlInOutParameter("in_staff_sb", OracleTypes.VARCHAR));
			call.addDeclaredParameter(new SqlInOutParameter("in_staff_name", OracleTypes.VARCHAR));
			call.addDeclaredParameter(
					new SqlOutParameter("out_config_4detail", OracleTypes.ARRAY, "user_cfg_4_detail_list".toUpperCase(),
							new SeAReturnType<UserConfigApproverForDetal>(UserConfigApproverForDetal.class)));
			Map<String, Object> in = new HashMap<>();
			in.put("in_id_don_vi", StringUtils.getString(bodyReq.getFilter().getId_don_vi()));
			in.put("in_ma_module", bodyReq.getFilter().getMa_chuc_nang());
			in.put("in_staff_sb", bodyReq.getFilter().getStaff_sb());
			in.put("in_staff_name", bodyReq.getFilter().getStaff_name());
			Map<String, Object> retMap = call.execute(in);
			Object listConfig = retMap.get("out_config_4detail");
			GetConfigApproveForDetailBodyRes body = new GetConfigApproveForDetailBodyRes();
			if (listConfig != null) {
				body.setRows((List<UserConfigApproverForDetal>) listConfig);
			}
			((GetConfigApproveForDetailRes) resMsg).setBody(body);
			resMsg.getHeader().setResult(new SeAResult("000"));
		} catch (Exception e) {
			SeAMail mail = new SeAMail();
			mail.setSubject("ManageApprover4DetailProcess-processBody-Exception");
			mail.setConten(e.toString());
			mailAdapter.alert(mail);
			SeAResult ers = new SeAResult("306");
			ers.setDetail(e.toString());
			resMsg.getHeader().setResult(ers);
		}
	}

	private void processUpdateMethod(ReqMes reqMsg, ResMsg resMsg) {
		try {
			UpdateConfigApproveForDetailBodyReq body = ((UpdateConfigApproveForDetailReq) reqMsg).getBody();
			List<UserUpdateConfigApproveForDetail> updateList = body.getRows();
			CallRequest callRQ = new CallRequest("u_user_cfg_approve_for_detail".toUpperCase());
			callRQ.setPackageName(ApiConfig.cfg.packagePermissionUIName);
			callRQ.addParam(new StoreParam("update_list", OracleTypes.ARRAY,
					DataSources.creatArraySql("update_cfg_4_detail_list".toUpperCase(), updateList)));
			callRQ.addParam(new StoreParam("in_user_name", OracleTypes.VARCHAR, reqMsg.getHeader().getUserID()));
			orclAdapter.call(callRQ);
			((ResMsg) resMsg).getHeader().setResult(new SeAResult("000"));
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
			((ResMsg) resMsg).getHeader().setResult(err);
		} catch (Exception e) {
			SeAMail mail = new SeAMail();
			mail.setSubject("ManageApprover4DetailProcess-processUpdateMethod-Exception");
			mail.setConten(e.toString());
			mailAdapter.alert(mail);
			SeAResult err = new SeAResult("306");
			err.setDetail(e.getMessage());
			((ResMsg) resMsg).getHeader().setResult(err);
			e.printStackTrace();
		}
	}

	private void processCreatMethod(ReqMes reqMsg, ResMsg resMsg) {
		try {
			UpdateConfigApproveForDetailBodyReq body = ((CreatConfigApproveForDetailReq) reqMsg).getBody();
			List<UserUpdateConfigApproveForDetail> updateList = body.getRows();
			CallRequest callRQ = new CallRequest("c_user_cfg_approve_for_detail".toUpperCase());
			callRQ.setPackageName(ApiConfig.cfg.packagePermissionUIName);
			callRQ.addParam(new StoreParam("creat_list", OracleTypes.ARRAY,
					DataSources.creatArraySql("update_cfg_4_detail_list".toUpperCase(), updateList)));
			callRQ.addParam(new StoreParam("in_user_name", OracleTypes.VARCHAR, reqMsg.getHeader().getUserID()));
			orclAdapter.call(callRQ);
			resMsg.getHeader().setResult(new SeAResult("000"));
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
			((ResMsg) resMsg).getHeader().setResult(err);
		} catch (Exception e) {
			SeAMail mail = new SeAMail();
			mail.setSubject("ManageApprover4DetailProcess-processCreatMethod-Exception");
			mail.setConten(e.toString());
			mailAdapter.alert(mail);
			SeAResult err = new SeAResult("306");
			err.setDetail(e.getMessage());
			((ResMsg) resMsg).getHeader().setResult(err);
			e.printStackTrace();
		}
	}

	private void processRemoveMethod(ReqMes reqMsg, ResMsg resMsg) {
		Array arraySql = null;
		try {
			RemoveConfigApproveBodyReq body = ((RemoveConfigApproveReq) reqMsg).getBody();
			List<Integer> rowsRemove = body.getRows();
			if (rowsRemove.size() > 0) {
				arraySql = DataSources.creatArraySql("config_id_list".toUpperCase(), rowsRemove);
				SimpleJdbcCall call = new SimpleJdbcCall(orclAdapter.getTemplete());
				call.withCatalogName(ApiConfig.cfg.packagePermissionUIName);
				call.withFunctionName("delete_cfg_approve".toUpperCase());
				call.declareParameters(new SqlParameter("list_id", OracleTypes.ARRAY, "config_id_list".toUpperCase()));
				call.declareParameters(new SqlParameter("in_user_name", OracleTypes.VARCHAR));
				Map<String, Object> in = new HashMap<>();
				in.put("list_id", arraySql);
				in.put("in_user_name", reqMsg.getHeader().getUserID());
				call.executeFunction(Object.class, in);
			}
			resMsg.getHeader().setResult(new SeAResult("000"));
		} catch (Exception e) {
			SeAResult err = new SeAResult("306");
			err.setDetail(e.getMessage());
			((ResMsg) resMsg).getHeader().setResult(err);
		} finally {
			DataSources.freeArray(arraySql);
		}
	}
}
