package org.bouil.twifip;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Result;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.User;
import de.umass.lastfm.scrobble.ScrobbleData;
import de.umass.lastfm.scrobble.ScrobbleResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

public class TwifipUtils {

    private static final Logger log = Logger.getLogger(TwifipUtils.class.getName());

    private static TwitterFactory twitterFactory = new TwitterFactory();

    private static String lastFmUser;

    private static String lastFmPassword;

    private static String lastfmApiKey;

    private static String lastFmApiSecret;

    public TwifipUtils() {
        Properties p = new Properties();
        try {
            p.load(getClass().getResourceAsStream("/lastfm.properties"));
            lastFmUser = p.getProperty("login");
            lastFmPassword = p.getProperty("password");
            lastfmApiKey = p.getProperty("apiKey");
            lastFmApiSecret = p.getProperty("apiSecret");
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public void updateLastfm(String current, PrintWriter writer) {
        TrackInfo trackInfo = new TrackInfo(current);
        Session session = Authenticator.getMobileSession(lastFmUser, lastFmPassword, lastfmApiKey, lastFmApiSecret);
        ScrobbleData scrobbleData = new ScrobbleData();
        scrobbleData.setAlbum(trackInfo.album);
        scrobbleData.setArtist(trackInfo.artist);
        scrobbleData.setTrack(trackInfo.title);
        scrobbleData.setTimestamp((int) (System.currentTimeMillis() / 1000));

        ScrobbleResult result = Track.scrobble(scrobbleData, session);
        writer.println(result);

        Result tagResult = Track.addTags(trackInfo.artist, trackInfo.title, "fip", session);
        writer.println(result);
    }

    public static class TrackInfo {
        public String title;
        public String artist;
        public String album;

        public TrackInfo(String current) {
            String[] split = current.trim().split(" - ");
            if (split.length >= 3) {
                title = split[0].trim();
                artist = split[1].trim();
                album = split[2].trim();
                int lastSpacePosition = album.lastIndexOf(' ');
                if (lastSpacePosition != -1) {
                    String year = album.substring(lastSpacePosition + 1, album.length()).trim();
                    try {
                        Integer.parseInt(year);
                        album = album.substring(0, lastSpacePosition).trim();
                    } catch (NumberFormatException e) {
                        // no year
                    }
                }
            }
        }

        @Override
        public String toString() {
            return "TrackInfo{" + "title='" + title + '\'' + ", artist='" + artist + '\'' + ", album='" + album + '\'' +
                    '}';
        }
    }

    public void updateTwitter(String current, PrintWriter writer) {
        try {
            Twitter twitter = twitterFactory.getInstance();
            Status status = twitter.updateStatus(current);
            writer.println(status.getId());
        } catch (TwitterException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            writer.println(e.getMessage());
        }
    }

    public String getCurrentFip() {
        try {
            URL url = new URL("http://players.tv-radio.com/radiofrance/metadatas/fipRSS_a_lantenne.html");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "ISO-8859-15"));
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
                ligne = ligne.replaceAll("</b></font><br><font face=\"arial\" size=\"1\" color=\"#ffffff\"><b>", " - ");
                ligne = ligne.replaceAll("<font face=\"arial\" size=\"2\" color=\"#ffffff\"><b>", "");
                ligne = ligne.replaceAll("<font face=\"arial\" size=\"2\" color=\"#ffffff\"><b>", "");
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
     *
     * @param args
     */
    public static void main(String[] args) {
        /* display current fip */
        final TwifipUtils twifipUtils = new TwifipUtils();
        String currentFip = twifipUtils.getCurrentFip();
        System.out.println(currentFip);
        System.out.println(new TrackInfo(currentFip));
        PrintWriter writer = new PrintWriter(System.out);
        twifipUtils.updateLastfm(currentFip, writer);
        writer.close();
    }
}

