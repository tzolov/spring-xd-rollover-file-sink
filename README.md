# spring-xd-rollover-file-sink

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

## Remove module

```
xd:>module delete --name sink:rollover-file
```