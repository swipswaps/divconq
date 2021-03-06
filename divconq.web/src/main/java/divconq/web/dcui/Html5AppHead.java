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
package divconq.web.dcui;

import divconq.hub.DomainInfo;
import divconq.hub.Hub;
import divconq.lang.op.OperationContext;
import divconq.util.StringUtil;
import divconq.web.WebContext;
import divconq.xml.XElement;
import w3.html.Head;
import w3.html.Link;
import w3.html.Meta;
import w3.html.Script;
import w3.html.Style;
import w3.html.Title;

/*
 * TODO these libs are not in this project - fix that
 * 
 * <Html5Head Title="NNN" Public="[T]/F" Mobile="[T]/F" Icon="/dcm/Asset/icon" Include="dcCore,dcManager,jQueryMobile">
 * 		<Keywords></Keywords>
 * 		<Description></Description>
 * </Html5Head>
 * 
 * see also http://wiki.whatwg.org/wiki/MetaExtensions
 * 		rating
 * 		
 * 
 */
public class Html5AppHead extends Head implements ICodeTag {
	protected divconq.xml.XElement source = null;
	
    public Html5AppHead() {
    	super();
	}
	
    public Html5AppHead(divconq.xml.XElement source, Object... args) {
    	super(args);
    	this.source = source;
	}

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		this.source = xel;
        this.myArguments = new Object[] { attrs, ctx.getDomain().parseXml(ctx, xel) };
		
		nodes.add(this);
	}
	
	@Override
	public void doBuild(WebContext ctx) {
		Nodes earlyadditional = new Nodes();
		Nodes lateadditional = new Nodes();
		
		// TODO some of this can move to compile phase
		
		earlyadditional.add(
				new Meta(new Attributes("chartset", "utf-8")),
				new Meta(new Attributes("name", "format-detection", "content", "telephone=no"))
		);
		
		if ("False".equals(this.source.getAttribute("Public")))
			earlyadditional.add(
					new Meta(new Attributes("name", "robots", "content", "noindex,nofollow"))
			);
		else
			earlyadditional.add(
					new Meta(new Attributes("name", "robots", "content", "index,follow"))
			);
		
		earlyadditional.add(
				new Meta(new Attributes("name", "viewport", "content", "width=device-width, initial-scale=1, maximum-scale=1.0, user-scalable=no"))
		);
		
		String title = "@ctx|PageTitle@ - @ctx|SiteTitle@";
		
		if (StringUtil.isNotEmpty(title))
			earlyadditional.add(
					new Title(title)
			);
		
		XElement domainwebconfig = null;
		
		String did = OperationContext.get().getUserContext().getDomainId();
		
		if (StringUtil.isNotEmpty(did)) {
			DomainInfo domain = Hub.instance.getDomainInfo(did);
			
			XElement domainconfig = domain.getSettings();
			
			if (domainconfig!= null) 
				domainwebconfig = domainconfig.selectFirst("Web");
		}
		
		String icon = this.source.getRawAttribute("Icon");
		
		if (StringUtil.isEmpty(icon))
			icon = this.source.getRawAttribute("Icon16");
		
		if (StringUtil.isEmpty(icon) && (domainwebconfig != null))
			icon = domainwebconfig.getRawAttribute("Icon");
		
		if (StringUtil.isNotEmpty(icon)) { 
			// if full name then use as the 16x16 version
			if (icon.endsWith(".png")) {
				earlyadditional.add(
						new Link(new Attributes("type", "image/png", "rel", "shortcut icon", "href", icon)),
						new Link(new Attributes("sizes", "16x16", "rel", "icon", "href", icon))
				);
			}
			else {
				earlyadditional.add(
						new Link(new Attributes("type", "image/png", "rel", "shortcut icon", "href", icon + "16.png")),
						new Link(new Attributes("sizes", "16x16", "rel", "icon", "href", icon + "16.png")),
						new Link(new Attributes("sizes", "32x32", "rel", "icon", "href", icon + "32.png")),
						new Link(new Attributes("sizes", "48x48", "rel", "icon", "href", icon + "48.png")),
						new Link(new Attributes("sizes", "64x64", "rel", "icon", "href", icon + "64.png")),
						new Link(new Attributes("sizes", "128x128", "rel", "icon", "href", icon + "128.png"))
				);
			}
		}
		
		icon = this.source.getRawAttribute("Icon32");
		
		if (StringUtil.isNotEmpty(icon)) { 
			earlyadditional.add(
					new Link(new Attributes("sizes", "32x32", "rel", "icon", "href", icon))
			);
		}
		
		icon = this.source.getRawAttribute("Icon64");
		
		if (StringUtil.isNotEmpty(icon)) { 
			earlyadditional.add(
					new Link(new Attributes("sizes", "64x64", "rel", "icon", "href", icon))
			);
		}
		
		divconq.xml.XElement del = this.source.find("Description");
		String desc = (del != null) ? del.getText() : "@ctx|SiteDescription@";
		
		if (StringUtil.isNotEmpty(desc))
			earlyadditional.add(
					new Meta(new Attributes("name", "description", "content", desc))
			);
		
		divconq.xml.XElement kel = this.source.find("Keywords");
		String keywords = (kel != null) ? kel.getText() : "@ctx|SiteKeywords@";
		
		if (StringUtil.isNotEmpty(keywords))
			earlyadditional.add(
					new Meta(new Attributes("name", "keywords", "content", keywords))
			);
		
		earlyadditional.add(
				new Style("/dcw/css/normalize.css"),
				new Style("/dcw/css/font-awesome.min.css"),

				new Style("/dcw/css/jquery.mobile-1.4.5.cust.css"),
				new Style("/dcw/css/jqm-icon-pack-ltd.css"),		// TODO need a better way to generalize
				new Style("/dcw/css/app.css"),
				new Style("/dcw/css/app-specific.css"),
				
				new Script("/dcw/js/aes.js"),
				//new Script("/dcw/js/openpgp.min.js"),
				new Script("/dcw/js/moment.min.js"),
				new Script("/dcw/js/numeral/numeral.min.js"),
				new Script("/dcw/js/numeral/languages.min.js"),
				new Script("/dcw/js/jquery-2.1.1.min.js"),
				new Script("/dcw/js/jquery-validate/jquery.validate.js"),
				new Script("/dcw/js/jquery-validate/additional-methods.js"),
				new Script("/dcw/js/dc.lang.js"),
				new Script("/dcw/js/dc.schema.js"),
				new Script("/dcw/js/schema.js"),		// TODO replace with /schema
				new Script("/dcw/js/dc.user.js"),
				new Script("/dcw/js/dc.comm.js"),
				new Script("/dcw/js/dc.ui.js"),
				new Script("/dcw/js/dc.handler.js"),
				new Script("/dcw/js/jquery.mobile-1.4.5.cust.js")
		);
		
		if (domainwebconfig != null) {
			for (XElement gel : domainwebconfig.selectAll("Global")) {
				if (gel.hasAttribute("Style"))
					earlyadditional.add(new Style(gel.getAttribute("Style")));
				else
					earlyadditional.add(new Script(gel.getAttribute("Script")));
			}
		}
		
		// additional scripts/styles
		
		//lateadditional.add(new IncludeHolder("Styles"));
		
		//lateadditional.add(new IncludeHolder("Scripts"));
		
	    this.build(ctx, earlyadditional, this.myArguments, lateadditional);
	}
}
