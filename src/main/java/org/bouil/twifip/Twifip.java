package org.bouil.twifip;

import static com.google.appengine.api.labs.taskqueue.TaskOptions.Builder.url;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.TaskOptions.Method;

public class Twifip extends HttpServlet {

    private static final long serialVersionUID = -5895034370912197747L;
    
    private static final Logger log = Logger.getLogger(Twifip.class.getName());

    private static TwitterFactory twitterFactory = new TwitterFactory();

    private static String previous = null;

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        final PrintWriter writer = resp.getWriter();
        resp.setContentType("text/plain");
        String current = getCurrentFip();
        if (current != null) {
            if (!current.equals(previous)) {
                writer.println(current);
                Twitter twitter = twitterFactory.getInstance();
                try {
                    log.info("Updating to " + current);
                    Status status = twitter.updateStatus(current);
                    previous = current;
                    writer.println(status.getId());
                } catch (TwitterException e) {
                    log.log(Level.SEVERE, e.getMessage(), e);
                    writer.println(e.getMessage());
                }
            } else {
                log.info("same as previous " + current);
                writer.println("Same as previous = " + current);
            }
        } else {
            log.info("null in fip html");
            writer.println("null");
        }
    }

    private String getCurrentFip() {
        try {
            URL url = new URL(
                    "http://players.tv-radio.com/radiofrance/metadatas/fipRSS_a_lantenne.html");
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    url.openStream(), "ISO-8859-15"));
            String line = null;
            StringBuilder sb = new StringBuilder();
            final String prefix = "<font face=\"arial\" size=\"2\" color=\"#ffffff\"><b>";
            final String suffix = "</b></font>";

            while ((line = reader.readLine()) != null) {
                if (line.startsWith(prefix) && line.endsWith(suffix)) {
                    sb.append(line);
                }
            }
            reader.close();

            String ligne = sb.toString();
            if (sb.length() > 0) {
                ligne = ligne
                        .replaceAll(
                                "</b></font><br><font face=\"arial\" size=\"1\" color=\"#ffffff\"><b>",
                                " - ");
                ligne = ligne
                        .replaceAll(
                                "<font face=\"arial\" size=\"2\" color=\"#ffffff\"><b>",
                                "");
                ligne = ligne
                        .replaceAll(
                                "<font face=\"arial\" size=\"2\" color=\"#ffffff\"><b>",
                                "");
                ligne = ligne.replaceAll("</b></font>", "");
            }
            return ligne;

        } catch (MalformedURLException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Test method
     * @param args
     */
    public static void main(String[] args){
        /* display current fip */
        final Twifip twifip = new Twifip();
        System.out.println(twifip.getCurrentFip());
    }
}
