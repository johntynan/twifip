package org.bouil.twifip;

import static com.google.appengine.api.labs.taskqueue.TaskOptions.Builder.url;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.TaskOptions.Method;

public class Twifip extends HttpServlet {

    private static final long serialVersionUID = -5895034370912197747L;

    private static final Logger log = Logger.getLogger(Twifip.class.getName());

    private static final TwifipUtils twifipUtils = new TwifipUtils();

    private static String previous = null;

    public void init() throws ServletException {
        super.init();
        scheduleCall();
    }

    private void scheduleCall() {
        log.info("Queuing call in 15s");
        Queue queue = QueueFactory.getDefaultQueue();

        queue.add(url("/twifip").method(Method.GET).countdownMillis(15000));
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        synchronized (this) {

            final PrintWriter writer = resp.getWriter();
            resp.setContentType("text/plain");
            String current = twifipUtils.getCurrentFip();
            if (current != null) {
                if (!current.equals(previous)) {
                    writer.println(current);
                    log.info("Updating to " + current);
                    if (twifipUtils.updateTwitter(current, writer)) {
                        twifipUtils.updateLastfm(current, writer);
                    }
                    previous = current;
                } else {
                    log.info("same as previous " + current);
                    writer.println("Same as previous = " + current);
                }
            } else {
                log.info("null in fip html");
                writer.println("null");
            }
            scheduleCall();
        }
    }
}


