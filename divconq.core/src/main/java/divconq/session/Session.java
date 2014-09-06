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
package divconq.session;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import divconq.bus.Message;
import divconq.bus.MessageUtil;
import divconq.bus.ServiceResult;
import divconq.hub.Hub;
import divconq.lang.FuncCallback;
import divconq.lang.IOperationObserver;
import divconq.lang.OperationContext;
import divconq.lang.OperationContextBuilder;
import divconq.lang.OperationResult;
import divconq.lang.UserContext;
import divconq.log.DebugLevel;
import divconq.log.Logger;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;
import divconq.util.TimeUtil;
import divconq.work.ISynchronousWork;
import divconq.work.TaskObserver;
import divconq.work.TaskRun;
import divconq.work.Task;

// TODO needs a plan system for what to do when session ends/times out/etc 
public class Session {
	static protected SecureRandom random = new SecureRandom();
	static protected AtomicLong taskid = new AtomicLong();
	
	static public String nextSessionId() {
		  return new BigInteger(130, Session.random).toString(32);
	}	
	
	static public String nextTaskId() {
		  return Session.nextTaskId("DEFAULT");
	}	
	
	static public String nextTaskId(String part) {
		  return OperationContext.getHubId() + "_" + part + "_" + UUID.randomUUID().toString().replace("-", "");
	}	
	
	static public String nextChannelId() {
		  return UUID.randomUUID().toString().replace("-", "");
	}	
	
	protected String id = null;
	protected String key = null;
	protected long lastAccess = 0;
	protected UserContext user = null;
	protected DebugLevel level = null;
	protected String originalOrigin = null;

	protected HashMap<String, Struct> cache = new HashMap<>();
	protected HashMap<String, IComponent> components = new HashMap<>();
	
	protected ReentrantLock tasklock = new ReentrantLock();
	protected HashMap<String, TaskRun> tasks = new HashMap<>();
	
	protected ReentrantLock channellock = new ReentrantLock();
	protected HashMap<String, DataStreamChannel> channels = new HashMap<>();
	
	protected ISessionAdapter adapter = null;
	protected ServiceResult sendwaitCallback = null;
	protected Message sendwaitMessage = null;
	protected ReentrantLock sendwaitLock = null;
	
	protected boolean keep = false;
	
	/*
Context: {
      Domain: "divconq.com",
      Origin: "http:[ipaddress]",
      Chronology: "/America/Chicago",
      Locale: "en-US",  
      UserId: "119",
      UserName: "awhite",
      FullName: "Andy White",
      Email: "andy.white@divconq.com",
      AuthToken: "010A0D0502",
      Credentials: {
         UserName: "nnnn",
         Password: "mmmm"
      }
}

	 
Context: {
      Domain: "divconq.com",
      Origin: "http:[ipaddress]",
      Chronology: "/America/Chicago",
      Locale: "en-US"
}

	 */
	
	public String getId() {
		return this.id;
	}
	
	public HashMap<String, Struct> getCache() {
		return this.cache;
	}

	public String getKey() {
		return this.key;
	}
	
	/**
	 * @return logging level to use with this session (and all sub tasks)
	 */
	public DebugLevel getLevel() {
		return this.level;
	}
	
	/**
	 * @param v logging level to use with this session (and all sub tasks)
	 */
	public void setLevel(DebugLevel v) {
		this.level = v;
	}
	
	public boolean getKeep() {
		return this.keep;
	}
	
	public void setKeep(boolean v) {
		this.keep = v;
	}

	public UserContext getUser() {
		return this.user;
	}

	public void setAdatper(ISessionAdapter v) {
		this.adapter = v;
	}
	
	public ISessionAdapter getAdapter() {
		return this.adapter;
	}
	
	public Session(OperationContextBuilder usrctx) {
		this.id = OperationContext.getHubId() + "_" + Session.nextSessionId();
		this.key = StringUtil.buildSecurityCode();
		this.level = Logger.getGlobalLevel();
		this.user = UserContext.allocate(usrctx);		
		this.originalOrigin = "hub:";
		
		this.touch();
	}
	
