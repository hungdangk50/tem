package com.seabank.hrsb.process;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import com.seabank.hrsb.base.ReqMes;
import com.seabank.hrsb.base.ResMsg;
import com.seabank.hrsb.database.CallRequest;
import com.seabank.hrsb.database.SFetchExtractor;
import com.seabank.hrsb.database.SFilter;
import com.seabank.hrsb.database.StoreParam;
import com.seabank.hrsb.message.CreatApproverReq;
import com.seabank.hrsb.message.EditApproverReq;
import com.seabank.hrsb.message.GetConfigReq;
import com.seabank.hrsb.message.GetConfigRes;
import com.seabank.hrsb.message.RemoveApproverReq;
import com.seabank.hrsb.message.body.GetConfigBodyRes;
import com.seabank.hrsb.model.ApproverConfig;
import com.seabank.hrsb.model.ChangeLog;
import com.seabank.hrsb.model.ConfigFilter;
import com.seabank.hrsb.model.SeAMail;
import com.seabank.hrsb.model.SeAModel;
import com.seabank.hrsb.model.SeAResult;
import com.seabank.hrsb.utils.AppLogger;
import com.seabank.hrsb.utils.StringUtils;

import oracle.jdbc.OracleTypes;

public class ConfigApproveProcess extends AbstractProcess {

	@SuppressWarnings("unchecked")
	@Transactional(rollbackFor = Exception.class)
	@Override
	public void processBody(ReqMes reqMsg, ResMsg resMsg) {
		try {
			if (reqMsg instanceof GetConfigReq) {
				ConfigFilter filter = ((GetConfigReq) reqMsg).getBody().getFilter();
				CallRequest callRQ = new CallRequest("HR_CHAM_CONG_GET_APPROVER_CFG");
				callRQ.addParam(new StoreParam("IN_MA_CHUC_NANG", OracleTypes.VARCHAR, filter.getMa_chuc_nang()));
				callRQ.addParam(new StoreParam("IN_MA_TOCHUC", OracleTypes.VARCHAR, filter.getMa_loai_tochuc()));
				callRQ.addParam(new StoreParam("APP_CONFIG_CUR", OracleTypes.CURSOR, null,
						new SFetchExtractor<ApproverConfig>(ApproverConfig.class)));
				Map<String, Object> ret = orclAdapter.call(callRQ);
				List<ApproverConfig> lst = (List<ApproverConfig>) ret.get("APP_CONFIG_CUR");
				GetConfigBodyRes body = new GetConfigBodyRes();
				body.setRows(lst);
				((GetConfigRes) resMsg).setBody(body);
				resMsg.getHeader().setResult(new SeAResult("000"));
			} else if (reqMsg instanceof EditApproverReq) {
				SeAResult rsCheck = reqMsg.valid();
				if (rsCheck.status) {
					List<ApproverConfig> aps = ((EditApproverReq) reqMsg).getBody().getRows();
					for (ApproverConfig approverConfig : aps) {
						ChangeLog changeLog = approverConfig.checkChange(orclAdapter);
						if (changeLog.isChange()) {
							SeAResult rsupdate = approverConfig.update(orclAdapter);
							if (rsupdate.status) {
								Map<String, Object> params = new HashMap<String, Object>();
								params.put("MODEL_NAME", approverConfig.getModelName());
								params.put("TIME_UPDATE", new Timestamp(System.currentTimeMillis()));
								params.put("USER_UPDATE", reqMsg.getHeader().getUserID());
								params.put("CONTEN_UPDATE", changeLog.getLogString());
								params.put("ACTION_NAME", "UPDATE");
								orclAdapter.insert("CHAM_CONG_HISTORY", params);
							}
						} else {
							AppLogger.LOGGER.info("ConfigApproveProcess-Update->Not Update because Not Change");
						}
					}
					resMsg.getHeader().setResult(new SeAResult("000"));
				} else {
					resMsg.getHeader().setResult(rsCheck);
				}
			} else if (reqMsg instanceof CreatApproverReq) {
				SeAResult rsCheck = reqMsg.valid();
				if (rsCheck.status) {
					List<ApproverConfig> aps = ((CreatApproverReq) reqMsg).getBody().getRows();
					for (ApproverConfig approverConfig : aps) {
						approverConfig.creat(orclAdapter);
					}
					resMsg.getHeader().setResult(new SeAResult("000"));
				} else {
					resMsg.getHeader().setResult(rsCheck);
				}
			} else if (reqMsg instanceof RemoveApproverReq) {
				SeAResult rsCheck = reqMsg.valid();
				if (rsCheck.status) {
					List<String> ids = ((RemoveApproverReq) reqMsg).getBody().getRows();
					for (String id : ids) {
						SFilter filter = new SFilter();
						filter.apendClause("ID", Integer.valueOf(id));
						SeAModel oldata = new ApproverConfig(id).getCurrentModel(orclAdapter);
//						if (orclAdapter.delete(oldata.getModelName(), filter)) {
//							Map<String, Object> params = new HashMap<String, Object>();
//							params.put("MODEL_NAME", oldata.getModelName());
//							params.put("TIME_UPDATE", new Timestamp(System.currentTimeMillis()));
//							params.put("USER_UPDATE", reqMsg.getHeader().getUserID());
//							params.put("CONTEN_UPDATE", StringUtils.objectToJson(oldata));
//							params.put("ACTION_NAME", "DELETE");
//							orclAdapter.insert("CHAM_CONG_HISTORY", params);
//						}
					}
					resMsg.getHeader().setResult(new SeAResult("000"));
				} else {
					resMsg.getHeader().setResult(rsCheck);
				}

			}
		} catch (Exception e) {
			SeAMail mail = new SeAMail();
			mail.setSubject("ConfigApproveProcess-processBody-Exception");
			mail.setConten(e.toString());
			mailAdapter.alert(mail);
			SeAResult ers = new SeAResult("306");
			ers.setDetail(e.getMessage());
			resMsg.getHeader().setResult(ers);
		}
	}

}
