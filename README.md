# jwebcrawler
a webcrawler made in java

## Run
Download the source code and execute the following command on the root folder 'webcrawler

## Command
1. For non-windows users ```./mvnw clean install; java -jar target/webcrawler-1.0-SNAPSHOT.jar <SUB_DOMAIN>```
1. For windows users ```mvnw.exe clean install; java -jar target/webcrawler-1.0-SNAPSHOT.jar <SUB_DOMAIN>```
Where <SUB_DOMAIN> is the url of the root sub-domain you want to crawl.
  
## Output
A tree structure of the subdomain pages reachable from the <SUB_DOMAIN>
 
