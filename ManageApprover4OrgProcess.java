package com.seabank.hrsb.process;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.postgresql.util.PGobject;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.ResultSetExtractor;
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
import com.seabank.hrsb.message.CreatConfigApproveForOrgReq;
import com.seabank.hrsb.message.GetConfigApproveForOrgExportReq;
import com.seabank.hrsb.message.GetConfigApproveForOrgExportRes;
import com.seabank.hrsb.message.GetConfigApproveForOrgReq;
import com.seabank.hrsb.message.GetConfigApproveForOrgRes;
import com.seabank.hrsb.message.GetOrgDetailReq;
import com.seabank.hrsb.message.GetOrgDetailsRes;
import com.seabank.hrsb.message.ImportAsyncReq;
import com.seabank.hrsb.message.RemoveConfigApproveReq;
import com.seabank.hrsb.message.UpdateConfigApproveForOrgReq;
import com.seabank.hrsb.message.ValidImportConfigApproverForOrgReq;
import com.seabank.hrsb.message.ValidImportConfigApproverForOrgRes;
import com.seabank.hrsb.message.body.AsyncProcessBodyRes;
import com.seabank.hrsb.message.body.GetConfigApproveForOrgBodyReq;
import com.seabank.hrsb.message.body.GetConfigApproveForOrgBodyRes;
import com.seabank.hrsb.message.body.GetConfigApproveForOrgExportBodyRes;
import com.seabank.hrsb.message.body.GetOrgDetailBodyReq;
import com.seabank.hrsb.message.body.GetOrgDetailBodyRes;
import com.seabank.hrsb.message.body.RemoveConfigApproveBodyReq;
import com.seabank.hrsb.message.body.UpdateConfigApproveForOrgBodyReq;
import com.seabank.hrsb.message.body.ValidImportConfigApproveForOrgBodyReq;
import com.seabank.hrsb.message.body.ValidImportConfigApproveForOrgBodyRes;
import com.seabank.hrsb.model.AsyncProcessState;
import com.seabank.hrsb.model.OrgDetails;
import com.seabank.hrsb.model.SeAMail;
import com.seabank.hrsb.model.SeAResult;
import com.seabank.hrsb.model.UserConfigApproverForOrg;
import com.seabank.hrsb.model.UserConfigApproverForOrgExport;
import com.seabank.hrsb.model.UserUpdateConfigApproveForOrg;
import com.seabank.hrsb.model.ValidUserConfigApproverForOrg;
import com.seabank.hrsb.utils.KeysUtils;
import com.seabank.hrsb.utils.StringUtils;

import oracle.jdbc.OracleTypes;

public class ManageApprover4OrgProcess extends AbstractProcess {

	@Override
	public void processBody(ReqMes reqMsg, ResMsg resMsg) {
		if (reqMsg instanceof GetConfigApproveForOrgReq) {
			processGetMethod(reqMsg, resMsg);
		} else if (reqMsg instanceof CreatConfigApproveForOrgReq) {
			processCreatMethod(reqMsg, resMsg);
		} else if (reqMsg instanceof UpdateConfigApproveForOrgReq) {
			processUpdateMethod(reqMsg, resMsg);
		} else if (reqMsg instanceof RemoveConfigApproveReq) {
			processRemoveMethod(reqMsg, resMsg);
		} else if (reqMsg instanceof ValidImportConfigApproverForOrgReq) {
			processValidMethod(reqMsg, resMsg);
		} else if (reqMsg instanceof ImportAsyncReq) {
			processImportMethod(reqMsg, resMsg);
		} else if (reqMsg instanceof GetConfigApproveForOrgExportReq) {
			processGetForExport(reqMsg, resMsg);
		} else if (reqMsg instanceof GetOrgDetailReq) {
			processGetOrgDetails(reqMsg, resMsg);
		}
	}

