package org.apache.ibatis.executor;

import java.sql.BatchUpdateException;
import java.util.List;

//批处理执行异常
public class BatchExecutorException extends ExecutorException {

	private static final long serialVersionUID = 154049229650533990L;
	private final List<BatchResult> successfulBatchResults;
	private final BatchUpdateException batchUpdateException;
	private final BatchResult batchResult;

	public BatchExecutorException(String message, BatchUpdateException cause, List<BatchResult> successfulBatchResults,
			BatchResult batchResult) {
		super(message + " Cause: " + cause, cause);
		this.batchUpdateException = cause;
		this.successfulBatchResults = successfulBatchResults;
		this.batchResult = batchResult;
	}
	
	//获取批次更新异常
	public BatchUpdateException getBatchUpdateException() {
		return batchUpdateException;
	}

	/*
	 * Returns a list of BatchResult objects. There will be one entry in the list
	 * for each successful sub-executor executed before the failing executor.
	 *
	 * @return the previously successful executor results (may be an empty list if
	 * no executor has executed successfully)
	 */
	public List<BatchResult> getSuccessfulBatchResults() {
		return successfulBatchResults;
	}

	/*
	 * Returns the SQL statement that caused the failure (not the parameterArray)
	 *
	 * @return the failing SQL string
	 */
	public String getFailingSqlStatement() {
		return batchResult.getSql();
	}

	/*
	 * Returns the statement id of the statement that caused the failure
	 *
	 * @return the statement id
	 */
	public String getFailingStatementId() {
		return batchResult.getMappedStatement().getId();
	}
}
