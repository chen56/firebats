package firebats.collections;

import static org.junit.Assert.*;

import org.fest.assertions.api.Assertions;
import org.junit.Test;

import com.google.common.collect.Lists;

import firebats.collections.PageRange;

public class TestPageRange {
	@Test
	public void page计算() {
		assertEquals(1, page(1, 1, 10));
		assertEquals(1, page(1, 9, 10));
		assertEquals(1, page(1, 10, 10));
		assertEquals(1, page(2, 11, 10));
		assertEquals(2, page(1, 19, 10));
		assertEquals(2, page(1, 20, 10));
		assertEquals(2, page(3, 21, 10));
	}
	
	@Test
	public void 每页的值() {
		PageRange page = PageRange.of(1, 6, 5);
		Assertions.assertThat(page.toContiguousSet().asList()).isEqualTo(Lists.newArrayList(1L,2L,3L,4L,5L));
		Assertions.assertThat(page.next().toContiguousSet().asList()).isEqualTo(Lists.newArrayList(6L));
        
		try {
			page.next().next();
			fail();
		} catch (Exception e) {
			assertEquals("[2/2] no next page",e.getMessage());
		}
		
		for (int i = 0; i < page.getPage(); i++) {
			System.out.println(page);
			if(!page.isLast()){
				page=page.next();
			}
		}
		assertEquals(2,page.getPageIndex());
		System.out.println("page 最后结果还是第二页："+page);
	}
	
	@Test
	public void first_last() {
		PageRange page = PageRange.of(1, 6, 5);
		//first last page
		assertEquals(false,page.isLast());
		assertEquals(true,page.isFirst());
		assertEquals(true,page.next().isLast());
		assertEquals(false,page.next().isFirst());
	}

	private long page(long start, long end, long limit) {
		return PageRange.of(start,end,limit).getPage();
	}
}
