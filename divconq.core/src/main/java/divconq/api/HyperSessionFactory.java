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

import divconq.xml.XElement;

public class HyperSessionFactory implements IApiSessionFactory {
	protected XElement config = null;
	
	@Override
	public void init(XElement config) {
		this.config = config;
	}

	@Override
	public ApiSession create() {
		HyperSession sess = new HyperSession();
		sess.init(this.config);
		return sess;
	}

	@Override
	public ApiSession create(XElement config) {
		HyperSession sess = new HyperSession();
		sess.init(config);
		return sess;
	}

}
