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
package divconq.script.inst.file;

import divconq.ctp.stream.FileSourceStream;
import divconq.ctp.stream.FunnelStream;
import divconq.ctp.stream.GzipStream;
import divconq.ctp.stream.IStreamDest;
import divconq.ctp.stream.IStreamSource;
import divconq.ctp.stream.JoinStream;
import divconq.ctp.stream.NullStream;
import divconq.ctp.stream.PgpEncryptStream;
import divconq.ctp.stream.SplitStream;
import divconq.ctp.stream.StreamWork;
import divconq.ctp.stream.TarStream;
import divconq.ctp.stream.UngzipStream;
import divconq.ctp.stream.UntarStream;
import divconq.filestore.IFileCollection;
import divconq.filestore.IFileStoreDriver;
import divconq.filestore.IFileStoreFile;
import divconq.hub.Hub;
import divconq.lang.op.OperationCallback;
import divconq.lang.op.OperationContext;
import divconq.script.Ops;
import divconq.script.StackEntry;
import divconq.struct.Struct;
import divconq.struct.scalar.NullStruct;
import divconq.struct.scalar.StringStruct;
import divconq.util.StringUtil;
import divconq.work.IWork;
import divconq.work.Task;
import divconq.work.TaskRun;
import divconq.xml.XElement;

public class FileOps extends Ops {
	@Override
	public void prepTarget(StackEntry stack) {
		this.nextOpResume(stack);
	}

	@Override
	public void runOp(StackEntry stack, XElement op, Struct target) {
		if ("Copy".equals(op.getName())) 
			this.copy(stack, op);					
		else if ("XCopy".equals(op.getName())) 
			this.xcopy(stack, op);
		else if ("Tar".equals(op.getName())) 
			this.injectStream(stack, op, new TarStream());					
		else if ("Untar".equals(op.getName())) 
			this.injectStream(stack, op, new UntarStream());					
		else if ("Gzip".equals(op.getName())) 
			this.injectStream(stack, op, new GzipStream());
		else if ("Ungzip".equals(op.getName())) 
			this.injectStream(stack, op, new UngzipStream());
		else if ("Funnel".equals(op.getName())) 
			this.injectStream(stack, op, new FunnelStream());
		else if ("Split".equals(op.getName())) 
			this.injectStream(stack, op, new SplitStream());
		else if ("Join".equals(op.getName())) 
			this.injectStream(stack, op, new JoinStream());
		else if ("PGPEncrypt".equals(op.getName())) 
			this.injectStream(stack, op, new PgpEncryptStream());
		else {
			OperationContext.get().error("Unknown FileOp: " + op.getName());
			this.nextOpResume(stack);
		}
	}

	protected void copy(StackEntry stack, XElement el) {
		IStreamSource streamin = this.getSourceStream(stack, el);
		
		if (streamin != null)
			this.executeDest(stack, el, streamin, false, true);
	}

	protected void xcopy(StackEntry stack, XElement el) {
		IStreamSource streamin = this.getSourceStream(stack, el);
		
		if (streamin != null)
			this.executeDest(stack, el, streamin, true, true);
	}

	protected void injectStream(StackEntry stack, XElement el, IStreamSource add) {
		IStreamSource streamin = this.getSourceStream(stack, el);
		
		if (streamin == null) 
			return;
		
		add.init(stack, el);
		
		add.setUpstream(streamin);
		
		this.registerSourceStream(stack, el, add);

        this.executeDest(stack, el, add, true, false);
	}

	protected IStreamSource getSourceStream(StackEntry stack, XElement el) {
        Struct src = stack.refFromElement(el, "Source");
        
        if ((src == null) || (src instanceof NullStruct)) {
        	src = stack.queryVariable("_LastStream");
        	
            if ((src == null) || (src instanceof NullStruct)) {
            	OperationContext.get().error("Missing source");
				this.nextOpResume(stack);
	        	return null;
            }
        }
        
        if (src instanceof IStreamSource)
        	return (IStreamSource) src;
        
        if (!(src instanceof IFileStoreFile) && !(src instanceof IFileStoreDriver) && !(src instanceof IFileCollection)) {
        	OperationContext.get().error("Invalid source type");
			this.nextOpResume(stack);
        	return null;
        }
        
		IStreamSource filesrc = null;
		
        if (src instanceof IFileStoreFile) 
        	filesrc = ((IFileStoreFile)src).allocSrc();
        else if (src instanceof IFileStoreDriver) 
       		filesrc = ((IFileStoreDriver)src).rootFolder().allocSrc();
        else 
        	filesrc = new FileSourceStream((IFileCollection) src); 
        
        if (filesrc == null) {
        	OperationContext.get().error("Invalid source type");
			this.nextOpResume(stack);
        	return null;
        }
        
        filesrc.init(stack, el);
        
		return filesrc;
	}

	protected IStreamDest getDestStream(StackEntry stack, XElement el, boolean autorelative) {
        Struct dest = stack.refFromElement(el, "Dest");
        
        if ((dest == null) || (dest instanceof NullStruct)) 
        	return null;
        
        if ((dest instanceof StringStruct) && "NULL".equals(((StringStruct)dest).getValue()))
        	return new NullStream();
        
        if (dest instanceof IStreamDest)
        	return (IStreamDest) dest;
        
        if (!(dest instanceof IFileStoreFile) && !(dest instanceof IFileStoreDriver)) {
        	OperationContext.get().error("Invalid dest type");
			this.nextOpResume(stack);
        	return null;
        }
        
        IStreamDest deststrm = null;
        
        if (dest instanceof IFileStoreDriver) 
        	deststrm = ((IFileStoreDriver)dest).rootFolder().allocDest();
        else 
        	deststrm = ((IFileStoreFile)dest).allocDest();
        
        if (deststrm == null) {
        	OperationContext.get().error("Unable to create destination stream");
			this.nextOpResume(stack);
        	return null;
        }
        
        deststrm.init(stack, el, autorelative);
        return deststrm;
	}
	
	protected void executeDest(StackEntry stack, XElement el, IStreamSource src, boolean autorelative, boolean destRequired) {
		stack.addVariable("_LastStream", (Struct)src);
		
		IStreamDest streamout = this.getDestStream(stack, el, autorelative);
        
        if (streamout != null) {
    		streamout.setUpstream(src);
    		
    		IWork sw = new StreamWork(streamout);
    		
    		Task t = Task.subtask(OperationContext.get().getTaskRun(), "Streaming", new OperationCallback() {
    			@Override
    			public void callback() {
    				FileOps.this.nextOpResume(stack);
    	        	return;
    			}
    		});
    		
    		t.withWork(sw);
    		
    		TaskRun run = new TaskRun(t);
    		
    		run.addCloseable(streamout);

    		Hub.instance.getWorkPool().submit(run);
        }
        else {
        	if (destRequired)
        		OperationContext.get().error("Missing dest for " + el.getName());
        	
			this.nextOpResume(stack);
        	return;
        }
	}

	protected void registerSourceStream(StackEntry stack, XElement el, IStreamSource src) {
        String name = stack.stringFromElement(el, "Name");
        
        if (StringUtil.isEmpty(name))
        	name = "Stream_" + stack.getActivity().tempVarName();
        
        // to be sure we cleanup the stream, all variables added will later be disposed of
        stack.addVariable(name, (Struct)src);
	}
	
	@Override
	public void cancel(StackEntry stack) {
		// TODO review after we make operations
	}
}
