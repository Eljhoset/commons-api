[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.eljhoset/commons-api/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.eljhoset/commons-api)
# About
project that serves as a notepad of the things that I normally use when creating rest api
# How to use it?
1. Add dependency
```xml
<dependency>
    <groupId>com.github.eljhoset</groupId>
    <artifactId>commons-api</artifactId>
    <version>0.0.6</version>
</dependency>
```
2. Don't forget to add bytebuddy
```xml
<dependency>
    <groupId>net.bytebuddy</groupId>
    <artifactId>byte-buddy</artifactId>
</dependency>
```
# How it works?
## Exceptions
* All the exception get mapped to a more generic json response
```json
{
    "message": "Request could no be processed"
}
* Map the exception to a more manageable response for the UI.
```
```java
import com.eljhoset.controlleradvice.ApiException;
import org.springframework.http.HttpStatus;

@ApiException(status = HttpStatus.CONFLICT, code = 1014,message="Not good")
public class CutomException extends RuntimeException {
}
```
Produce the next response
```json
{
    "code": 1014,
    "message": "Not good"
}
```
or use the exception message
```java
import com.eljhoset.controlleradvice.ApiException;
import org.springframework.http.HttpStatus;

@ApiException(status = HttpStatus.CONFLICT, code = 1014)
public class CutomException extends RuntimeException {
}
...
@GetMapping("/{entityId}")
public String hey(@PathVariable("entityId") String entityId) {
	throw new CutomException(String.format("Entity %s not found",entityId));
}
```
```bash
curl -X GET \
  http://host/url/15
```
Produce the next response
```json
{
    "code": 1014,
    "message": "Entity 15 not found"
}
```
* Customize the Exception mapping
```java
import com.eljhoset.controlleradvice.ExceptionResponseInterseptor;
import org.springframework.stereotype.Component;
@Component
class CustomExceptionMapping implements ExceptionResponseInterseptor{

    @Override
    public Object handle(Exception exception) {
        return "Upps Something is broken";
    }
    
}
```
