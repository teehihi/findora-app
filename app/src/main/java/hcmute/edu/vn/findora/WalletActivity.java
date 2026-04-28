package hcmute.edu.vn.findora;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.findora.adapter.TransactionAdapter;
import hcmute.edu.vn.findora.model.Transaction;

public class WalletActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvBalanceAmount, tvTotalEarned, tvTotalSpent;
    private RecyclerView rvTransactions;
    private LinearLayout layoutEmpty;
    
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        initViews();
        setupRecyclerView();
        loadUserBalance();
        loadTransactions();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        loadUserBalance();
        loadTransactions();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvBalanceAmount = findViewById(R.id.tvBalanceAmount);
        tvTotalEarned = findViewById(R.id.tvTotalEarned);
        tvTotalSpent = findViewById(R.id.tvTotalSpent);
        rvTransactions = findViewById(R.id.rvTransactions);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        transactionList = new ArrayList<>();
        adapter = new TransactionAdapter(this, transactionList);
        
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);
    }

    private void loadUserBalance() {
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long points = documentSnapshot.getLong("points");
                        if (points != null) {
                            tvBalanceAmount.setText(String.valueOf(points));
                        } else {
                            tvBalanceAmount.setText("0");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    tvBalanceAmount.setText("0");
                });
    }

    private void loadTransactions() {
        android.util.Log.d("WalletActivity", "Loading transactions for user: " + currentUserId);
        
        db.collection("transactions")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    android.util.Log.d("WalletActivity", "Transactions loaded: " + queryDocumentSnapshots.size());
                    
                    transactionList.clear();
                    
                    android.util.Log.d("WalletActivity", "Found " + queryDocumentSnapshots.size() + " transactions");
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Transaction transaction = document.toObject(Transaction.class);
                        transaction.setId(document.getId());
                        transactionList.add(transaction);
                        
                        android.util.Log.d("WalletActivity", String.format(
                            "Transaction: %s, %d points, type: %s",
                            transaction.getTitle(),
                            transaction.getPoints(),
                            transaction.getType()
                        ));
                    }
                    
                    adapter.notifyDataSetChanged();
                    
                    // Calculate stats
                    int totalEarned = 0, totalSpent = 0;
                    for (Transaction t : transactionList) {
                        if ("earn".equals(t.getType())) totalEarned += t.getPoints();
                        else totalSpent += t.getPoints();
                    }
                    tvTotalEarned.setText(String.valueOf(totalEarned));
                    tvTotalSpent.setText(String.valueOf(totalSpent));
                    
                    // Show/hide empty state
                    if (transactionList.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvTransactions.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvTransactions.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("WalletActivity", "Error loading transactions", e);
                    android.widget.Toast.makeText(this, "Lỗi: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    
                    layoutEmpty.setVisibility(View.VISIBLE);
                    rvTransactions.setVisibility(View.GONE);
                });
    }
}
