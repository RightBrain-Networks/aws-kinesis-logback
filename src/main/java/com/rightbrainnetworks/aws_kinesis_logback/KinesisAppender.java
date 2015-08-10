package com.rightbrainnetworks.aws_kinesis_logback;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.amazonaws.services.kinesis.model.DescribeStreamResult;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.ResourceNotFoundException;
import com.amazonaws.services.kinesis.model.StreamStatus;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* modeled after AWS log4j Kinesis Appender */

public class KinesisAppender extends AppenderBase<ILoggingEvent> {
  private int maxRetries;
  private int bufferSize;
  private int threadCount;
  private String encoding;
  private int limit;
  private String region;
  private String streamName;
  private String userAgentString;
  private int keepAliveSeconds;
  private static final Logger LOGGER = LoggerFactory.getLogger(KinesisAppender.class);
  private AmazonKinesisAsyncClient kinesisClient;
  private AsyncPutCallStatsReporter asyncCallHander;
  int counter = 0;
  PatternLayoutEncoder encoder;
 
  
  public String getStreamName() {
	    return streamName;
  }
  
  public void setStreamName(String streamName) {
	    this.streamName = streamName.trim();
  }
  
  public String getEncoding() {
	    return encoding;
  }
  

  public void setEncoding(String encoding) {
	    this.encoding = encoding.trim();
  }
  
  public int getMaxRetries() {
	    return maxRetries;
  }

  public void setMaxRetries(int maxRetries) {
	    this.maxRetries = maxRetries;
  }
  
  public int getBufferSize() {
	    return bufferSize;
  }

  public void setBufferSize(int bufferSize) {
	    this.bufferSize = bufferSize;
  }
  
  public int getThreadCount() {
	    return threadCount;
  }

  public void setThreadCount(int threadCount) {
	    this.threadCount = threadCount;
  }
  
  public void setRegion(String region) {
	    this.region = region.trim();
  }

  public String getRegion() {
	    return region;
  }
  
  public void setUserAgentString(String uas) {
	    this.userAgentString = uas.trim();
  }

  public String getUserAgentString() {
	    return userAgentString;
  }
  
  
  public int getKeepAliveSeconds() {
	    return keepAliveSeconds;
  }

  public void setKeepAliveSeconds(int keepAliveSeconds) {
	   this.keepAliveSeconds = keepAliveSeconds;
  }
  public void setLimit(int limit) {
	   this.limit = limit;
  }

  public int getLimit() {
	   return limit;
  }
	  
  public PatternLayoutEncoder getEncoder() {
	   return encoder;
  }

  public void setEncoder(PatternLayoutEncoder encoder) {
	   this.encoder = encoder;
  }
	  
  
  
  @Override
  public void start() {
	  
	  if (this.encoder == null) {
      addError("No encoder set for the appender named ["+ streamName +"].");
      return;
    }
    
    try {
      encoder.init(System.out);
      
    } catch (IOException e) {
    }
    super.start();
  }
  
  public void append(ILoggingEvent event){
   //LOGGER.info(streamName);
    if (counter >= limit) {
      return;
    }
    try {
         this.encoder.doEncode(event);
         ClientConfiguration  clientConfiguration = this.setClientConfig();
         ThreadPoolExecutor threadPoolExecutor = this.setThreadPoolExecuter();
         kinesisClient = this.setKinesisClient(clientConfiguration, threadPoolExecutor);

         try {
        	String  streamStatus  = this.getStreamStatus(kinesisClient,streamName);
            if (!StreamStatus.ACTIVE.name().equals(streamStatus) && !StreamStatus.UPDATING.name().equals(streamStatus)) {
                LOGGER.error("Stream " + streamName + " is not ready (in active/updating status) for appender: " + streamName);
                return;
            }
        } catch (ResourceNotFoundException rnfe) {
                LOGGER.error("Stream " + streamName + " doesn't exist for appender: " + streamName+ rnfe.getErrorMessage());      
        }
          
        try {
        	this.doAppend(kinesisClient,event);

        } catch (Exception e) {
            LOGGER.error("Failed to schedule log entry for publishing into Kinesis stream: " + streamName);
        }
        
    } catch (IOException e) {
         LOGGER.error("Failed to schedule log entry for publishing into Kinesis stream: " + e.getMessage());
    }

    // prepare for next event
    counter++;
  }
  
  
  private ClientConfiguration setClientConfig(){
	  ClientConfiguration clientConfiguration = new ClientConfiguration();
      clientConfiguration.setMaxErrorRetry(maxRetries);
      clientConfiguration.setRetryPolicy(new RetryPolicy(PredefinedRetryPolicies.DEFAULT_RETRY_CONDITION,
      PredefinedRetryPolicies.DEFAULT_BACKOFF_STRATEGY, maxRetries, true));
      clientConfiguration.setUserAgent(userAgentString);
      return clientConfiguration;
  }
  
  private ThreadPoolExecutor setThreadPoolExecuter(){
	  BlockingQueue<Runnable> taskBuffer = new LinkedBlockingDeque<Runnable>(bufferSize);
	  ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(threadCount, threadCount,
		         keepAliveSeconds, TimeUnit.SECONDS, taskBuffer, new BlockFastProducerPolicy());
	  threadPoolExecutor.prestartAllCoreThreads();
	  return threadPoolExecutor;
  }
  
  private AmazonKinesisAsyncClient setKinesisClient(ClientConfiguration clientConfiguration,ThreadPoolExecutor threadPoolExecutor){
	  kinesisClient = new AmazonKinesisAsyncClient(new DefaultAWSCredentialsProviderChain(),clientConfiguration,threadPoolExecutor);
      kinesisClient.setRegion(Region.getRegion(Regions.fromName(region)));
	  return kinesisClient;
  }
  
  private String getStreamStatus(AmazonKinesisAsyncClient kinesisClient,String streamName) throws ResourceNotFoundException{
	  DescribeStreamResult describeResult = kinesisClient.describeStream(streamName);
      String streamStatus = describeResult.getStreamDescription().getStreamStatus();
	  return streamStatus;
  }
  
  private void doAppend(AmazonKinesisAsyncClient kinesisClient, ILoggingEvent event) throws Exception{
	  ByteBuffer data = ByteBuffer.wrap(event.getMessage().getBytes(encoding));
	  asyncCallHander = new AsyncPutCallStatsReporter("KinesisAppender");
      kinesisClient.putRecordAsync(new PutRecordRequest().withPartitionKey(UUID.randomUUID().toString())
    .withStreamName(streamName).withData(data), asyncCallHander);
	  
  }
  
}
