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
package divconq.view.json;

import java.util.HashMap;
import java.util.Map;

import divconq.view.Element;
import divconq.view.Fragment;
import divconq.view.Node;
import divconq.view.Nodes;
import divconq.web.IContentBuilder;
import divconq.web.IContentInfo;
import divconq.web.IWebDomain;
import divconq.web.ViewInfo;
import divconq.web.WebContext;

public class ContentInfo implements IContentInfo {
	protected Nodes contenttemplate = null; 
	protected Class<? extends IContentBuilder> contentbuilder = null;
	
	protected Map<String,Nodes> templatetree = new HashMap<String,Nodes>(); 
	protected Map<String,Class<? extends IContentBuilder>> templatebuilder = new HashMap<String, Class<? extends IContentBuilder>>();
	
	protected ViewInfo info = null;

	public void load(ViewInfo info, divconq.xml.XElement el) {
		this.info = info;
		
		if (el.hasAttribute("ContentClass")) {
			try {
				Class<?> cls = info.getDomain().getExtension().getBundle().getClass(el.getAttribute("ContentClass"));
				
				// TODO improve tracing
				if (cls != null) 
					this.contentbuilder = cls.asSubclass(IContentBuilder.class);
			}
			catch (Exception x) {
				// TODO improve tracing
				System.out.println("could not load class: " + el.getAttribute("ContentClass"));
			}
		}
		
		IWebDomain domain = info.getDomain();
		//IViewParser parser = domain.getFormatParser("JsonOutput");
		
		// TODO log
		//if (parser == null)
		//	return;
		
		divconq.xml.XElement cel = el.find("Content");
		
		if (cel != null)
			this.contenttemplate = domain.parseXml("JsonOutput", info, cel);		
		
		for (divconq.xml.XElement tmpel : el.selectAll("Template")) {
			if (!tmpel.hasAttribute("Id"))
				continue;
			
			String tid = tmpel.getAttribute("Id");
			
			try {
				if (tmpel.hasAttribute("ContentClass")) {
					try {
						Class<?> cls = domain.getExtension().getBundle().getClass(tmpel.getAttribute("ContentClass"));
						
						// TODO improve tracing
						if (cls != null) 
							this.templatebuilder.put(tid, cls.asSubclass(IContentBuilder.class)); 
					}
					catch (Exception x) {
						// TODO improve tracing
						System.out.println("could not load class: " + el.getAttribute("ContentClass"));
					}
				}
				else
					this.templatetree.put(tid, domain.parseXml("JsonOutput", info, tmpel));
			}
			catch (Exception x) {
				// TODO trace
			}
		}
	}
	
	public Nodes getOutput(Fragment frag, WebContext ctx) throws Exception {
		if (this.contentbuilder != null)
			return this.contentbuilder.newInstance().getContent(ctx, this.info, frag);
		
		if (this.contenttemplate != null)
			return this.contenttemplate.deepCopy();		
		
		return null;
	}

	public Nodes getTemplateOutput(String name, Element parent, Map<String, String> params) {
		try {
			Nodes tmpl = null;
			
			if (this.templatebuilder.containsKey(name))
				tmpl = this.templatebuilder.get(name).newInstance().getContent(null, this.info, parent);			
			else if (this.templatetree.containsKey(name))
				tmpl = this.templatetree.get(name).deepCopy();
			
			for (Node n : tmpl.getList()) {
				n.setParams(params);
			}
			
			return tmpl;
		} 
		catch (Exception x) {
			// TODO Auto-generated catch block
		}
		
		return null;
	}
}