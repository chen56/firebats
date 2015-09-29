package firebats.profiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import firebats.net.Path;
 
public class TestProfiler {
	private Profiler Root=Profiler.newRoot();
    @Test
    public void path() throws InterruptedException{
    	Profiler b = Root.child("a/b");
    	assertEquals(1,Root.children().size());
    	assertEquals(1,Root.child("a").children().size());
    	assertEquals("b",Root.child("a").children().iterator().next().getName());

    	assertEquals(0,b.children().size());

    	Root.print();
    }

    @Test
    public void 重复获取_不新增() throws InterruptedException{
    	Profiler a = Root.child("a");
    	a.child("aa");
    	a.child("aa");
    	assertEquals(1,a.children().size());
    	Root.print();
    }
    @Test
    public void test() throws InterruptedException{
    	Profiler a = Root.child("a");
    	ProfilerWatch p=a.start();
    	assertEquals(1,a.getStarted());
    	assertEquals(0,a.getStoped());

    	p.stop();
    	assertEquals(1,a.getStarted());
    	assertEquals(1,a.getStoped());
    	Root.print();
    }

    @Test
    public void real() throws InterruptedException{
    	Profiler a = Root.child("a");
    	ProfilerWatch ap=a.start();
    	sleep(1);
    	ap.stop();
 
    	ap=a.start();
    	sleep(1);
    	ap.stop();

    	Profiler b = Root.child("b");
    	ProfilerWatch bp=b.start();
    	sleep(1);
    	bp.stop();
 
    	bp=b.start();
    	sleep(1);
    	bp.stop();

    	Root.print();
    }

	private void sleep(long millis) throws InterruptedException {
		TimeUnit.MILLISECONDS.sleep(millis);
	}
	
    @Test
    public void studyPath() throws InterruptedException{
    	print("",0);//0
		print(" ",1);//1
		print("a",1);//1
		print("a/b",2);//2
		print("/a/b",2);//2
		try {
	 		print(null,0);//error
            fail();
		} catch (Exception e) {
 		}
    }

	private static void print(String p,int expectedSegmentCount) {
		assertEquals(expectedSegmentCount,Path.fromPortableString(p).segmentCount());
// 		System.out.println("["+p+"]:"+Path.fromPortableString(p).segmentCount());
	}

}