	private void processGetOrgDetails(ReqMes reqMsg, ResMsg resMsg) {
		Array sqlArr = null;
		try {
			GetOrgDetailBodyReq bodyReq = ((GetOrgDetailReq) reqMsg).getBody();
			List<Integer> orgIds = bodyReq.getOrg_ids();
			if (orgIds.size() > 0) {
				List<OrgDetails> orgDetails = null;
				GetOrgDetailBodyRes body = new GetOrgDetailBodyRes();
				sqlArr = DataSources.creatArraySql("config_id_list".toUpperCase(), orgIds);
				StringBuilder sql = new StringBuilder("select * from Table(")
						.append(ApiConfig.cfg.packagePermissionUIName).append(".get_org_details(?))");
//				String sql = "select * from Table(cham_cong_permission_ui.get_org_details(?))";
				orgDetails = DataSources.orclAdapter.getTemplete().query(sql.toString(), new Object[] { sqlArr },
						new int[] { OracleTypes.ARRAY }, new ResultSetExtractor<List<OrgDetails>>() {

							@Override
							public List<OrgDetails> extractData(ResultSet rs) throws SQLException, DataAccessException {
								// TODO Auto-generated method stub
								List<OrgDetails> lst = new ArrayList<>();
								while (rs.next()) {
									OrgDetails nx = new OrgDetails();
									nx.load(rs);
									lst.add(nx);
								}
								return lst;
							}
						});
				if (orgDetails != null) {
					body.setOrg_details(orgDetails);
					((GetOrgDetailsRes) resMsg).setBody(body);
					resMsg.getHeader().setResult(new SeAResult("000"));
				}
			}
			resMsg.getHeader().setResult(new SeAResult("000"));

		} catch (Exception e) {
			SeAMail mail = new SeAMail();
			mail.setSubject("ManageApprover4OrgProcess-processBody-Exception");
			mail.setConten(e.toString());
			mailAdapter.alert(mail);
			SeAResult ers = new SeAResult("306");
			ers.setDetail(e.toString());
			resMsg.getHeader().setResult(ers);
		}

	}

	@SuppressWarnings("unchecked")
	private void processGetForExport(ReqMes reqMsg, ResMsg resMsg) {
		try {
			GetConfigApproveForOrgBodyReq bodyReq = ((GetConfigApproveForOrgExportReq) reqMsg).getBody();
			SimpleJdbcCall call = new SimpleJdbcCall(DataSources.orclAdapter.getTemplete());
			call.withCatalogName(ApiConfig.cfg.packagePermissionUIName)
					.withProcedureName("fetch_config_approve_4_org_ex")
					.addDeclaredParameter(new SqlInOutParameter("id_don_vi", OracleTypes.VARCHAR));
			call.addDeclaredParameter(new SqlInOutParameter("ma_module", OracleTypes.VARCHAR));
			call.addDeclaredParameter(new SqlOutParameter("out_config_4org_export", OracleTypes.ARRAY,
					"user_cfg_4_org_export_list".toUpperCase(),
					new SeAReturnType<UserConfigApproverForOrgExport>(UserConfigApproverForOrgExport.class)));
			Map<String, Object> in = new HashMap<>();
			in.put("id_don_vi", StringUtils.getString(bodyReq.getFilter().getId_don_vi()));
			in.put("ma_module", bodyReq.getFilter().getMa_chuc_nang());
			Map<String, Object> retMap = call.execute(in);
			Object listConfig = retMap.get("out_config_4org_export");
			GetConfigApproveForOrgExportBodyRes body = new GetConfigApproveForOrgExportBodyRes();
			if (listConfig != null) {
				body.setRows((List<UserConfigApproverForOrgExport>) listConfig);
			}
			((GetConfigApproveForOrgExportRes) resMsg).setBody(body);
			resMsg.getHeader().setResult(new SeAResult("000"));
		} catch (Exception e) {
			SeAMail mail = new SeAMail();
			mail.setSubject("ManageApprover4OrgProcess-processBody-Exception");
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
			mail.setSubject("ManageApprover4OrgProcess-processImportMethod-Exception");
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
			ValidImportConfigApproveForOrgBodyReq reqbody = ((ValidImportConfigApproverForOrgReq) reqMsg).getBody();
			List<UserUpdateConfigApproveForOrg> importList = reqbody.getRows();
			List<ValidUserConfigApproverForOrg> validList = new ArrayList<>();
			List<UserUpdateConfigApproveForOrg> validToDb = new ArrayList<>();
			for (UserUpdateConfigApproveForOrg vl : importList) {
				ValidUserConfigApproverForOrg vlTemp = new ValidUserConfigApproverForOrg(vl);
				if (!vlTemp.getValid().status) {
					validList.add(vlTemp);
				} else {
					validToDb.add(vl);
				}
			}

			if (validToDb.size() > 0) {
				sqlArr = DataSources.creatArraySql("update_cfg_4_org_list".toUpperCase(), validToDb);
				SimpleJdbcCall call = new SimpleJdbcCall(DataSources.orclAdapter.getTemplete());
				call.withCatalogName(ApiConfig.cfg.packagePermissionUIName)
						.withFunctionName("vl_import_cfg_approve_4_org");
				call.addDeclaredParameter(new SqlParameter("import_list", OracleTypes.ARRAY));
				call.declareParameters(
						new SqlOutParameter("return", OracleTypes.ARRAY, "valid_cfg_4_org_list".toUpperCase(),
								new SeAReturnType<ValidUserConfigApproverForOrg>(ValidUserConfigApproverForOrg.class)));
				Map<String, Object> param = new HashMap<>();
				param.put("import_list", sqlArr);
				List<ValidUserConfigApproverForOrg> validListRet = (List<ValidUserConfigApproverForOrg>) call
						.executeFunction(Object.class, param);
				validList.addAll(validListRet);
			}
			SeAResult mainRs = getMainResult(validList);
			ValidImportConfigApproveForOrgBodyRes resBody = new ValidImportConfigApproveForOrgBodyRes();
			String session = saveImportRequest(reqMsg.getCommand(), importList, reqbody.getImport_session(), mainRs,
					reqMsg.getHeader().getUserID());
			resBody.setImport_session(session);
			resBody.setRows((List<ValidUserConfigApproverForOrg>) validList);
			resBody.setValid(mainRs);
			((ValidImportConfigApproverForOrgRes) resMsg).setBody(resBody);
			resMsg.getHeader().setResult(new SeAResult("000"));
		} catch (Exception e) {
			SeAMail mail = new SeAMail();
			mail.setSubject("ManageApprover4OrgProcess-processValidMethod-Exception");
			mail.setConten(e.toString());
			mailAdapter.alert(mail);
			SeAResult ers = new SeAResult("306");
			ers.setDetail(e.toString());
			resMsg.getHeader().setResult(ers);
		} finally {
			DataSources.freeArray(sqlArr);
		}

	}

