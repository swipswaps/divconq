package divconq.mail;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.stream.ChunkedInput;
import divconq.filestore.CommonPath;
import divconq.lang.op.FuncCallback;
import divconq.struct.RecordStruct;
import divconq.web.IInnerContext;
import divconq.web.IWebMacro;
import divconq.web.Request;
import divconq.web.Response;
import divconq.web.WebDomain;
import divconq.web.WebSite;

public class EmailInnerContext implements IInnerContext {
	protected Request request = null;
	protected Response htmlresponse = null;
	protected Response textresponse = null;
	protected String subject = null;
	
	protected WebSite site = null;
	
	protected Response currresponse = null;
	protected FuncCallback<EmailInnerContext> callback = null;
	
	protected RecordStruct altparams = null;
	
	public void setSubject(String v) {
		this.subject = v;
	}
	
	public String getSubject() {
		return this.subject;
	}
	
	@Override
	public void setAltParams(RecordStruct v) {
		this.altparams = v;
	}
	
	@Override
	public RecordStruct getAltParams() {
		return this.altparams;
	}
	
	public EmailInnerContext(CommonPath path, WebSite site, FuncCallback<EmailInnerContext> callback) {
		this.site = site;
		
        this.request = new Request();
        this.request.loadVoid(path);
        
        this.htmlresponse = new Response();
        this.htmlresponse.loadVoid();
        
        this.textresponse = new Response();
        this.textresponse.loadVoid();
        
        this.currresponse = this.textresponse;
        
        this.callback = callback;
	}

	@Override
	public Request getRequest() {
		return this.request;
	}

	@Override
	public Response getResponse() {
		return this.currresponse;
	}

	public Response getHtmlResponse() {
		return this.htmlresponse;
	}

	public Response getTextResponse() {
		return this.textresponse;
	}

	@Override
	public WebDomain getDomain() {
		return this.site.getDomain();
	}

	@Override
	public WebSite getSite() {
		return this.site;
	}
	
	@Override
	public IWebMacro getMacro(String name) {
		return null;
	}

	@Override
	public void send() {
		this.callback.setResult(this);
		this.callback.complete();
	}

	@Override
	public void sendStart(int contentLength) {
		System.out.println("unexpected send start");
	}

	@Override
	public void send(ByteBuf content) {
		System.out.println("unexpected send buf");
	}

	@Override
	public void send(ChunkedInput<HttpContent> content) {
		System.out.println("unexpected send chunk");
	}

	@Override
	public void sendEnd() {
		System.out.println("unexpected sendEnd");
	}

	@Override
	public void close() {
		System.out.println("unexpected close");
	}

	public void useText() {    
        this.currresponse = this.textresponse;
	}

	public void useHtml() {    
        this.currresponse = this.htmlresponse;
	}

	public boolean isTextMode() {
		return (this.currresponse == this.textresponse);
	}			
}