	public Session(String origin, String domainid) {
		this(new OperationContextBuilder().withGuestUserTemplate().withDomainId(domainid));
		
		this.originalOrigin = origin;
	}
	
	public Session(OperationContext ctx) {
		this(ctx.getUserContext().toBuilder());
		
		this.originalOrigin = ctx.getOrigin();
	}
	
	public void touch() {
		this.lastAccess = System.currentTimeMillis();		
	}
	
	// TODO user sessions can be idle for a longer time (7 minutes?) than guest sessions (75 seconds)
	public boolean isLongRunning() {
		return this.user.isTagged("User");
	}
	
	public void end() {
		//System.out.println("collab session ended: " + this.collabId);
		if (!this.user.looksLikeGuest() && !this.user.looksLikeRoot()) {
			Message msg = new Message("dcAuth", "Authentication", "SignOut");				
			Hub.instance.getBus().sendMessage(msg);	
			
			// reset the creds and the verification
			//this.user = UserContext.checkCredentials(this.user, null).toUserContext();
			this.user = UserContext.allocateGuest();
		}
		
		Logger.info("Ending session: " + this.id);
		
		// TODO consider clearing adapter and reply handler too
	}
	
	public OperationContext allocateTaskContext() {
		return this.allocateTaskContext(this.originalOrigin);
	}
	
	public OperationContext allocateTaskContext(String origin) {
		return OperationContext.allocate(this.user, 
				new OperationContextBuilder()
					.withOrigin(origin)
					.withDebugLevel(this.level)
					.withSessionId(this.id));
	}
	
	public OperationContext setTaskContext(String origin) {
		OperationContext tc = this.allocateTaskContext(origin);
		OperationContext.set(tc);
		return tc;
	}
	
	public OperationContext setTaskContext() {
		OperationContext tc = this.allocateTaskContext(this.originalOrigin);
		OperationContext.set(tc);
		return tc;
	}

	public Task allocateTaskBuilder() {
		return new Task()
			.withContext(this.allocateTaskContext(this.originalOrigin));
	}

	public Task allocateTaskBuilder(String origin) {
		return new Task()
			.withContext(this.allocateTaskContext(origin));
	}
	
	public TaskRun submitTask(Task task, IOperationObserver... observers) {
		TaskRun run = new TaskRun(task);
		
		if (task == null) {
			run.errorTr(213, "info");
			return run;
		}
		
		// ensure we have an id
		run.prep();
		
		final String id = task.getId();
		
		// the submitted task will now report as owned by this session - if it isn't already
		String sid = task.getContext().getSessionId();
		
		if (!this.id.equals(sid))
			task.withContext(task.getContext().toBuilder().withSessionId(this.id).toOperationContext());
		
		this.tasks.put(id, run);
		
		for (IOperationObserver observer: observers)
			run.addObserver(observer);
		
		run.addObserver(new TaskObserver() {			
			@Override
			public void completed(TaskRun or) {
				// TODO review that this is working correctly and does not consume memory
				// otherwise TaskRun complete can lookup session and remove via there - might be better
				Session.this.tasks.remove(id);
			}
		});
		
		Hub.instance.getWorkPool().submit(run);
		
		return run;
	}

	// collect all tasks, filter by tags if any
	public void collectTasks(List<TaskRun> bucket, String... tags) {
		for (TaskRun task : this.tasks.values()) 
			if ((tags.length == 0) || task.getTask().isTagged(tags))
				bucket.add(task);
	}

	public void countTags(Map<String, Long> tagcount) {
		for (TaskRun task : this.tasks.values()) {
			ListStruct tags = task.getTask().getTags();
			
			if ((tags == null) || (tags.getSize() == 0)) {
				long cnt = tagcount.containsKey("[none]") ? tagcount.get("[none]") : 0;
				
				cnt++;
				
				tagcount.put("[none]", cnt);
			}
			else {
				for (Struct stag : tags.getItems()) {
					String tag = stag.toString();
					
					long cnt = tagcount.containsKey(tag) ? tagcount.get(tag) : 0;
					
					cnt++;
					
					tagcount.put(tag, cnt);
				}
			}
		}
	}

