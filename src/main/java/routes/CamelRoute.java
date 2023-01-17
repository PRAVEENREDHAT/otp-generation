package routes;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.crypto.CryptoDataFormat;
import org.apache.camel.converter.stream.InputStreamCache;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.Unremovable;
import io.quarkus.logging.Log;

@ApplicationScoped@Unremovable
public class CamelRoute extends RouteBuilder {
    @ConfigProperty(name = "HASHKEY")
    public String password;
    @Override
    public void configure() throws Exception {
    

    byte[] keyBytes = password.getBytes();
    SecretKeyFactory factory = SecretKeyFactory.getInstance("DES");
    SecretKey key = factory.generateSecret(new DESKeySpec(keyBytes));

    CryptoDataFormat cryptoFormat = new CryptoDataFormat("DES",key);
        cryptoFormat.setShouldAppendHMAC(true);
        cryptoFormat.setMacAlgorithm("HmacSHA256");
    
    rest("/v1/OTP/generate")
        .id("gen1")
        .post()
        .enableCORS(true)
        .to("direct:generate");

    rest("/v1/OTP/verify")
        .id("ver1")
        .post()
        .enableCORS(true)
        .to("direct:verify");
        
    from("direct:generate")
        .id("gen2")
        .unmarshal().json()
        .log("Entered: ${body}")
        .setHeader("otpdigit").simple("{{OTPdigit}}")
        .setHeader("mnumber",simple("${body[mobNum]}"))
        .setHeader("minKey",constant("{{lowRange}}"))
        .setHeader("maxKey",constant("{{highRange}}"))
        .setHeader("otptime", simple("${body[otpTime]}"))
        .choice().when(simple("${body} == null || ${body[mobNum]} == null || ${body[otpTime]} == null"))
        .throwException(NullPointerException.class, "Bad Input").end()
        .setHeader("otp",simple("${random(${header.minKey},${header.maxKey})}"))
        .removeHeader("minKey")
        .removeHeader("maxKey")
        .log("otp generate ->>>>:   ${header.otp}")
        .setHeader("otplimit",method(TimeNow.class, "addTime"))
        // .setHeader("otplimit",simple("${date:now:yyyyMMddhhmmssSSS}"))
        .setBody(simple("${header.mnumber}.${header.otp}.${header.otplimit}"))
        .log("otpcode: ${body}")
        .marshal(cryptoFormat)
            .process(x->{
                StringBuilder hex = new StringBuilder();
                InputStreamCache h = (InputStreamCache)x.getIn().getBody();
                byte[] re=h.readAllBytes();
                Log.info(re);
                // Iterating through each byte in the array
                for (byte i : re) {
                    hex.append(String.format("%02X", i));
                }
                x.getIn().setBody(hex);
            })
        .choice().when(simple("${headers.password}!= 974321563589"))
        .removeHeader("otp")
        .end()
        .setBody(simple("${body}.${header.otplimit}"))
        .log("hex is : ${body}")
        .marshal().json()
        ;   

    from("direct:verify")
        .id("ver2")
        .unmarshal().json()
        .log("Entered verify: ${body}")
        .setHeader("id").simple("${body[id]}")
        .setHeader("mnumber",simple("${body[mobNum]}"))
        .setHeader("otp", simple("${body[otp]}"))
        .log("${headers}")
        .setHeader("otplimit",simple("${header.id.split('\\.')[1]}"))
        .setBody(simple("${header.mnumber}.${header.otp}.${header.otplimit}"))
        .log("${body}")
        .marshal(cryptoFormat)
        .process(x->{
            StringBuilder hex = new StringBuilder();
            InputStreamCache h = (InputStreamCache)x.getIn().getBody();
            byte[] re=h.readAllBytes();
            Log.info(re);
            // Iterating through each byte in the array
            for (byte i : re) {
                hex.append(String.format("%02X", i));
            }
            x.getIn().setBody(hex);
        })
        .setHeader("timenow",method(TimeNow.class, "getTime"))
        .setBody(simple("${body}.${headers.otplimit}"))
        .choice().when(simple("'${body}' == '${header.id}' && ${headers.otplimit} >= ${header.timenow} "))
        .setBody(simple("{\"status\":\"otpVerified\"}")).endChoice()
        .otherwise()
        .setBody(simple("{\"status\":\"otpNotVerified\"}")).end()
        ;
    
}}
