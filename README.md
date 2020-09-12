[![Build Status](https://travis-ci.org/acabra85/jwebcrawler.svg?branch=master)](https://travis-ci.org/acabra85/jwebcrawler)
[![codecov](https://codecov.io/gh/acabra85/jwebcrawler/branch/master/graph/badge.svg)](https://codecov.io/gh/acabra85/jwebcrawler)

# jwebcrawler
A webcrawler made in java

## Run
Download the source code and execute the following command inside the **root folder** 'jwebcrawler'

## Command
1. Execute this ```./mvnw clean install; java -jar target/jwebcrawler-1.0-SNAPSHOT.jar <SUB_DOMAIN> {WORKER_COUNT} {WORKER_AWAIT_TIME} {TOTAL_TIMEOUT} {PRINT_RESULTS_TO_FILE}```
* **<SUB_DOMAIN>** is the uri of the root sub-domain you want to crawl. (Mandatory)
* **{WORKER_COUNT}** is the total amount of desired concurrent workers. **See notes below** (Optional, 
Default: Available cores returned by Java Virtual Machine)
* **{WORKER_AWAIT_TIME}** is the total amount of time in seconds a worker should wait before retrying. (Optional, Default: 1 second)
* **{TOTAL_TIMEOUT}** is the total amount of time in seconds a worker should wait before retrying. (Optional, Default: 1 second)
* **{PRINT_RESULTS_TO_FILE}** Request to print results to a file that will be located in the results/ folder (Optional, Default: false)

### e.g. 
  
## Output
```./mvnw clean install; java -jar target/jwebcrawler-1.0-SNAPSHOT.jar http://localhost:8000```

For the above command with a server localhost in port 8000, serving the folder /site. will show the following
```
---- Site Map [http://localhost:8000] ----

http://localhost:8000
---http://localhost:8000/a2.html
------http://localhost:8000/a5.html
------http://localhost:8000/index.html
---http://localhost:8000/a3.html
------http://localhost:8000/a6.html
------http://localhost:8000/index.html
---http://localhost:8000/a4.html
------http://localhost:8000/a7.html
---------http://localhost:8000/a8.html
------------http://localhost:8000/a9.html
------http://localhost:8000/index.html

-------------------------
```
Where the amount of dashes indicates the hierarchy as how the site was traversed starting from the root <SUB_DOMAIN> in 
this case http://localhost:8000.
 
 
## Notes
The worker crawling mechanism is based on Idle Budget strategy, where a worker gets an initial (8) amount of idle units
and every time a task is completed it is awarded additional idle units (2).
If there is no work available (idle worker) the worker will redeem one idle unit from his budget and sleep a given time 
before retrying again.

The idea is that if a worker retried/waited enough time without being able to perform any work he can exit gracefully.
And if he was able to complete a task additional working units are awarded (since processing one task will generate
more work e.g. The html is downloaded and contains links that have not been crawled).

## Defaults
The file ```/src/main/resources/config.json``` contains the defaults for execution
```
{
     "workerCount": 1, // total workers
     "sleepTime": 1, // sleep time of workers before retry on an empty queue (seconds)
     "maxExecutionTime": 0, // by default there is no limit, in seconds 
     "siteHeight": 6, // how deep to traverse the site-tree
     "maxSiteNodeLinks": 10 // how many maximum children per tree-site-node
     "reportToFile": false // should print report to a file (by default prints to console) 
   }
```
