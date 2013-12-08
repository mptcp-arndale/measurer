package com.mikkojuola;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.Runnable;
import java.lang.Thread;
import java.lang.Math;
import java.nio.charset.Charset;
import java.util.regex.*;
import java.util.Set;
import java.util.Queue;
import java.util.LinkedList;

import android.util.Log;

/* This class is for capturing the amount of traffic on each network interface.
 **/
public class Interfaces
{
    // Map from interface names to traffic data.
    private HashMap<String, Traffic> interfaces =
        new HashMap<String, Traffic>();

    // How often to poll /proc/net/dev (in milliseconds)
    private int samplingInterval = 250;

    // Current time in milliseconds from the creation of an object of this
    // class.
    private long currentTime = 0;
    private long startTime = System.currentTimeMillis();

    public Set<String> getInterfaces()
    {
        return interfaces.keySet();
    }

    public synchronized long getCurrentTime()
    {
        currentTime = System.currentTimeMillis() - startTime;
        return currentTime;
    }

    public class Traffic
    {
        // Array where each item is the amount of traffic after i*N
        // milliseconds, where i is the index and N is the sampling interval.
        public HashMap<Long, Long> totals = new HashMap<Long, Long>();
        public HashMap<Long, Long> deltas = new HashMap<Long, Long>();
        long last = -1;

        private void push(long received, long transmitted)
        {
            // (maybe) TODO: use separate received/transmitted instead of
            // lumping them into one.
            long total = received + transmitted;
            totals.put(currentTime, total);
            if ( last != -1 ) {
                deltas.put(currentTime, total - last);
            }
            last = total;
        }

        private Traffic copy()
        {
            Traffic result = new Traffic();
            for (HashMap.Entry<Long, Long> entry : totals.entrySet()) {
                result.totals.put( entry.getKey(), entry.getValue() );
            }
            for (HashMap.Entry<Long, Long> entry : deltas.entrySet()) {
                result.deltas.put( entry.getKey(), entry.getValue() );
            }
            result.last = last;
            return result;
        }
    }

    public Interfaces()
    {
        new Thread(new Runnable () {
            @Override
            public void run() {
                try {
                    while(true) {
                        processProcNetDev();
                        Thread.sleep(samplingInterval);
                    }
                } catch ( Exception e ) {
                    return;
                }
            }
        }).start();
    }

    public synchronized HashMap<String, Traffic> getTraffic()
    {
        // Deep copy of 'interfaces'.
        HashMap<String, Traffic> result = new HashMap<String, Traffic>();
        for (HashMap.Entry<String, Traffic> entry : interfaces.entrySet()) {
            result.put( entry.getKey()   // Strings are immutable
                      , entry.getValue().copy() );
        }
        return result;
    }

    private Pattern devmatch =
        //                 interface name
        //                     |
        //                     |     bytes received
        //                     |         |
        //                     v         v
        Pattern.compile("\\s*(.*):\\s*([0-9]+)\\s*[0-9]+\\s*" +
                        "[0-9]+\\s*[0-9]+\\s*[0-9]+\\s*[0-9]+\\s*[0-9]+\\s*[0-9]+\\s*" +
                        "([0-9]+).*");
        //                  ^
        //                  |
        //           bytes transmitted

    private synchronized void processProcNetDev() throws IOException
    {
        currentTime = System.currentTimeMillis() - startTime;

        FileInputStream fis = new FileInputStream("/proc/net/dev");
        BufferedReader br = new
            BufferedReader(new InputStreamReader( fis
                                                , Charset.forName("UTF-8")));

        // Discard the first and second line, they contain no information
        br.readLine();
        br.readLine();

        String line;

        while ( (line = br.readLine()) != null ) {
            Matcher m = devmatch.matcher(line);
            m.matches();

            String interface_name = m.group(1);
            long received = Long.parseLong(m.group(2));
            long transmitted = Long.parseLong(m.group(3));

            if ( !interfaces.containsKey(interface_name) ) {
                interfaces.put(interface_name, new Traffic());
            }
            Traffic t = interfaces.get(interface_name);
            t.push(received, transmitted);
        }

        fis.close();
    }
}

