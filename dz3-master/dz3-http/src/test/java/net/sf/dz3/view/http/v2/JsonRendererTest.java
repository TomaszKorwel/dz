package net.sf.dz3.view.http.v2;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.junit.Test;

import com.google.gson.Gson;

import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.device.sensor.impl.NullSensor;
import net.sf.dz3.instrumentation.Marker;
import net.sf.dz3.view.http.common.QueueFeeder;
import net.sf.jukebox.datastream.signal.model.DataSample;

public class JsonRendererTest {

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

            DataSample<Double> demand = new DataSample<Double>("source-d", "signature-d", new Double(0), null);
            ThermostatSignal ts = new ThermostatSignal(true, true, true, true, demand);

            Marker m = new Marker("consume");

            tr.consume(new DataSample<ThermostatSignal>("source-ts", "signature-ts", ts, null));
            
            m.close();

            assertEquals("wrong queue size", 1, queue.size());
            
            ZoneSnapshot head = queue.peek();
            
            // Let's see how long it is in JSON
            
            Gson gson = new Gson();
            String actual = gson.toJson(head);

            logger.debug("queue head (" + actual.length() + " bytes): " + actual);

            assertEquals("wrong toString()", "test-thermostat: Cooling, CALLING, signal=0.0, current=0.0, setpoint=20.0, on hold", queue.peek().toString());

        } finally {
            NDC.pop();
        }
    }
}
