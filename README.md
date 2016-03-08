# SpringXD Rolling File Sink

Rollover file sink that is rolled on preconfigured intervals.
File is rolled over every __rolloverPeriodMs__, starting from __rolloverStartTimeMs__. The __filename__ must 
include the string __yyyy_mm_dd__, which is replaced with the actual date when creating and rolling over the file.

Old files are retained for a __retainDays__ number of days before being deleted.

## Build

```
./gradlew clean build
```

The `rollover-file-1.0.0.BUILD-SNAPSHOT.jar` is produced in `./build/libs`. 

## Upload module

```
xd:>module upload --file <path to>/rollover-file-1.0.0.BUILD-SNAPSHOT.jar --type sink --name rollover-file
```

## Use

Roll over files either when the file size became 10K (e.g. 10240 bytes) or rollover time period of 30 sec. elapses.

```
xd>stream create --name rolloverFileTest --definition "time | rollover-file --filename=test_yyyy_mm_dd --dateFormat=yyyy_mm_dd_HHmmss --maxRolledFileSize=10240 --rolloverPeriod=30000 --archivePrefix=archive" --deploy 
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
			<td><sub>After how many messages the output buffer is flushed. When zero it flushes on file rollover only.</sub></td>
			<td><sub>0</sub></td>
		</tr>
		<tr>
			<td><sub>rolloverPeriod</sub></td>
			<td><sub>Time period between two consecutive roll over tasks (in milliseconds). If set to -1 then it defaults to 24 hours period starting from midnight.</sub></td>
			<td><sub>86400000 (~24h)</sub></td>
		</tr>
		<tr>
			<td><sub>maxRolledFileSize</sub></td>
			<td><sub>File size in bytes. When reached the file is rolled over. Set -1 to disable.</sub></td>
			<td><sub>-1</sub></td>
		</tr>
		<tr>
			<td><sub>archivePrefix</sub></td>
			<td><sub>Name prefix assigned to roll over files. Skipped if the value is empty.</sub></td>
			<td><sub>archive</sub></td>
		</tr>
		<tr>
			<td><sub>compressArchive</sub></td>
			<td><sub>If true the rolled files are compressed with gzip.</sub></td>
			<td><sub>true</sub></td>
		</tr>
		<tr>
			<td><sub>binary</sub></td>
			<td><sub>If false, will append a newline character at the end of each line.</sub></td>
			<td><sub>false</sub></td>
		</tr>						
	</tbody>	  	
</table>

## Remove module

```
xd:>module delete --name sink:rollover-file
```