package hcmute.edu.vn.findora.model;

public class FinderUser {
    private String uid;
    private String name;
    private String photoUrl;

    public FinderUser(String uid, String name, String photoUrl) {
        this.uid = uid;
        this.name = name;
        this.photoUrl = photoUrl;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }
}
