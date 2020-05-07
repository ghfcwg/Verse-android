package glass.chungwon.verse;

/**
 * Model class
 * <p></p>
 *
 */
public class VerseItem {

    public final String id;
    public final String content;
    public final String title;
    public final String reference;
    public final String url;
    public final Integer rating;

    public VerseItem(String id, String content, String title, String reference, String url, Integer rating) {
        this.id = id;
        this.content = content;
        this.title = title;
        this.reference = reference;
        this.url = url;
        this.rating = rating;
    }

}
