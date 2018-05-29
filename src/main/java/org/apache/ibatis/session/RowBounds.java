package org.apache.ibatis.session;

//行范围(分页用)
public class RowBounds {

	public static final int NO_ROW_OFFSET = 0;
	public static final int NO_ROW_LIMIT = Integer.MAX_VALUE;
	public static final RowBounds DEFAULT = new RowBounds();

	private int offset;  //开始位置
	private int limit;   //限制条数
	
	public RowBounds() {
		this.offset = NO_ROW_OFFSET;
		this.limit = NO_ROW_LIMIT;
	}

	public RowBounds(int offset, int limit) {
		this.offset = offset;
		this.limit = limit;
	}

	//获取开始位置
	public int getOffset() {
		return offset;
	}

	//获取取出条数
	public int getLimit() {
		return limit;
	}

}
