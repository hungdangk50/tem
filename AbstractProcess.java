package com.seabank.hrsb.process;

import org.springframework.beans.factory.annotation.Autowired;

import com.seabank.hrsb.adapter.DBAdapter;
import com.seabank.hrsb.adapter.MailAdapter;
import com.seabank.hrsb.base.ReqHeader;
import com.seabank.hrsb.base.ReqMes;
import com.seabank.hrsb.base.ResMsg;
import com.seabank.hrsb.database.CountRequest;
import com.seabank.hrsb.model.SeAMail;
import com.seabank.hrsb.model.SeAResult;
import com.seabank.hrsb.utils.AppLogger;
import com.seabank.hrsb.utils.StringUtils;

public abstract class AbstractProcess {

	@Autowired
	protected DBAdapter orclAdapter;
	@Autowired
	protected DBAdapter pgAdapter;

	@Autowired
	protected MailAdapter mailAdapter;

	public abstract void processBody(ReqMes reqMsg, ResMsg resMsg);

	public void execute(ReqMes reqMsg, ResMsg resMsg) {
		String traceID = null;
		long timeBegin = System.currentTimeMillis();
		try {
			SeAResult checkTraceID = checkUniqueReq(reqMsg);
			if (checkTraceID.status) {
				traceID = reqMsg.getHeader().getTraceid();
				AppLogger.LOGGER.info("Start-Process-->" + reqMsg.getCommand() + "|" + traceID);
				Number reqID = logRequest(reqMsg);
				if (reqID != null) {
					SeAResult valid = reqMsg.valid();
					if (valid.status) {
						processBody(reqMsg, resMsg);
					} else {
						resMsg.getHeader().setResult(valid);
					}
					logResponse(reqID.intValue(), resMsg);
				} else {
					SeAResult ers = new SeAResult("306");
					ers.setDetail("Request ID is Null");
					resMsg.getHeader().setResult(ers);
				}
				long timeExe = System.currentTimeMillis() - timeBegin;
				AppLogger.LOGGER
						.info("End-Process-->" + reqMsg.getCommand() + "|" + traceID + " | TimeProcess = " + timeExe);
			} else {
				resMsg.getHeader().setResult(checkTraceID);
			}

		} catch (Exception e) {
			SeAMail mail = new SeAMail();
			mail.setSubject("AbstractProcess-execute-Exception");
			mail.setConten(e.getMessage());
			mailAdapter.alert(mail);
			SeAResult ers = new SeAResult("306");
			ers.setDetail(e.toString());
			resMsg.getHeader().setResult(ers);
			AppLogger.LOGGER.error("execute-Exception-->" + e.toString());
		}
	}

	protected Number logRequest(ReqMes mes) {
		return pgAdapter.insertLogRequest(mes);
	}

	protected void logResponse(int reqID, ResMsg mes) {
		pgAdapter.insertLogResponse(reqID, mes);
	}

	protected SeAResult checkUniqueReq(ReqMes reqMes) {
		SeAResult res = null;
		ReqHeader header = reqMes.getHeader();
		if (header != null) {
			String traceID = header.getTraceid();
			if (!StringUtils.isEmpty(traceID)) {
				CountRequest countReq = new CountRequest("tbl_request");
				countReq.setField("id");
				countReq.filter.apendClause("trace_id", traceID);
				int check = pgAdapter.count(countReq);
				if (check == 0) {
					res = new SeAResult("000");
				} else if (check > 0) {
					res = new SeAResult("010");
					res.setDetail(traceID);
				} else {
					res = new SeAResult("306");
					res.setDetail("Check TraceId error");
				}
			} else {
				res = new SeAResult("012");
				res.setDetail(traceID + "");
			}
		} else {
			res = new SeAResult("161");
			res.setDetail("header is Null");
		}
		return res;
	}
}
