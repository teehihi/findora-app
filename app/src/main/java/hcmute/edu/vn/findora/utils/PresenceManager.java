package hcmute.edu.vn.findora.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages online presence using Firebase Realtime Database.
 * Structure: /presence/{uid}/online: true/false, lastSeen: timestamp
 */
public class PresenceManager {

    public interface OnPresenceListener {
        void onPresenceChanged(boolean isOnline, long lastSeenMillis);
    }

    private static final FirebaseDatabase db = FirebaseDatabase.getInstance();

    /**
     * Call when current user comes online (onStart of main activities).
     * Sets online=true and registers onDisconnect to set online=false automatically.
     */
    public static void goOnline() {
        String uid = getCurrentUid();
        if (uid == null) return;

        DatabaseReference ref = db.getReference("presence").child(uid);

        Map<String, Object> onlineData = new HashMap<>();
        onlineData.put("online", true);
        onlineData.put("lastSeen", ServerValue.TIMESTAMP);
        ref.setValue(onlineData);

        // Auto set offline when connection drops (handles kill/crash)
        Map<String, Object> offlineData = new HashMap<>();
        offlineData.put("online", false);
        offlineData.put("lastSeen", ServerValue.TIMESTAMP);
        ref.onDisconnect().setValue(offlineData);
    }

    /**
     * Call when current user goes offline (onStop — app no longer visible).
     */
    public static void goOffline() {
        String uid = getCurrentUid();
        if (uid == null) return;

        DatabaseReference ref = db.getReference("presence").child(uid);
        // Cancel the onDisconnect so it doesn't fire again later
        ref.onDisconnect().cancel();

        Map<String, Object> offlineData = new HashMap<>();
        offlineData.put("online", false);
        offlineData.put("lastSeen", ServerValue.TIMESTAMP);
        ref.setValue(offlineData);
    }

    /**
     * Listen to another user's presence.
     * Returns the ValueEventListener so caller can remove it later.
     */
    public static ValueEventListener listenToPresence(String uid, OnPresenceListener listener) {
        DatabaseReference ref = db.getReference("presence").child(uid);

        ValueEventListener vel = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    listener.onPresenceChanged(false, 0);
                    return;
                }
                Boolean online = snapshot.child("online").getValue(Boolean.class);
                Long lastSeen = snapshot.child("lastSeen").getValue(Long.class);
                listener.onPresenceChanged(
                    Boolean.TRUE.equals(online),
                    lastSeen != null ? lastSeen : 0
                );
            }

            @Override
            public void onCancelled(DatabaseError error) {
                listener.onPresenceChanged(false, 0);
            }
        };

        ref.addValueEventListener(vel);
        return vel;
    }

    /**
     * Stop listening to a user's presence.
     */
    public static void removePresenceListener(String uid, ValueEventListener listener) {
        db.getReference("presence").child(uid).removeEventListener(listener);
    }

    private static String getCurrentUid() {
        var user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }
}
