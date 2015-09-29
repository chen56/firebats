package firebats.profiler;

import java.util.Collection;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import firebats.net.Path;

public class Profiler {
	private static final String Format = "%-20s %-20s %-20s %-20s %-10s";
	private AtomicLong started=new AtomicLong(0);
	private AtomicLong stoped=new AtomicLong(0);
	private AtomicLong elapsedNano=new AtomicLong(0);
  	private ConcurrentNavigableMap<String,Profiler> children=new ConcurrentSkipListMap<>();
	private String name;
	private Profiler parent;
	private int level;
	private static Profiler Empty=new Profiler(null, "empty", 0){
		@Override
		public Profiler child(String path) {
			return empty();
		}
		@Override
		public ProfilerWatch start() {
 			return ProfilerWatch.empty();
		}
		@Override
		public void print() {
 		}
    };

	/*internal*/ Profiler(Profiler parent,String name,int level){
		this.name=name;
		this.parent=parent;
		this.level=level;
	}
	public static Profiler newRoot(){
		return new Profiler(null,"Root",0); 
	}
	
	public boolean isRoot(){
		return parent==null;
	}
	
	public String getName() {
		return name;
	}
	
	public Collection<Profiler> children() {
		return children.values();
	}

	public Profiler child(String path){
		Preconditions.checkArgument(!Strings.isNullOrEmpty(path),"path should not be empty");
 		Path p=Path.fromPortableString(path);
 		return ensureChild(p,0);
	}

	private Profiler ensureChild(Path path, int index) {
		String name=path.segment(index);
		Profiler child;
		if(children.containsKey(name)){
			child=children.get(name);
		}else{
			Profiler newValue=new Profiler(parent,path.segment(index),level+1);
			Profiler oldValue=children.putIfAbsent(name, newValue);
			child = oldValue == null ? newValue:oldValue;
		}

		if(path.segmentCount()==index+1){
 			return child;
		}
 		return child.ensureChild(path,index+1);
	}
	
	public void print() {
 		String head=String.format(Format, "started","stoped","elapsed(mills)","average(mills)","profilerNode");
		System.out.println(head);
        printTree();
	}
    
 	private void printTree() {
 		long stoped=getStoped();
 		long elapsedMills = TimeUnit.MILLISECONDS.convert(elapsedNano.get(),TimeUnit.NANOSECONDS);
		long average=stoped==0?0:elapsedMills/stoped;
		String str=String.format(Format, started,stoped,elapsedMills,average,space(level)+"+"+name);
		System.out.println(str);
 		for (Profiler c : children.values()) {
			c.printTree();
		}
 	}
 	
	private String space(int space) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < space; i++) {
			sb.append("  ");
		}
		return sb.toString();
	}

	public long getStarted() {
 		return started.get();
	}

	public ProfilerWatch start() {
		started.incrementAndGet();
 		return new ProfilerWatch(this);
	}
	
    
	public long getStoped() {
 		return stoped.get();
	}
    
	/*internal*/ void _stop(ProfilerWatch watch) {
		stoped.incrementAndGet();
		elapsedNano.addAndGet(watch.elapsed(TimeUnit.NANOSECONDS));
	}
	
    
	public static Profiler empty() {
 		return Empty;
	}
}