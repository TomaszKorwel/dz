package net.sf.dz3.view.http.v3;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.junit.Test;

import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.device.sensor.impl.NullSensor;
import net.sf.dz3.instrumentation.Marker;
import net.sf.dz3.view.http.common.QueueFeeder;
import net.sf.dz3.view.http.v3.ZoneSnapshotList.ZoneSnapshot;
import net.sf.jukebox.datastream.signal.model.DataSample;

public class ProtobufRendererTest {

    private final Logger logger = Logger.getLogger(getClass());

    @Test
    public void testRender0() {

        NDC.push("testRender");

        try {

            ThermostatModel t = new ThermostatModel("test-thermostat", new NullSensor("null-address", 100), 20, 1, 0.000002,0, 2);
            Map<String, Object> context = new HashMap<String, Object>();
            BlockingQueue<ZoneSnapshot> queue = new LinkedBlockingQueue<ZoneSnapshot>();

            context.put(QueueFeeder.QUEUE_KEY, queue);

            ThermostatRenderer tr = new ThermostatRenderer(t, context, null);
            
            long timestamp = System.currentTimeMillis();
            DataSample<Double> demand = new DataSample<Double>(timestamp, "source-d", "signature-d", new Double(0), null);
            ThermostatSignal ts = new ThermostatSignal(true, true, true, true, demand);
            
            Marker m = new Marker("consume");

            tr.consume(new DataSample<ThermostatSignal>(timestamp, "source-ts", "signature-ts", ts, null));
            
            m.close();

            assertEquals("wrong queue size", 1, queue.size());
            
            ZoneSnapshot head = queue.peek();
            
            String actual = head.toString().replaceAll("\n", " ");
            String expected =
                    "# " + head.getClass().getName() + "@" + Integer.toHexString(head.hashCode()) +
                    " enabled: true mode: COOLING mode_value: -1 name: \"test-thermostat\" on_hold: true setpoint_temperature: 20.0 state: CALLING state_value: 1 timestamp: " + timestamp + " voting: true";

            logger.debug("queue head (" + head.toByteString().size() + " bytes): " + actual);
            
            assertEquals("wrong toString()", expected, actual);

        } finally {
            NDC.pop();
        }
    }
}
