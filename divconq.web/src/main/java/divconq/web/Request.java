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
package divconq.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import divconq.filestore.CommonPath;
import divconq.log.Logger;
import divconq.util.StringUtil;
import divconq.www.http.ContentType;
import divconq.www.http.parse.ContentTypeParser;
import divconq.www.http.parse.DateParser;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

public class Request {
	protected CommonPath path = null;
	protected CommonPath orgpath = null;
	protected String orgrequri = null;
	protected HttpHeaders headers = null;
	protected HttpMethod method = null;
	protected Map<String, List<String>> parameters = null;
    protected Map<String, Cookie> cookies = new HashMap<>();
	protected ContentType contentType = null;
	
    public Cookie getCookie(String name) {
    	return this.cookies.get(name);
    }

	public boolean hasHeader(String name) {
		return this.headers.contains(name);
	}
	
    public String getHeader(String name) {
    	return this.headers.get(name);
    }
    
    public long getDateHeader(String name) {
    	String value = this.headers.get(name);
    	
    	if (StringUtil.isEmpty(value))
    		return -1;
    	
    	DateParser parser = new DateParser();
    	return parser.convert(value);
    }
    
    public CommonPath getOriginalPath() {
		return this.orgpath;
	}
    
    public CommonPath getPath() {
		return this.path;
	}
    
    public String getOriginalPathUri() {
		return this.orgrequri;
	}
    
    // sometimes the path is rewritten by rules, this should reflect the current view of the path
    // TODO maybe store the "original" path too?
    public void setPath(CommonPath v) {
    	this.path = v;
    }
    
    public HttpMethod getMethod() {
		return this.method;
	}
    
    public ContentType getContentType() {
		return this.contentType;
	}
    
    public Map<String, List<String>> getParameters() {
    	return this.parameters;
    }
    
    public String getParameter(String name) {
    	if (this.parameters == null)
    		return null;
    	
    	List<String> values = this.parameters.get(name);
    	
    	if ((values == null) || (values.size() == 0))
    		return null;
    	
    	return values.get(0);
    }
	
    public List<String> getParameters(String name) {
    	if (this.parameters == null)
    		return null;
    	
    	return this.parameters.get(name);
    }
    
    public boolean pathEquals(String v) {
    	if (this.path != null)
    		return this.path.getFull().equals(v);
    	
    	return false;
    }
	
    public boolean pathStartsWith(String v) {
    	if (this.path != null)
    		return this.path.getFull().startsWith(v);
    	
    	return false;
    }
    
	public void load(ChannelHandlerContext ctx, HttpRequest req) {
		this.method = req.getMethod();
		this.headers = req.headers();
		
        String value = req.headers().get(Names.COOKIE);

        if (StringUtil.isNotEmpty(value)) {
        	Set<Cookie> cset = ServerCookieDecoder.STRICT.decode(value);
        	
        	for (Cookie cookie : cset) 
        		this.cookies.put(cookie.name(), cookie);
        }
        
        QueryStringDecoder decoderQuery = new QueryStringDecoder(req.getUri());        
        this.parameters = decoderQuery.parameters();        // TODO decode
        this.path = new CommonPath(QueryStringDecoder.decodeComponent(decoderQuery.path()));
        this.orgpath = this.path;
        this.orgrequri = QueryStringDecoder.decodeComponent(decoderQuery.uri());
        
        this.contentType = new ContentTypeParser(this.headers.get(Names.CONTENT_TYPE));
        
    	if (Logger.isDebug()) {
    		Logger.debug("Request Method " + this.method);
    		
    		for (Entry<String, String> ent : this.headers.entries()) {
        		Logger.debug("Request header: " + ent.getKey() + ": " + ent.getValue());
    		}
    	}
	}
    
	public void loadVoid(CommonPath path) {
		this.method = HttpMethod.GET;
		this.headers = new  DefaultHttpHeaders();
        
        this.parameters = new HashMap<>();
        this.path = path;
        this.orgpath = this.path;
	}
}
