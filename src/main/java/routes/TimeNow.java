package routes;

import java.sql.Timestamp;
import java.time.Instant;

import org.apache.camel.Handler;
import org.apache.camel.Header;

public class TimeNow {
    @Handler
    public Long addTime(@Header("otptime")  Integer value)
    {
        return Timestamp.from(Instant.now().plusSeconds((long)value*60)).getTime();
    }
    @Handler
    public Long getTime()
    {
        return Timestamp.from(Instant.now()).getTime();
    }
}
