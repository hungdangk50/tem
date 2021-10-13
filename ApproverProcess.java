package com.seabank.hrsb.process;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnType;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

import com.seabank.hrsb.base.ReqMes;
import com.seabank.hrsb.base.ResMsg;
import com.seabank.hrsb.cache.CacheItem;
import com.seabank.hrsb.cache.CacheKey;
import com.seabank.hrsb.cache.SeACache;
import com.seabank.hrsb.constant.ApiConfig;
import com.seabank.hrsb.database.DataSources;
import com.seabank.hrsb.mail.SBStaff;
import com.seabank.hrsb.message.ApproverForStaffReq;
import com.seabank.hrsb.message.ApproverForStaffRes;
import com.seabank.hrsb.message.ApproverPermissionReq;
import com.seabank.hrsb.message.ApproverPermissionRes;
import com.seabank.hrsb.message.ApproverYourSelfReq;
import com.seabank.hrsb.message.ApproverYourSelfRes;
import com.seabank.hrsb.message.NextApproverReq;
import com.seabank.hrsb.message.NextApproverRes;
import com.seabank.hrsb.message.Staff4ApproveReq;
import com.seabank.hrsb.message.Staff4ApproveRes;
import com.seabank.hrsb.message.body.ApproverForStaffReqBody;
import com.seabank.hrsb.message.body.ApproverForStaffResBody;
import com.seabank.hrsb.message.body.ApproverPermissionBodyRes;
import com.seabank.hrsb.message.body.ApproverYourSelfBodyReq;
import com.seabank.hrsb.message.body.ApproverYourSelfBodyRes;
import com.seabank.hrsb.message.body.NextApproverBodyReq;
import com.seabank.hrsb.message.body.NextApproverBodyRes;
import com.seabank.hrsb.message.body.Staff4ApproveBodyRes;
import com.seabank.hrsb.message.body.Staff4ApproverBodyReq;
import com.seabank.hrsb.model.Approver4Staff;
import com.seabank.hrsb.model.NextSBApprover;
import com.seabank.hrsb.model.SBApprove;
import com.seabank.hrsb.model.SeAMail;
import com.seabank.hrsb.model.SeAResult;
import com.seabank.hrsb.model.Staff4Approver;
import com.seabank.hrsb.utils.SQlDataUtils;
import com.seabank.hrsb.utils.StringUtils;

import oracle.jdbc.OracleTypes;

public class ApproverProcess extends AbstractProcess {

	@Override
	public void processBody(ReqMes reqMsg, ResMsg resMsg) {
		try {
			if (reqMsg instanceof ApproverPermissionReq) {
				String moduleName = ((ApproverPermissionReq) reqMsg).getBody().getModule_name();
				String userName = reqMsg.getHeader().getUserID();
				CacheKey key = new CacheKey(userName, moduleName);
				ApproverPermissionBodyRes bodyRes = null;
				CacheItem cache = SeACache.approveDetailCaches.get(key);
				if (cache == null) {
					bodyRes = new ApproverPermissionBodyRes();
				} else {
					bodyRes = (ApproverPermissionBodyRes) cache.getValue();
				}
				resMsg.getHeader().setResult(new SeAResult("000"));
				((ApproverPermissionRes) resMsg).setBody(bodyRes);
			} else if (reqMsg instanceof NextApproverReq) {
				processNextApprover(reqMsg, resMsg);
			} else if (reqMsg instanceof ApproverYourSelfReq) {
				processApproverYourself(reqMsg, resMsg);
			} else if (reqMsg instanceof Staff4ApproveReq) {
				processStaff4Approve(reqMsg, resMsg);
			} else if (reqMsg instanceof ApproverForStaffReq) {
				processApprove4Staff(reqMsg, resMsg);
			}
		} catch (Exception e) {
			SeAMail mail = new SeAMail();
			mail.setSubject("ApproveDetailProcess-processBody-Exception");
			mail.setConten(e.toString());
			mailAdapter.alert(mail);
			SeAResult ers = new SeAResult("306");
			ers.setDetail(e.toString());
			resMsg.getHeader().setResult(ers);
		}
	}