	// count all tasks, filter by tags if any
	public int countTasks(String... tags) {
		int num = 0;
		
		for (TaskRun task : this.tasks.values()) 
			if ((tags.length == 0) || task.getTask().isTagged(tags))
				num++;
		
		return num;
	}

	// count all tasks, filter by tags if any
	public int countIncompleteTasks(String... tags) {
		int num = 0;
		
		for (TaskRun task : this.tasks.values()) 
			if (!task.isComplete() && ((tags.length == 0) || task.getTask().isTagged(tags)))
				num++;
		
		return num;
	}
	
	public RecordStruct toStatusReport() {
		RecordStruct rec = new RecordStruct();
		
		rec.setField("Id", this.id);
		rec.setField("Key", this.key);
		
		if (this.lastAccess != 0)
			rec.setField("LastAccess", TimeUtil.stampFmt.print(this.lastAccess));
		
		if (this.user != null)
			rec.setField("UserContext", this.user.freezeToRecord());
		
		if (this.level != null)
			rec.setField("DebugLevel", this.level.toString());
		
		if (StringUtil.isNotEmpty(this.originalOrigin))
			rec.setField("Origin", this.originalOrigin);
		
		rec.setField("Keep", this.keep);
		
		ListStruct tasks = new ListStruct();
		
		for (TaskRun t : this.tasks.values())
			tasks.addItem(t.toStatusReport());
		
		rec.setField("Tasks", tasks);
		
		return rec;
	}
	
	/**
	 * Typically called by Hyper RPC
	 * 
	 *	 we don't need a time out, it is up to the client to timeout
	 * 
	 * @param msg
	 * @param serviceResult
	 */
	public void sendMessageWait(Message msg, ServiceResult serviceResult) {
		// only create if needed
		if (this.sendwaitLock == null)
			this.sendwaitLock = new ReentrantLock();
		
		this.sendwaitLock.lock();
		
		try {
			// a single HTTP session can handle only 1 request at a time
			// TODO log
			if (this.sendwaitMessage != null) 
				System.out.println("Sending another message when we haven't finished with ####################################: " + msg.toPrettyString());
			
			this.sendwaitCallback = serviceResult;		
			this.sendwaitMessage = msg;
		}
		finally {
			this.sendwaitLock.unlock();
		}
		
		this.sendMessage(msg);
	}

	/**
	 * Typically called by Web and Common RPC
	 * 
	 * @param msg
	 */
	public void sendMessage(final Message msg) {
		// be sure we are using a proper context
		if (!OperationContext.hasContext()) 
			this.setTaskContext();
		
		// note that session has been used
		this.touch();		
		
		// update the credentials if present in message 
		if (msg.hasField("Credentials")) {
			// we don't want the creds in the message root on the bus - because they should
			// travel as part of the context with the message
			RecordStruct newcreds = msg.getFieldAsRecord("Credentials");
			msg.removeField("Credentials");
			
			// if the sent credentials are different from those already in context then change
			// (set checks if different)
			OperationContextBuilder umod = UserContext.checkCredentials(this.user, newcreds);

			// credentials have changed
			if (umod != null) {
				this.user = umod.toUserContext();
				
				OperationContext.set(OperationContext.allocate(this.user, OperationContext.get().freezeToRecord()));
			}
		}
		
		// not valid outside of RPC calls
		// msg.removeField("Session");  NOT valid at all?
		
		String service = msg.getFieldAsString("Service");
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");

		// user requests a new session directly
		if ("Session".equals(service)) {
			// user requests end to session
			if ("Control".equals(feature)) {
				if ("Start".equals(op)) {
					this.verifySession(new FuncCallback<Message>() {				
						@Override
						public void callback() {
							Message rmsg = this.getResult();
							
							// TODO review how this is used/give less info to caller by default
							RecordStruct body = new RecordStruct();							
							rmsg.setField("Body", body);
							
							Session.this.user.freezeRpc(body);
							
							body.setField("SessionId", Session.this.id);		// TODO probably not
							body.setField("SessionKey", Session.this.key);		// TODO probably not
							
							Session.this.reply(rmsg, msg);
						}
					});
					
					return;
				}
				else if ("Stop".equals(op)) {
					Session.this.reply(MessageUtil.success(), msg);
					Hub.instance.getSessions().terminate(this.id);
					return;
				}
				else if ("Touch".equals(op)) {
					Session.this.reply(MessageUtil.success(), msg);
					return;
				}
			}
		}
		
		// if the caller skips Session Start that is fine - but if they pass creds we verify anyway before processing the message 		
		this.verifySession(new FuncCallback<Message>() {				
			@Override
			public void callback() {
				Message rmsg = this.getResult();
				
				if (rmsg.hasErrors())
					Session.this.reply(rmsg, msg);
				else
					Session.this.sendMessageThru(msg);
			}
		});
	}

