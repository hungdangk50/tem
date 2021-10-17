package com.seabank.hrsb.mail;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.seabank.hrsb.constant.ApiConfig;
import com.seabank.hrsb.database.DataSources;
import com.seabank.hrsb.model.Approver4Mail;
import com.seabank.hrsb.model.NextSBApprover;
import com.seabank.hrsb.utils.AppLogger;
import com.seabank.hrsb.utils.JsonUtils;
import com.seabank.hrsb.utils.StringUtils;

import oracle.jdbc.OracleTypes;

public class ProcessEmail4Approve implements ProcessEmailData {

	@Override
	public Map<String, List<MailData>> getToAddress(MailRequest req) {
		Map<String, List<MailData>> retVal = new HashMap<>();
		AppLogger.LOGGER.debug(req.toString() + " -valid Data->" + JsonUtils.object2jsonString(req.getExe_datas()));
		req.checkBeforeSend();
		for (MailData mailData : req.getExe_datas()) {
			AppLogger.LOGGER.debug(
					req.toString() + " - Get Address for Data->" + JsonUtils.object2jsonString(mailData.getApprover()));
			List<NextSBApprover> lstNextApprover = getNextApprover(req.getMail_type(), mailData.getApprover());
			if (lstNextApprover != null) {
				for (NextSBApprover na : lstNextApprover) {
					String email = na.getNext_approver().getApprover_email();
					if (StringUtils.isEmpty(email)) {
						email = "NOTVALID";
					}
					if (!retVal.containsKey(email)) {
						retVal.put(email, new ArrayList<>());
					}
					retVal.get(email).add(mailData);
				}
			}
		}
		return retVal;
	}

	private List<NextSBApprover> getNextApprover(String mailType, List<Approver4Mail> approver) {
		List<NextSBApprover> retVal = null;
		Array SqlData = null;
		try {
			if ("M_DILAMLAI".equals(mailType)) {
				mailType = "M_NGHI_VANG";
			}
			SqlData = DataSources.creatArraySql("APPROVE_REQ_LIST", approver);
			StringBuilder sql = new StringBuilder("select * from Table(").append(ApiConfig.cfg.packagePermissionName)
					.append(".get_next_approver(?,?))");
//			String sql = "select * from Table(cham_cong_permission.get_next_approver(?,?))";
			retVal = DataSources.orclAdapter.getTemplete().query(sql.toString(), new Object[] { mailType, SqlData },
					new int[] { OracleTypes.VARCHAR, OracleTypes.ARRAY },
					new ResultSetExtractor<List<NextSBApprover>>() {

						@Override
						public List<NextSBApprover> extractData(ResultSet rs) throws SQLException, DataAccessException {
							List<NextSBApprover> nx = new ArrayList<>();
							while (rs.next()) {
								NextSBApprover ab = new NextSBApprover();
								ab.load(rs);
								nx.add(ab);
							}
							return nx;
						}
					});
		} catch (Exception e) {
			AppLogger.LOGGER.error("ProcessEmail4Approve-getNextApprover-Ex-->>" + e.getMessage());
		} finally {
			DataSources.freeArray(SqlData);
		}
		return retVal;
	}

}