	private void processApprove4Staff(ReqMes reqMsg, ResMsg resMsg) {
		ApproverForStaffResBody body = null;
		try {
			body = new ApproverForStaffResBody();
			ApproverForStaffReqBody bodyReq = ((ApproverForStaffReq) reqMsg).getBody();
			String maChucNang = bodyReq.getMa_chuc_nang();
			List<SBStaff> sbList = bodyReq.getSb_list();
			Array SqlData = DataSources.creatArraySql("staff_req_list".toUpperCase(), sbList);
			List<Approver4Staff> approver4Staff = null;
			try {
				StringBuilder sql = new StringBuilder("select * from Table(")
						.append(ApiConfig.cfg.packagePermissionName).append(".get_approve_info_for_staff(?,?))");
//				String sql = "select * from Table(cham_cong_permission.get_approve_info_for_staff(?,?))";
				approver4Staff = DataSources.orclAdapter.getTemplete().query(sql.toString(),
						new Object[] { maChucNang, SqlData }, new int[] { OracleTypes.NVARCHAR, OracleTypes.ARRAY },
						new ResultSetExtractor<List<Approver4Staff>>() {

							@Override
							public List<Approver4Staff> extractData(ResultSet rs)
									throws SQLException, DataAccessException {
								// TODO Auto-generated method stub
								Map<String, Approver4Staff> mapStaff = new HashMap<>();
								while (rs.next()) {
									String sb_staff = rs.getString("sb_staff");
									if (!StringUtils.isEmpty(sb_staff)) {
										if (!mapStaff.containsKey(sb_staff)) {
											mapStaff.put(sb_staff, new Approver4Staff(sb_staff));
										}
										mapStaff.get(sb_staff).load(rs);
									}
								}
								List<Approver4Staff> retVal = new ArrayList<>();
								retVal.addAll(mapStaff.values());
								return retVal;
							}
						});
			} catch (Exception e) {
				SeAResult ers = new SeAResult("306");
				ers.setDetail(e.toString());
				resMsg.getHeader().setResult(ers);
			} finally {
				if (SqlData != null) {
					SqlData.free();
				}
			}
			if (approver4Staff != null) {
				body.setApprovers(approver4Staff);
				((ApproverForStaffRes) resMsg).setBody(body);
				resMsg.getHeader().setResult(new SeAResult("000"));
			}
		} catch (Exception e) {
			SeAResult ers = new SeAResult("306");
			ers.setDetail(e.toString());
			resMsg.getHeader().setResult(ers);
		}
	}

//	@SuppressWarnings("unchecked")
//	private void processStaff4Approve(ReqMes reqMsg, ResMsg resMsg) {
//		try {
//			Staff4ApproverBodyReq bodyReq = ((Staff4ApproveReq) reqMsg).getBody();
//			Staff4ApproveBodyRes bodyRes = new Staff4ApproveBodyRes();
//			CallRequest callRQ = new CallRequest("SP_GET_EMP_APPROVAL");
//			callRQ.addParam(new StoreParam("V_ORG_ID", OracleTypes.VARCHAR, bodyReq.getFilter().getOrg_id()));
//			callRQ.addParam(new StoreParam("V_JOBTYPE_ID", OracleTypes.NUMBER,
//					StringUtils.getIntegerObject(bodyReq.getFilter().getJob_type_id())));
//			callRQ.addParam(new StoreParam("V_JOBTITLE_ID", OracleTypes.NUMBER,
//					StringUtils.getIntegerObject(bodyReq.getFilter().getJob_title_id())));
//			callRQ.addParam(new StoreParam("RS", OracleTypes.CURSOR, null,
//					new SFetchExtractor<Staff4Approver>(Staff4Approver.class)));
//			Map<String, Object> ret = orclAdapter.call(callRQ);
//			Object datas = ret.get("RS");
//			if (datas != null) {
//				bodyRes.setDatas((List<Staff4Approver>) datas);
//				((Staff4ApproveRes) resMsg).setBody(bodyRes);
//				resMsg.getHeader().setResult(new SeAResult("000"));
//			} else {
//				SeAResult ers = new SeAResult("306");
//				ers.setDetail("Data Null");
//				resMsg.getHeader().setResult(ers);
//			}
//		} catch (Exception e) {
//			SeAMail mail = new SeAMail();
//			mail.setSubject("processStaff4Approve-processBody-Exception");
//			mail.setConten(e.toString());
//			mailAdapter.alert(mail);
//			SeAResult ers = new SeAResult("306");
//			ers.setDetail(e.toString());
//			resMsg.getHeader().setResult(ers);
//		}
//	}

	@SuppressWarnings("unchecked")
	private void processStaff4Approve(ReqMes reqMsg, ResMsg resMsg) {
		try {
			Staff4ApproverBodyReq bodyReq = ((Staff4ApproveReq) reqMsg).getBody();
			Staff4ApproveBodyRes bodyRes = new Staff4ApproveBodyRes();
			List<Staff4Approver> datas = null;
			StringBuilder sql = new StringBuilder("select * from Table(").append(ApiConfig.cfg.packagePermissionName)
					.append(".f_get_staff_for_approve(?,?,?))");
			datas = DataSources.orclAdapter.getTemplete().query(sql.toString(),
					new Object[] { bodyReq.getFilter()
							.getOrg_id(),
							StringUtils.getIntegerObject(bodyReq.getFilter().getJob_type_id()),
							StringUtils.getIntegerObject(bodyReq.getFilter().getJob_title_id()) },
					new int[] { OracleTypes.VARCHAR,
							OracleTypes.NUMBER, OracleTypes.NUMBER },
					new ResultSetExtractor<List<Staff4Approver>>() {
						@Override
						public List<Staff4Approver> extractData(ResultSet rs) throws SQLException, DataAccessException {
							// TODO Auto-generated method stub
							List<Staff4Approver> lst = new ArrayList<>();
							while (rs.next()) {
								Staff4Approver nx = new Staff4Approver();
								nx.load(rs);
								lst.add(nx);
							}
							return lst;
						}
					});
			if (datas != null) {
				bodyRes.setDatas((List<Staff4Approver>) datas);
				((Staff4ApproveRes) resMsg).setBody(bodyRes);
				resMsg.getHeader().setResult(new SeAResult("000"));
			} else {
				SeAResult ers = new SeAResult("306");
				ers.setDetail("Data Null");
				resMsg.getHeader().setResult(ers);
			}
		} catch (Exception e) {
			SeAMail mail = new SeAMail();
			mail.setSubject("processStaff4Approve-processBody-Exception");
			mail.setConten(e.toString());
			mailAdapter.alert(mail);
			SeAResult ers = new SeAResult("306");
			ers.setDetail(e.toString());
			resMsg.getHeader().setResult(ers);
		}
	}