	// if session has an unverified user, verify it
	public void verifySession(final FuncCallback<Message> cb) {
		if (!this.user.isVerified()) {
			final boolean waslikequest = this.user.looksLikeGuest();
			
			OperationContext tc = OperationContext.get();
			
			tc.verify(new FuncCallback<UserContext>() {				
				@Override
				public void callback() {
					UserContext uc = this.getResult();
					
					if (uc != null) {
						// TODO if the user context changed, then update the operation context
						//if (!uc.equals(this.getContext().getUserContext()))
						
						// update the operation context
						OperationContext.use(uc, OperationContext.get().toBuilder());
						
						// it would not be ok to store TaskContext here because of elavation
						// but user context can be
						Session.this.user = uc;
						
						boolean nowlikeguest = Session.this.user.looksLikeGuest();
						
						if (nowlikeguest && !waslikequest)
							cb.error(1, "User not authenticated!");
						
						// although we typically do not change the context of a callback, in this case we should
						// so the user verify will travel along with the request message 
						cb.setContext(OperationContext.get());
					}
					
					if (cb != null) {
						cb.setResult(this.toMessage());
						cb.completed();
					}
				}
			});
			
			return;
		}
		
		if (cb != null) {
			cb.setResult(MessageUtil.success());
			cb.completed();
		}
	}
	
	private void sendMessageThru(final Message msg) {		
		// TODO make sure the message has been validated by now
		
		String service = msg.getFieldAsString("Service");
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
		
		if ("Session".equals(service)) {
			if ("Control".equals(feature)) {
				if ("CheckInBox".equals(op)) {
					Message reply = MessageUtil.success();
					
					if (this.adapter != null) 
						reply.setField("Body", this.adapter.popMessages());
					
					Session.this.reply(reply, msg);					
					return;
				}
				
				if ("CheckJob".equals(op)) {
					RecordStruct rec = msg.getFieldAsRecord("Body");
					Long jid = rec.getFieldAsInteger("JobId");
					
					TaskRun info = this.tasks.get(jid);
					
					if (info != null) {
						Struct res = info.getResult();
						Message reply = MessageUtil.messages(info);
						
						reply.setField("Body",
								new RecordStruct(
										new FieldStruct("AmountCompleted", info.getAmountCompleted()),
										new FieldStruct("Steps", info.getSteps()),
										new FieldStruct("CurrentStep", info.getCurrentStep()),
										new FieldStruct("CurrentStepName", info.getCurrentStepName()),
										new FieldStruct("ProgressMessage", info.getProgressMessage()),
										new FieldStruct("Result", res)
								)
						);
						
						Session.this.reply(reply, msg);
					}
					else {
						Message reply = MessageUtil.error(1, "Job Not Found");		// TODO
						Session.this.reply(reply, msg);
					}
					
					return;
				}
				
				if ("ClearJob".equals(op)) {
					RecordStruct rec = msg.getFieldAsRecord("Body");
					Long jid = rec.getFieldAsInteger("JobId");
					
					this.tasks.remove(jid);
					
					Session.this.reply(MessageUtil.success(), msg);
					return;
				}
				
				if ("KillJob".equals(op)) {
					RecordStruct rec = msg.getFieldAsRecord("Body");
					Long jid = rec.getFieldAsInteger("JobId");
					
					// get not remove, because kill should do the remove and we let it do it in the natural way
					TaskRun info = this.tasks.get(jid);
					
					if (info != null) 
						info.kill();
					
					Session.this.reply(MessageUtil.success(), msg);
					return;
				}

				/* TODO someday support an interactive groovy shell via any session, assuming SysAdmin access and system wide setting
        Binding b = new Binding();
        b.setVariable("x", 1);
        b.setVariable("y", 2);
        b.setVariable("z", 3);
        GroovyShell sh = new GroovyShell(b);
        
        sh.evaluate("print z");
        sh.evaluate("d = 1");
        sh.evaluate("print d");
        
        sh.evaluate("println divconq.util.HashUtil.getMd5('abcxyz')");
        
        sh.evaluate("import divconq.util.HashUtil");
        sh.evaluate("println HashUtil.getMd5('abcxyz')");
				 * 
				 * 
				 * consider
				 * 	http://mrhaki.blogspot.co.uk/2011/06/groovy-goodness-add-imports.html
				 * 
				 * 
				 */
				
			}
			else if ("DataChannel".equals(feature)) {
				this.dataChannel(msg);
				return;
			}
		}
		
		this.sendMessageIn(msg);
	}
	
