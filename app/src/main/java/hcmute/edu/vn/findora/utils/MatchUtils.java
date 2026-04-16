package hcmute.edu.vn.findora.utils;

import android.location.Location;

import hcmute.edu.vn.findora.model.Post;

public class MatchUtils {

    /**
     * Calculates a matching score between two posts.
     * Higher score indicates a better match.
     *
     * @param p1 First post (e.g., a "Lost" post)
     * @param p2 Second post (e.g., a "Found" post)
     * @return Score >= 0.0
     */
    public static double calculateMatchScore(Post p1, Post p2) {
        if (p1 == null || p2 == null) return 0.0;

        double score = 0.0;

        // 1. Text Score (Title matching)
        String title1 = p1.getTitle() != null ? p1.getTitle().toLowerCase() : "";
        String title2 = p2.getTitle() != null ? p2.getTitle().toLowerCase() : "";
        if (!title1.isEmpty() && !title2.isEmpty()) {
            if (title1.equals(title2)) {
                score += 1.0;
            } else if (title1.contains(title2) || title2.contains(title1)) {
                score += 0.5;
            }
        }

        // 2. Location Score (Distance)
        if (p1.getLat() != null && p1.getLng() != null && p2.getLat() != null && p2.getLng() != null) {
            float[] results = new float[1];
            Location.distanceBetween(
                    p1.getLat(), p1.getLng(),
                    p2.getLat(), p2.getLng(),
                    results
            );
            float distanceMeters = results[0];
            
            // If very close (< 500m), big boost. 
            if (distanceMeters < 500) {
                score += 1.0;
            } else if (distanceMeters < 2000) {
                score += 0.5; // Within 2km
            } else if (distanceMeters < 5000) {
                score += 0.2; // Within 5km
            }
        }

        // 3. Image Score (AI classification matching)
        String imgLabel1 = p1.getImageLabel();
        String imgLabel2 = p2.getImageLabel();
        
        if (imgLabel1 != null && !imgLabel1.isEmpty() && imgLabel2 != null && !imgLabel2.isEmpty()) {
            if (imgLabel1.equalsIgnoreCase(imgLabel2)) {
                // Determine confidence adjustment
                double conf1 = p1.getConfidence() != null ? p1.getConfidence() : 0.5;
                double conf2 = p2.getConfidence() != null ? p2.getConfidence() : 0.5;
                
                // Increase score significantly for label match, scaled by their confidences
                score += (2.0 * (conf1 * conf2));
            }
        }

        return score;
    }
}
