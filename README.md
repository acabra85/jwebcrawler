[![Build Status](https://travis-ci.org/acabra85/jwebcrawler.svg?branch=master)](https://travis-ci.org/acabra85/jwebcrawler)
[![codecov](https://codecov.io/gh/acabra85/jwebcrawler/branch/master/graph/badge.svg)](https://codecov.io/gh/acabra85/jwebcrawler)

# jwebcrawler
A webcrawler made in java

## Run
Download the source code and execute the following command inside the **root folder** 'jwebcrawler'

## Command
1. Execute this ```./mvnw clean install; java -jar target/jwebcrawler-2.0-SNAPSHOT.jar <SUB_DOMAIN> {TOTAL_TIMEOUT} {WORKER_COUNT} {WORKER_AWAIT_TIME} {PRINT_RESULTS_TO_FILE} {MAX_SITE_NODE_LINKS} {MAX_SITE_HEIGHT}```
* **<SUB_DOMAIN>** is the uri of the root sub-domain you want to crawl. (Mandatory)
* **{TOTAL_TIMEOUT}** is the total amount of time in seconds a worker should wait before retrying. (Optional, Default: 30 seconds)
* **{WORKER_COUNT}** is the total amount of desired concurrent workers. **See notes below** (Optional, Default: 1)
* **{WORKER_AWAIT_TIME}** is the total amount of time in seconds a worker should wait after an item was processed from the queue. (Optional, Default: 1 second)
* **{PRINT_RESULTS_TO_FILE}** Request to print results to a file that will be located in the results/ folder (Optional, Default: false)
* **{MAX_SITE_NODE_LINKS}** if 0 does not limit the children count per node (Optional, default 10)
* **{MAX_SITE_HEIGHT}** if 0 does not limit the height of the tree site (Optional, default 6)
### e.g. 
```./mvnw clean install; java -jar target/jwebcrawler-1.0-SNAPSHOT.jar http://localhost:8000```
## Output

For the above command with a server localhost in port 8000, serving the folder src/test/resources/site. will show the following
```
---- Results for [http://localhost:8000] ----

Total Concurrent Workers: 1
Total Pages crawled: 8
Total Links Discovered: 11
Total Links not downloadable due reporting failures: 3
Total Links rejected after timeout: 0
Total Links redirected: 0
Total time taken: 30.993 seconds.

---------- Site Map ---------------
http://localhost:8000/
---http://localhost:8000/a2.html
------http://localhost:8000/a5.html
------http://localhost:8000/index.html
---http://localhost:8000/a3.html
------http://localhost:8000/a6.html
---------http://localhost:8000/a6redirect.html
---http://localhost:8000/a4.html
------http://localhost:8000/a7.html
---------http://localhost:8000/a8.html
------------http://localhost:8000/a9.html

-----------------------------------
```
Where the amount of dashes indicates the hierarchy as how the site was traversed starting from the root <SUB_DOMAIN> in 
this case http://localhost:8000.
 
 
## Notes
The worker crawling mechanism is based on simple consumer-producer queue. A fix set amount of workers are created and 
once a timeout is reached they are terminated using a POISON_PILL mechanism, that indicates the thread to complete the loop
of execution.

There are multiple things to consider further in terms of failures:
1. Most websites have in place throttling, ideally the crawler should adapt for this when receiving 429 http status code as response.
2. Multiple filters can be created for URLs, this case is set for SameSite urls but this can be extended easily with a dedicated Filter interface and the decorator pattern.

## Defaults
The file ```/src/main/resources/config.json``` contains the defaults for execution
```
{
     "maxExecutionTime": 0, // by default there is no limit, in seconds 
     "workerCount": 1, // total workers
     "sleepTime": 1, // sleep time of workers before retry on an empty queue (seconds)
     "siteHeight": 6, // how deep to traverse the site-tree
     "maxSiteNodeLinks": 10 // how many maximum children per tree-site-node
     "reportToFile": false // should print report to a file (by default prints to console) 
   }
```

## Development

### Unit Tests
Run ```mvn test```

### Mutation tests
Run ``` mvn org.pitest:pitest-maven:mutationCoverage```

Results of mutation test available under folder /target/pit-reports/