	private void sendMessageIn(final Message msg) {		
		// so that responses come to the Sessions service
		// the id will be stripped off before delivery to client
		String resptag = msg.getFieldAsString("RespondTag");
		
		if (StringUtil.isNotEmpty(resptag)) {
			msg.setField("RespondTag", this.id + "_" + resptag);		
			msg.setField("RespondTo", "Session");
		}
		
		/*
		System.out.println("------------");
		
		System.out.println("elevated: " + tc.isElevated());
		System.out.println("user: " + tc.getUserContext());
		System.out.println("message: " + msg);
		
		System.out.println("------------");
		*/
		
		OperationResult smor = Hub.instance.getBus().sendMessage(msg);
		
		if (smor.hasErrors())
			Session.this.reply(MessageUtil.messages(smor), msg);		
	}
	
	public void deliver(Message msg) {
		if ("SendWait".equals(msg.getFieldAsString("Tag"))) {
			// only create if needed
			if (this.sendwaitLock == null)
				this.sendwaitLock = new ReentrantLock();
			
			this.sendwaitLock.lock();
			
			try {
				if (this.sendwaitCallback != null) {
					this.sendwaitCallback.setReply(msg);
					this.sendwaitCallback.completed();
					
					this.sendwaitCallback = null;
					this.sendwaitMessage = null;
				}
			}
			finally {
				this.sendwaitLock.unlock();
			}
			
			return;
		}
		
		if (this.adapter != null)
			this.adapter.deliver(msg);
	}
	
	// only allowed to be called on local session replies, not for external use because of threading
	protected void reply(final Message rmsg, final Message msg) {
		// put the reply on a new thread because of how LocalSession will build up a large call stack
		// if threads don't change 
		
		Hub.instance.getWorkPool().submit(new ISynchronousWork() {
			@Override
			public void run(TaskRun run) {
		    	rmsg.setField("Service", "Replies");  // msg.getFieldAsString("RespondTo"));
		    	rmsg.setField("Feature", "Reply");
		    	rmsg.setField("Op", "Deliver");
		    	
				String tag = msg.getFieldAsString("RespondTag");

				// should always have a tag if got here
				if (StringUtil.isNotEmpty(tag)) {
					// pull session id out of the tag
					int pos = tag.indexOf('_', 30);
					
					if (pos != -1)
						tag = tag.substring(pos + 1);
					
					// strip out session id, restore original tag
					rmsg.setField("Tag", tag);
					
			    	Session.this.deliver(rmsg);
				}
				else {
					// TODO
				}
			}
		});
	}

