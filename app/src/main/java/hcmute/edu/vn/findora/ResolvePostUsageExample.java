package hcmute.edu.vn.findora;

/**
 * USAGE EXAMPLE: How to show the ResolvePostBottomSheet
 * 
 * Add this code to your Activity (e.g., PostDetailActivity, MyPostsActivity)
 * where you have a "Đã giải quyết" button:
 * 
 * Example 1: From a button click
 * --------------------------------
 * Button btnResolve = findViewById(R.id.btnResolve);
 * btnResolve.setOnClickListener(v -> {
 *     ResolvePostBottomSheet bottomSheet = ResolvePostBottomSheet.newInstance(
 *         postId,      // The ID of the post to resolve
 *         postOwnerId  // The ID of the post owner (current user)
 *     );
 *     bottomSheet.show(getSupportFragmentManager(), "ResolvePostBottomSheet");
 * });
 * 
 * 
 * Example 2: From MyPostsAdapter
 * --------------------------------
 * In your adapter's onBindViewHolder:
 * 
 * holder.btnResolve.setOnClickListener(v -> {
 *     ResolvePostBottomSheet bottomSheet = ResolvePostBottomSheet.newInstance(
 *         post.getPostId(),
 *         post.getUserId()
 *     );
 *     bottomSheet.show(((AppCompatActivity) context).getSupportFragmentManager(), 
 *                      "ResolvePostBottomSheet");
 * });
 * 
 * 
 * Example 3: From PostDetailActivity
 * -----------------------------------
 * In PostDetailActivity.java, find the resolve button and add:
 * 
 * MaterialButton btnResolve = findViewById(R.id.btnResolve);
 * btnResolve.setOnClickListener(v -> {
 *     ResolvePostBottomSheet bottomSheet = ResolvePostBottomSheet.newInstance(
 *         postId,
 *         FirebaseAuth.getInstance().getCurrentUser().getUid()
 *     );
 *     bottomSheet.show(getSupportFragmentManager(), "ResolvePostBottomSheet");
 * });
 */
public class ResolvePostUsageExample {
    // This is just a documentation class
}
