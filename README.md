# Spring Xd Rollover File Sink

Rollover file sink that is rolled on preconfigured intervals.
File is rolled over every *rolloverPeriodMs*, starting from *rolloverStartTimeMs*. The *filename* must 
include the string __yyyy_mm_dd__, which is replaced with the actual date when creating and rolling over the file.

Old files are retained for a *retainDays* number of days before being deleted.

## Build

```
./gradlew clean build
```

## Upload module

```
xd:>module upload --file <path to>/rollover-file-1.0.0.BUILD-SNAPSHOT.jar --type sink --name rollover-file
```

## Use

```
xd>stream create --name rolloverFileTest --definition "time | rollover-file --filename=test_yyyy_mm_dd --dateFormat=yyyy_mm_dd_HHmm --retainDays=1 --flushRate=2" --deploy 
```

### Options

<table>
	<thead>
		<tr>
			<th><sub>Property</sub></th>
			<th><sub>Description</sub></th>
			<th><sub>Default Value</sub></th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td><sub>filename</sub></td>
			<td><sub>The filename must include the string yyyy_mm_dd which is replaced with the actual date when creating and rolling over the file.</sub></td>
			<td><sub>none</sub></td>
		</tr>
		<tr>
			<td><sub>append</sub></td>
			<td><sub>If true, existing files will be appended to.</sub></td>
			<td><sub>true</sub></td>
		</tr>
		<tr>
			<td><sub>retainDays</sub></td>
			<td><sub>The number of days to retain files before deleting them. 0 to retain forever.</sub></td>
			<td><sub>0</sub></td>
		</tr>
		<tr>
			<td><sub>dateFormat</sub></td>
			<td><sub>The format for the date file substitution. The default is yyyy_MM_dd.</sub></td>
			<td><sub>yyyy_MM_dd</sub></td>
		</tr>
		<tr>
			<td><sub>backupFormat</sub></td>
			<td><sub>The format for the file extension of backup files. The default is HHmmssSSS.</sub></td>
			<td><sub>HHmmssSSS</sub></td>
		</tr>
		<tr>
			<td><sub>timeZoneID</sub></td>
			<td><sub>TimeZone ID</sub></td>
			<td><sub>Europe/Amsterdam</sub></td>
		</tr>
		<tr>
			<td><sub>bufferSize</sub></td>
			<td><sub>Output stream buffer size. If set to -1 no buffering is used.</sub></td>
			<td><sub>8192</sub></td>
		</tr>
		<tr>
			<td><sub>flushRate</sub></td>
			<td><sub>After how many messages the output buffer is flushed. If if zero then flush only on rollover event.</sub></td>
			<td><sub>0</sub></td>
		</tr>
		<tr>
			<td><sub>startRolloverNow</sub></td>
			<td><sub>Should it start the rollover trigger now or at midnight.</sub></td>
			<td><sub>false</sub></td>
		</tr>
		<tr>
			<td><sub>rolloverPeriod</sub></td>
			<td><sub>Period [ms] to repeat the file rollover.</sub></td>
			<td><sub>86400000</sub></td>
		</tr>		
	</tbody>	  	
</table>


## Remove module

```
xd:>module delete --name sink:rollover-file
```