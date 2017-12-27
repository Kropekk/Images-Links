package sample;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;


public class UrlResult {
    private String address;
    private ArrayList<MyLink> hyperlinks = new ArrayList<>();

    private long imagesSize = 0;

    private long numberOfImages = 0;

    public UrlResult(String s) {
        this.address = s;
    }

    public String getAddress() {
        return address;
    }

    public ArrayList<MyLink> getHyperlinks() {
        return hyperlinks;
    }

    public long getImagesSize() {
        return imagesSize;
    }

    public long getNumberOfImages() {
        return numberOfImages;
    }

    public UrlResult handle() {
        Document doc;
        try {
            doc = Jsoup.connect(getAddress()).get();
        }
        catch (Exception e) {
            return null;
        }
        Elements links = doc.select("a[href]");
        for(Element e : links) {
            String s = e.attr("abs:href");
            if (s.startsWith("http://")) {
                hyperlinks.add(new MyLink(s));
            }
        }
        imagesSize = 0;
        numberOfImages = 0;
        HashSet<String> countedImages = new HashSet<>();
        links = doc.select("img");
        for (Element e : links) {
            String s = e.attr("abs:src");
            if (!countedImages.contains(s)) {
                countedImages.add(s);
                if (!s.startsWith("http://")) {
                    s = address + s;
                }
                try {
                    HttpURLConnection url = (HttpURLConnection) new URL(s).openConnection();
                    url.setRequestMethod("HEAD");
                    imagesSize += url.getContentLength();
                    ++numberOfImages;
                } catch (Exception ex) {}
            }
        }
        return this;
    }
}
