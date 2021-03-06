/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2014 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package divconq.api;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import divconq.api.ClientInfo.ConnectorKind;
import divconq.bus.Message;
import divconq.lang.op.OperationResult;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class WebSession extends HyperSession {
	@Override
	public void sendForgetMessage(Message msg) {
		super.sendForgetMessage(msg);
	}
	
	@Override
	public void sendMessage(Message msg, ServiceResult callback) {
		OperationResult or = this.connect();
		
		if (or.hasErrors()) {
			callback.complete();
			return;
		}
		
		callback.setSession(this);
		
		this.replies.registerForReply(msg, callback);

    	this.handler.send(msg);
    }

	@Override
	public void thawContext(Message result) {
		super.thawContext(result);
		
		// make sure Session is in Cookies (client handler) too, so that session can work with upload and download
		if (StringUtil.isNotEmpty(this.sessionid)) {
			//Cookie k = new DefaultCookie("SessionId", this.sessionid + "_" + this.sessionKey);
			Cookie k = new DefaultCookie("dcSessionId", this.sessionid + "_XXX");		// TODO resolve this
			this.handler.getCookies().put("dcSessionId", k);
		}
	}

	@Override
    public void init(XElement config) {
    	// run only once even if call multiple times
		if (this.info != null)
			return;

		super.init(config);
		
		if (this.info != null)
			this.info.kind = ConnectorKind.WebSocket;
	}
}
