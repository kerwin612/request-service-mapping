# request-service-mapping  
  **A ribbon plug-in that supports mapping requests to the target server by configuration.**    
  *Only support used with `spring-cloud-gateway(version≥2.1.0.RELEASE)` and `spring-cloud-feign(version≥2.1.0.RELEASE)`.*

`client   ---request--->   service_cluster[service_instance1, service_instance2, service_instance3]`  
In the above scenario, in `spring-cloud`, the service instance that ultimately responds to the client request is random by default:  
`client ---random---> service_instance`  
After the `request-service-mapping` is introduced, the specified service instance can be configured to respond to requests from the specified client:  
`client ---mapped---> service_instance`    
Configuration template (Match by regular expression):  
`{"client_ip" : "service_instance_ip"}`  


## Dependency  
**maven**:  
```xml
<dependency>
  <groupId>io.github.kerwin612</groupId>
  <artifactId>request-service-mapping</artifactId>
  <version>1.1</version>
</dependency>
```
**gradle**:  
```groovy
implementation 'io.github.kerwin612:request-service-mapping:1.1'
```  

## Configuration  
* `request.service.mapping`  
    type: **map**    
    default: **{"\.\*": "\.\*"}**    
    client and server mapping. In the map, the key is the client ip, the value is the server ip, matched by the regular expression.  
* `request.service.mapping.short`  
    type: **boolean**  
    default: **false**  
    whether it matches quickly. If set to true, matching to the first one will not continue matching.  
* `request.service.mapping.disable`  
    type: **boolean**  
    default: **false**   
    whether to disable.  
* `request.service.mapping.client-ip-header`  
    type: **string**  
    default: **\_\_CLIENT_IP\_\_**  
    the request header key for storing the client IP. After the component obtains the client IP, the client IP is stored in the request header with the request header key.  
* `request.service.mapping.get-client-ip-headers`  
    type: **array**  
    default: **X-Forwarded-For,Proxy-Client-IP,WL-Proxy-Client-IP,HTTP_CLIENT_IP,HTTP_X_FORWARDED_FOR**   
    the request header key of get the client IP. This component will preferentially obtain the client IP through this request header key.  

**By default, just configure `request.service.mapping`.**  
