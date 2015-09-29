package firebats.collections;

import com.google.common.base.Preconditions;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;

public class PageRange {
	private long start;
	private long end;
	private long limit;
	private long page;
	private long pageIndex;
	private long pageStart;
	private long pageEnd;

	private PageRange(long start, long end, long limit, long page, long pageIndex) {
		this.start = start;
		this.end = end;
		this.limit = limit;
		this.page = page(start,end,limit);
		this.pageIndex = pageIndex;
		this.pageStart = start + (pageIndex - 1) * limit;
		long e = pageIndex * limit;
		this.pageEnd = end > e ? e : end;
	}

	public static long page(long start, long end, long limit){
		return ((end - start) / limit) + 1;
	}
	
	public static PageRange of(long start, long end, long limit) {
		long page=page(start, end, limit);
		return new PageRange(start, end, limit, page,/*pageIndex start */ 1);
	}

	public PageRange next() {
		Preconditions.checkArgument(!this.isLast(),"[%s/%s] no next page",pageIndex,page);
		return new PageRange(start, end, limit, page, pageIndex + 1);
	}
	/**
 	 * 
	 * @return 当前页起始值
	 */
	public long getPageStart() {
		return pageStart;
	}
	/**
 	 * 
	 * @return 当前页结束值
	 */
	public long getPageEnd() {
		return pageEnd;
	}

	public long getStart() {
		return start;
	}

	public long getEnd() {
		return end;
	}

	public long getLimit() {
		return limit;
	}

	/**
	 * 页数从1开始，而不是0。
	 * 
	 * @return 总页数
	 */
	public long getPage() {
		return page;
	}
	/**
	 * 页数从1开始，而不是0。
	 * 
	 * @return 当前页
	 */
	public long getPageIndex() {
		return pageIndex;
	}

	public Range<Long> toRange() {
		return Range.closed(pageStart, pageEnd);
	}

	public ContiguousSet<Long> toContiguousSet(){
		return ContiguousSet.create(toRange(), DiscreteDomain.longs());
	}
	
	@Override
	public String toString() {
		return String.format("[%s,%s] at %s/%s of [%s,%s]", pageStart, pageEnd,  pageIndex,page, start, end);
	}
	/**
	 * 页数从1开始，而不是0。
	 * @return 是否为第一页
	 */
	public boolean isFirst() {
 		return pageIndex==1;
	}
	/**
	 * 页数从1开始，而不是0。
	 * @return 是否为最后一页
	 */
	public boolean isLast() {
 		return pageIndex>=page;
	}

	public long pageRows() {
 		return toContiguousSet().size();
	}
}