	private void processApproverYourself(ReqMes reqMsg, ResMsg resMsg) {
		ApproverYourSelfBodyRes body = null;
		try {
			ApproverYourSelfBodyReq bodyReq = ((ApproverYourSelfReq) reqMsg).getBody();
			String module_name = bodyReq.getModule_name();
			String userLogin = reqMsg.getHeader().getUserID();

			SimpleJdbcCall call = new SimpleJdbcCall(orclAdapter.getTemplete());
			call.withCatalogName(ApiConfig.cfg.packagePermissionName).withFunctionName("get_approver_yourself");
			call.addDeclaredParameter(new SqlParameter("user_login", OracleTypes.VARCHAR));
			call.addDeclaredParameter(new SqlParameter("function_code", OracleTypes.VARCHAR));
			call.declareParameters(
					new SqlOutParameter("return", OracleTypes.STRUCT, "APPROVE_YOURSELF", new SqlReturnType() {

						@Override
						public Object getTypeValue(CallableStatement cs, int paramIndex, int sqlType, String typeName)
								throws SQLException {
							Object retVal = null;
							Object obStruct = cs.getObject(paramIndex);
							if (obStruct != null && obStruct instanceof Struct) {
								try {
									retVal = SQlDataUtils.readStruct((Struct) obStruct, ApproverYourSelfBodyRes.class);
								} catch (InstantiationException e) {
									retVal = null;
								} catch (IllegalAccessException e) {
									retVal = null;
								} catch (SQLException e) {
									retVal = null;
								}
							}
							return retVal;
						}
					}));
			Map<String, Object> inParam = new HashMap<>();
			inParam.put("user_login", userLogin);
			inParam.put("function_code", module_name);
			Map<String, Object> retMap = call.execute(inParam);
			body = (ApproverYourSelfBodyRes) retMap.get("return");
			if (body != null) {
				((ApproverYourSelfRes) resMsg).setBody(body);
				resMsg.getHeader().setResult(new SeAResult("000"));
			} else {
				SeAResult ers = new SeAResult("306");
				ers.setDetail("Return is Null");
				resMsg.getHeader().setResult(ers);
			}
		} catch (Exception e) {
			SeAResult ers = new SeAResult("306");
			ers.setDetail(e.toString());
			resMsg.getHeader().setResult(ers);
		}
	}

	private void processNextApprover(ReqMes reqMsg, ResMsg resMsg) {
		NextApproverBodyRes body = null;
		try {
			body = new NextApproverBodyRes();
			NextApproverBodyReq bodyReq = ((NextApproverReq) reqMsg).getBody();
			String maChucNang = bodyReq.getMa_chuc_nang();
			List<SBApprove> sbList = bodyReq.getSb_list();
			Array SqlData = DataSources.creatArraySql("APPROVE_REQ_LIST", sbList);
			List<NextSBApprover> nextApprovers = null;
			try {
				StringBuilder sql = new StringBuilder("select * from Table(")
						.append(ApiConfig.cfg.packagePermissionName).append(".get_next_approver(?,?))");
//				String sql = "select * from Table(cham_cong_permission.get_next_approver(?,?))";
				nextApprovers = DataSources.orclAdapter.getTemplete().query(sql.toString(),
						new Object[] { maChucNang, SqlData }, new int[] { OracleTypes.VARCHAR, OracleTypes.ARRAY },
						new ResultSetExtractor<List<NextSBApprover>>() {

							@Override
							public List<NextSBApprover> extractData(ResultSet rs)
									throws SQLException, DataAccessException {
								// TODO Auto-generated method stub
								List<NextSBApprover> lst = new ArrayList<>();
								while (rs.next()) {
									NextSBApprover nx = new NextSBApprover();
									nx.load(rs);
									lst.add(nx);
								}
								return lst;
							}
						});
			} catch (Exception e) {
				SeAResult ers = new SeAResult("306");
				ers.setDetail(e.toString());
				resMsg.getHeader().setResult(ers);
			} finally {
				if (SqlData != null) {
					SqlData.free();
				}
			}
			if (nextApprovers != null) {
				body.setDatas(nextApprovers);
				((NextApproverRes) resMsg).setBody(body);
				resMsg.getHeader().setResult(new SeAResult("000"));
			}
		} catch (Exception e) {
			SeAResult ers = new SeAResult("306");
			ers.setDetail(e.toString());
			resMsg.getHeader().setResult(ers);
		}
	}
}
