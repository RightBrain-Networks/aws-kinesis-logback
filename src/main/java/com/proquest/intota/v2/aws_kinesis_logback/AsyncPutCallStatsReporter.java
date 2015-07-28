package com.proquest.intota.v2.aws_kinesis_logback;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;


import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gathers information on how many put requests made by AWS SDK's async client,
 * succeeded or failed since the beginning
 */
public class AsyncPutCallStatsReporter implements AsyncHandler<PutRecordRequest, PutRecordResult> {
  private static final Logger LOGGER = LoggerFactory.getLogger(AsyncPutCallStatsReporter.class);
  private String appenderName;
  private long successfulRequestCount;
  private long failedRequestCount;
  private DateTime startTime;

  public AsyncPutCallStatsReporter(String appenderName) {
    this.appenderName = appenderName;
    this.startTime = DateTime.now();
  }

  /**
   * This method is invoked when there is an exception in sending a log record
   * to Kinesis. These logs would end up in the application log if configured
   * properly.
   */
  @Override
  public void onError(Exception exception) {
    failedRequestCount++;
    LOGGER.error("Failed to publish a log entry to kinesis using appender: " + appenderName, exception);
  }

  /**
   * This method is invoked when a log record is successfully sent to Kinesis.
   * Though this is not too useful for production use cases, it provides a good
   * debugging tool while tweaking parameters for the appender.
   */
  @Override
  public void onSuccess(PutRecordRequest request, PutRecordResult result) {
    successfulRequestCount++;
    if (LOGGER.isDebugEnabled() && (successfulRequestCount + failedRequestCount) % 3000 == 0) {
    	LOGGER.debug("Appender (" + appenderName + ") made " + successfulRequestCount
          + " successful put requests out of total " + (successfulRequestCount + failedRequestCount) + " in "
          + PeriodFormat.getDefault().print(new Period(startTime, DateTime.now())) + " since start");
    }
  }
}