	public void addChannel(DataStreamChannel v) {
		this.channellock.lock();
		
		try {
			this.channels.put(v.getId(), v);
		}
		finally {
			this.channellock.unlock();
		}
	}
	
	public DataStreamChannel getChannel(String id) {
		return this.channels.get(id);
	}
	
	public void removeChannel(String id) {
		this.channellock.lock();
		
		try {
			this.channels.remove(id);
		}
		finally {
			this.channellock.unlock();
		}
	}

	public void dataChannel(final Message msg) {
		String op = msg.getFieldAsString("Op");
		
		if ("Establish".equals(op)) {
			DataStreamChannel chan = new DataStreamChannel(this.getId(), msg.getFieldAsRecord("Body").getFieldAsString("Title"));
			
			RecordStruct sr = msg.getFieldAsRecord("Body").getFieldAsRecord("StreamRequest");
			
			RecordStruct srb = sr.getFieldAsRecord("Body");
			
			if (srb == null) {
				Session.this.reply(MessageUtil.error(0, "Missing StreamRequest Body"), msg);
				return;
			}
			
			// add to the existing fields - which might typically be "FilePath" or "Token"
			srb.setField("Channel", chan.getId());
			
			Message srmsg = MessageUtil.fromRecord(sr);

			Hub.instance.getBus().sendMessage(srmsg, res -> {				
				if (res.hasErrors()) { 
					res.error(1, "Start Upload error: " + res.getMessage());
					
					Session.this.reply(res.toLogMessage(), msg);
					return;
				}

				RecordStruct srrec = res.getBodyAsRec();
				
				if (srrec == null) { 
					Session.this.reply(MessageUtil.error(1, "Start Upload error: Missing StreamRequest response"), msg);
					return;
				}
					
				Session.this.addChannel(chan);
				chan.setBinding((RecordStruct) srrec.deepCopy());
				
				// protect from client view
				srrec.removeField("Hub");
				srrec.removeField("Session");
				srrec.removeField("Channel");
				
				// include the client end of the channel
				srrec.setField("ChannelId", chan.getId());
				
				Session.this.reply(MessageUtil.success(srrec), msg);
			}); 
			
			return;
		}
		
		if ("Free".equals(op)) {
			String chid = msg.getFieldAsRecord("Body").getFieldAsString("ChannelId");
			this.removeChannel(chid);
			
			Session.this.reply(MessageUtil.success(), msg);
			return;
		}

		/*
		if ("Allocate".equals(op)) {
			DataStreamChannel chan = new DataStreamChannel(this.getId(), msg.getFieldAsRecord("Body").getFieldAsString("Title"));
			
			this.addChannel(chan);
			
			Session.this.reply(MessageUtil.success("ChannelId", chan.getId()), msg);
			return;
		}
		
		if ("Bind".equals(op)) {
			RecordStruct rec = msg.getFieldAsRecord("Body");
			String chid = rec.getFieldAsString("ChannelId");
			
			DataStreamChannel chan = this.getChannel(chid);
			
			if (chan == null) {
				Session.this.reply(MessageUtil.error(1, "Missing channel"), msg);
				return;
			}
			
			chan.setBinding(rec);
			
			// TODO tell the channel it is a dest or src
			
			Session.this.reply(MessageUtil.success(), msg);
			return;
		}
		*/
		
		Session.this.reply(MessageUtil.errorTr(441, "Session", "DataChannel", op), msg);
	}

	public void clearToGuest() {
		this.user = UserContext.allocateGuest();
	}

	public void reviewPlan() {
		// TODO add session plan features
		
		// cleannup expired channels
		List<DataStreamChannel> killlist = new ArrayList<>();
		
		this.channellock.lock();
		
		try {
			for (DataStreamChannel chan : this.channels.values()) {
				if (chan.isHung()) 
					killlist.add(chan);
			}
		}
		finally {
			this.channellock.unlock();
		}
		
		for (DataStreamChannel chan : killlist) {
			Logger.warn("Session " + this.id + " found hung transfer: " + chan);
			chan.abort();
		}		
	}

	public Collection<DataStreamChannel> channels() {
		return this.channels.values();
	}
}