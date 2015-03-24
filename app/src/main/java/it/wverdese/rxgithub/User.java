package it.wverdese.rxgithub;

/**
 * Created by walter on 23/03/15.
 */
public class User {

    private int id;
    private String avatar_url;
    private String login;

    public int getId() {
        return id;
    }

    public String getImageUrl() {
        return avatar_url;
    }

    public String getName() {
        return login;
    }
}
