package divconq.web.md.plugin;

import java.util.List;
import java.util.Map;

import divconq.web.md.Plugin;
import divconq.web.md.ProcessContext;
import divconq.xml.XElement;

public class CaptionedImagePlugin extends Plugin {

	public CaptionedImagePlugin() {
		super("CapitionedImage");
	}

	@Override
	public void emit(ProcessContext ctx, XElement parent, List<String> lines,
			Map<String, String> params) {
		// TODO Auto-generated method stub
		
	}
	
	/*
	@Override
	public void emit(StringBuilder out, List<String> lines, Map<String, String> params) {
		out.append("<img ");
		
        for (String name : params.keySet()) 
            out.append(name + "=\"" + divconq.xml.XNode.quote(params.get(name)) + "\" ");
		
		out.append("/>");
	}
	*/
}