	private SeAResult getMainResult(List<ValidUserConfigApproverForOrg> valids) {
		SeAResult retVal = new SeAResult("000");
		if (valids != null) {
			for (ValidUserConfigApproverForOrg validItem : (List<ValidUserConfigApproverForOrg>) valids) {
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

	private String saveImportRequest(String Command, List<UserUpdateConfigApproveForOrg> validList,
			String import_session, SeAResult mainResult, String userName) {
		// TODO Auto-generated method stub
		String sesion = import_session;
		if (StringUtils.isEmpty(sesion)) {
			sesion = KeysUtils.generateKey("per-import", "for-org", 10);
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
			e.printStackTrace();
		}

		return sesion;
	}

	@SuppressWarnings("unchecked")
	private void processGetMethod(ReqMes reqMsg, ResMsg resMsg) {
		try {
			GetConfigApproveForOrgBodyReq bodyReq = ((GetConfigApproveForOrgReq) reqMsg).getBody();
			SimpleJdbcCall call = new SimpleJdbcCall(DataSources.orclAdapter.getTemplete());
			call.withCatalogName(ApiConfig.cfg.packagePermissionUIName).withProcedureName("fetch_config_approve_4_org")
					.addDeclaredParameter(new SqlInOutParameter("id_don_vi", OracleTypes.VARCHAR));
			call.addDeclaredParameter(new SqlInOutParameter("ma_module", OracleTypes.VARCHAR));
			call.addDeclaredParameter(
					new SqlOutParameter("out_config_4org", OracleTypes.ARRAY, "user_cfg_4_org_list".toUpperCase(),
							new SeAReturnType<UserConfigApproverForOrg>(UserConfigApproverForOrg.class)));
			Map<String, Object> in = new HashMap<>();
			in.put("id_don_vi", StringUtils.getString(bodyReq.getFilter().getId_don_vi()));
			in.put("ma_module", bodyReq.getFilter().getMa_chuc_nang());
			Map<String, Object> retMap = call.execute(in);
			Object listConfig = retMap.get("out_config_4org");
			GetConfigApproveForOrgBodyRes body = new GetConfigApproveForOrgBodyRes();
			if (listConfig != null) {
				body.setRows((List<UserConfigApproverForOrg>) listConfig);
			}
			((GetConfigApproveForOrgRes) resMsg).setBody(body);
			resMsg.getHeader().setResult(new SeAResult("000"));
		} catch (Exception e) {
			SeAMail mail = new SeAMail();
			mail.setSubject("ManageApprover4OrgProcess-processBody-Exception");
			mail.setConten(e.toString());
			mailAdapter.alert(mail);
			SeAResult ers = new SeAResult("306");
			ers.setDetail(e.toString());
			resMsg.getHeader().setResult(ers);
		}
	}

	private void processCreatMethod(ReqMes reqMsg, ResMsg resMsg) {
		try {
			UpdateConfigApproveForOrgBodyReq body = ((CreatConfigApproveForOrgReq) reqMsg).getBody();
			List<UserUpdateConfigApproveForOrg> updateList = body.getRows();
			CallRequest callRQ = new CallRequest("c_user_cfg_approve_for_org".toUpperCase());
			callRQ.setPackageName(ApiConfig.cfg.packagePermissionUIName);
			callRQ.addParam(new StoreParam("creat_list", OracleTypes.ARRAY,
					DataSources.creatArraySql("update_cfg_4_org_list".toUpperCase(), updateList)));
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
			SeAResult err = new SeAResult("306");
			err.setDetail(e.getMessage());
			((ResMsg) resMsg).getHeader().setResult(err);
			e.printStackTrace();
		}
	}

	private void processUpdateMethod(ReqMes reqMsg, ResMsg resMsg) {
		try {
			UpdateConfigApproveForOrgBodyReq body = ((UpdateConfigApproveForOrgReq) reqMsg).getBody();
			List<UserUpdateConfigApproveForOrg> updateList = body.getRows();
			Collections.sort(updateList, new Comparator<UserUpdateConfigApproveForOrg>() {

				@Override
				public int compare(UserUpdateConfigApproveForOrg o1, UserUpdateConfigApproveForOrg o2) {
					return o1.compareTo(o2);
				}
			});
			CallRequest callRQ = new CallRequest("u_user_cfg_approve_for_org".toUpperCase());
			callRQ.setPackageName(ApiConfig.cfg.packagePermissionUIName);
			callRQ.addParam(new StoreParam("update_list", OracleTypes.ARRAY,
					DataSources.creatArraySql("update_cfg_4_org_list".toUpperCase(), updateList)));
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
			SeAResult err = new SeAResult("306");
			err.setDetail(e.getMessage());
			((ResMsg) resMsg).getHeader().setResult(err);
			e.printStackTrace();
		}
	}

	private void processRemoveMethod(ReqMes reqMsg, ResMsg resMsg) {
		Array sqlArr = null;
		try {
			RemoveConfigApproveBodyReq body = ((RemoveConfigApproveReq) reqMsg).getBody();
			List<Integer> rowsRemove = body.getRows();
			if (rowsRemove.size() > 0) {
				sqlArr = DataSources.creatArraySql("config_id_list".toUpperCase(), rowsRemove);
				SimpleJdbcCall call = new SimpleJdbcCall(orclAdapter.getTemplete());
				call.withCatalogName(ApiConfig.cfg.packagePermissionUIName);
				call.withFunctionName("delete_cfg_approve".toUpperCase());
				call.declareParameters(new SqlParameter("list_id", OracleTypes.ARRAY, "config_id_list".toUpperCase()));
				call.declareParameters(new SqlParameter("in_user_name", OracleTypes.VARCHAR));
				Map<String, Object> in = new HashMap<>();
				in.put("list_id", sqlArr);
				in.put("in_user_name", reqMsg.getHeader().getUserID());
				call.executeFunction(Object.class, in);
			}
			resMsg.getHeader().setResult(new SeAResult("000"));
		} catch (Exception e) {
			SeAResult err = new SeAResult("306");
			err.setDetail(e.getMessage());
			((ResMsg) resMsg).getHeader().setResult(err);
			e.printStackTrace();
		} finally {
			DataSources.freeArray(sqlArr);
		}
	}
}
