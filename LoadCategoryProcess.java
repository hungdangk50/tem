package com.seabank.hrsb.process;

import java.util.List;

import com.seabank.hrsb.base.ReqMes;
import com.seabank.hrsb.base.ResMsg;
import com.seabank.hrsb.database.FetchRequest;
import com.seabank.hrsb.database.SFetchExtractor;
import com.seabank.hrsb.message.LoadCategoryReq;
import com.seabank.hrsb.message.LoadCategoryRes;
import com.seabank.hrsb.message.body.loadCategoryBodyRes;
import com.seabank.hrsb.model.CategoryFilter;
import com.seabank.hrsb.model.CategoryItem;
import com.seabank.hrsb.model.SeAResult;

public class LoadCategoryProcess extends AbstractProcess {

	@SuppressWarnings("unchecked")
	@Override
	public void processBody(ReqMes reqMsg, ResMsg resMsg) {
		CategoryFilter filter = ((LoadCategoryReq) reqMsg).getBody().getFilter();
		FetchRequest rq = new FetchRequest("CHAM_CONG_CATEGORY_ITEMS");
		rq.setFields("ITEM_VALUE,ITEM_LABEL");
		rq.setOrderBy("ITEM_SEQUENCE", "ASC");
		rq.filter = filter.getFilter();
		rq.setExtractor(new SFetchExtractor<CategoryItem>(CategoryItem.class));
		List<CategoryItem> items = (List<CategoryItem>) orclAdapter.fetch(rq);
		if (items != null) {
			loadCategoryBodyRes body = new loadCategoryBodyRes();
			body.setItems(items);
			((LoadCategoryRes) resMsg).setBody(body);
			resMsg.getHeader().setResult(new SeAResult("000"));
		} else {
			SeAResult ers = new SeAResult("307");
			ers.setDetail("Not Get Items");
			resMsg.getHeader().setResult(ers);
		}

	}

}
