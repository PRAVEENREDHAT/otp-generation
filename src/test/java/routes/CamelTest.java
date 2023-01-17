package routes;


import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.Test;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
@QuarkusTest
class CamelTest extends CamelQuarkusTestSupport {
    @Override
    public RouteBuilder createRouteBuilder(){
        CamelRoute cr=new CamelRoute();
        cr.password="89678w7f1fw67fsqyxug78s3";
        return cr;
    }

    @Test
     void checknotVerifiedoutput() throws Exception {
        AdviceWith.adviceWith(context, "gen2", a ->
        a.weaveAddLast().to("mock:output"));
        AdviceWith.adviceWith(context, "ver2", a ->
        a.weaveAddLast().to("mock:output2"));
        MockEndpoint mock = getMockEndpoint("mock://output");
        Log.info("mock value "+mock.getExpectedMinimumCount());
        mock.expectedMinimumMessageCount(1);
        String input="{\"mobNum\": \"7895018921\",\"otpTime\": 10}";
        // template.sendBody("direct:generate",input );
        template.sendBody("direct:generate",input );
        List<Exchange> list=mock.getReceivedExchanges();
        String Body=list.get(0).getIn().getBody(String.class);
        Log.info(Body);
        assertTrue(Body.matches("^\"[A-Za-z0-9]+.*[0-9]+\"$"));
        assertMockEndpointsSatisfied();
        String input2="{\"id\":"+Body+",\"otp\":\"215244\",\"mobNum\":\"7895018921\"}";
        Log.info("input2 "+input2);

        MockEndpoint mock2 = getMockEndpoint("mock://output2");
        Log.info("mock value "+mock2.getExpectedMinimumCount());
        mock2.expectedMinimumMessageCount(1);
        template.sendBody("direct:verify",input2 );
        List<Exchange> list2=mock2.getReceivedExchanges();

        String Body2=list2.get(0).getIn().getBody(String.class);
        Log.info("body2-> "+Body2);
        assertEquals("{\"status\":\"otpNotVerified\"}", Body2);
    }
    @Test
     void contextStart() throws Exception {
        Log.info("Asserting Camel Context stated on code-startup");
        assertTrue(context.isStarted());
    }

    @Test
     void contextIsAlive() throws Exception {
        Log.info("Asserting Camelcontext is alive");
        assertFalse(context.isStopped());
    }

    @Test
     void contextValidEndpoint() throws Exception {
        Log.info("asserting context has valid endpoints");
        assertTrue(context.getEndpoints().toString().contains("/v1/OTP/generate"));
    }

    @Test
    void contextValidEndpoint1() throws Exception {
       Log.info("asserting context has valid endpoints");
       assertTrue(context.getEndpoints().toString().contains("/v1/OTP/verify"));
   }

   @Test
   void testJMSRoute() throws Exception {

      assertMockEndpointsSatisfied();

  }
}
