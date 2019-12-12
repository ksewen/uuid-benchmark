# a micro benchmark test for uuid generator

## usage
### build
```bash
mvn clean package
```

### run
1. input command and run jar file
```bash
java -jar uuid-benchmark.jar
```
2. u must input some information for test
```bash
please input the benchmark type UUID/KUID: 
# only support UUID or KUID
UUID
# u can input a blank value, default file is /root/benchmark/benchmark-{type}-thread-{thread-counts}.log, type and thread-counts will replace with the correct value 
please input the output file: 
/root/benchmark/benchmark-UUID-thread-7.log
# only support integer, default is 1
please input the thread count: 
7
```