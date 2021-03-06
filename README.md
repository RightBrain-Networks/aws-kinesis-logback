## aws-kinesis-logback ##
#### Logback spring config file in resources directory ####
```
<?xml version="1.0" encoding="UTF-8" ?>
<configuration scan="true">
<property name="applicationName" value="your-application-name"/>
<property resource="application.properties" />
<appender name="KINESIS"  class="com.rightbrainnetworks.aws_kinesis_logback.KinesisAppender">
    
<filter class="ch.qos.logback.classic.filter.ThresholdFilter">
  <level>INFO</level>
</filter>

<encoder>
  <pattern>%date %level [%thread] %logger{10} [%file:%line] %msg%n</pattern>
</encoder>
        
<streamName>${streamName}</streamName>
<encoding>${encoding}</encoding>
<maxRetries>${maxRetries}</maxRetries>
<bufferSize>${bufferSize}</bufferSize>
<threadCount>${threadCount}</threadCount>
<region>${region}</region>
<userAgentString>${userAgentString}</userAgentString>
<keepAliveSeconds>${keepAliveSeconds}</keepAliveSeconds>
<limit>${limit}</limit>
</appender>

<logger name = "com.rightbrainnetworks.aws_kinesis_logback" level="INFO">
  <appender-ref ref="KINESIS"/>
</logger> 
 
    
<root level="ERROR">
  <appender-ref ref="KINESIS"/>
</root>

</configuration> 
```


##### Further configs #####

##### RBN's kinesis appender can be used with a standalone java application as well, just write values directly in logback config above #####



######<i> application.properties file in resources directory </i>######
```

streamName = your-aws-kinesis-stream-name
encoding = UTF-8
maxRetries = 3
bufferSize = 2000
threadCount = 20
region = us-east-1
userAgentString = kinesis-logback-appender/1.0.1
keepAliveSeconds = 30
limit = 10
```

##### Further configs #####

##### Sample AWS credentials file on instance or you may have IAM roles setup on EC2. For more info on setting up aws creds refer to <a href="http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/credentials.html">AWS api documentation here</a> #####
###### <i>Sample ~/.aws/credentials file  </i>######

```
[default]

aws_access_key_id = YOUR_AWS_ACCESS_KEY_ID
aws_secret_access_key = YOUR_AWS_SECRET_ACCESS_KEY
```



