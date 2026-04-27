package hcmute.edu.vn.findora;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.findora.adapter.VoucherAdapter;

public class VoucherMarketActivity extends AppCompatActivity {

    private ImageView btnBack;
    private RecyclerView rvVouchers;
    private VoucherAdapter adapter;
    private List<VoucherAdapter.Voucher> allVouchers;
    private List<VoucherAdapter.Voucher> filteredVouchers;
    private FirebaseFirestore db;
    private long currentPoints = 0;
    
    private TextView chipAll, chipHighlands, chipJollibee, chipCoffeeHouse, chipXanhSM;
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voucher_market);

        db = FirebaseFirestore.getInstance();
        initViews();
        setupFilterChips();
        fetchUserPoints();
        loadVouchers();

        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        rvVouchers = findViewById(R.id.rvVouchers);
        
        chipAll = findViewById(R.id.chipAll);
        chipHighlands = findViewById(R.id.chipHighlands);
        chipJollibee = findViewById(R.id.chipJollibee);
        chipCoffeeHouse = findViewById(R.id.chipCoffeeHouse);
        chipXanhSM = findViewById(R.id.chipXanhSM);
        
        allVouchers = new ArrayList<>();
        filteredVouchers = new ArrayList<>();
        rvVouchers.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
    }
    
    private void setupFilterChips() {
        chipAll.setOnClickListener(v -> filterVouchers("ALL"));
        chipHighlands.setOnClickListener(v -> filterVouchers("HIGHLANDS"));
        chipJollibee.setOnClickListener(v -> filterVouchers("JOLLIBEE"));
        chipCoffeeHouse.setOnClickListener(v -> filterVouchers("COFFEE_HOUSE"));
        chipXanhSM.setOnClickListener(v -> filterVouchers("XANH_SM"));
    }
    
    private void filterVouchers(String filter) {
        currentFilter = filter;
        updateChipStyles();
        
        filteredVouchers.clear();
        
        if (filter.equals("ALL")) {
            filteredVouchers.addAll(allVouchers);
        } else {
            for (VoucherAdapter.Voucher voucher : allVouchers) {
                if (filter.equals("HIGHLANDS") && voucher.brandName.contains("HIGHLANDS")) {
                    filteredVouchers.add(voucher);
                } else if (filter.equals("JOLLIBEE") && voucher.brandName.contains("JOLLIBEE")) {
                    filteredVouchers.add(voucher);
                } else if (filter.equals("COFFEE_HOUSE") && voucher.brandName.contains("COFFEE HOUSE")) {
                    filteredVouchers.add(voucher);
                } else if (filter.equals("XANH_SM") && voucher.brandName.contains("XANH SM")) {
                    filteredVouchers.add(voucher);
                }
            }
        }
        
        updateAdapter();
    }
    
    private void updateChipStyles() {
        // Reset all chips
        chipAll.setBackgroundResource(0);
        chipHighlands.setBackgroundResource(0);
        chipJollibee.setBackgroundResource(0);
        chipCoffeeHouse.setBackgroundResource(0);
        chipXanhSM.setBackgroundResource(0);
        
        int grayColor = ContextCompat.getColor(this, R.color.outline);
        chipAll.setTextColor(grayColor);
        chipHighlands.setTextColor(grayColor);
        chipJollibee.setTextColor(grayColor);
        chipCoffeeHouse.setTextColor(grayColor);
        chipXanhSM.setTextColor(grayColor);
        
        // Set active chip
        TextView activeChip = null;
        switch (currentFilter) {
            case "ALL": activeChip = chipAll; break;
            case "HIGHLANDS": activeChip = chipHighlands; break;
            case "JOLLIBEE": activeChip = chipJollibee; break;
            case "COFFEE_HOUSE": activeChip = chipCoffeeHouse; break;
            case "XANH_SM": activeChip = chipXanhSM; break;
        }
        
        if (activeChip != null) {
            activeChip.setBackgroundResource(R.drawable.bg_category_active);
            activeChip.setTextColor(ContextCompat.getColor(this, R.color.primary_dark));
        }
    }

    private void fetchUserPoints() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentPoints = documentSnapshot.getLong("points") != null ? documentSnapshot.getLong("points") : 0;
                        updateAdapter();
                    }
                });
    }

    private void loadVouchers() {
        // Mock data for demo if collection is empty
        allVouchers.add(new VoucherAdapter.Voucher(
            "1", 
            "Voucher giảm giá 25% tối đa 100k", 
            20, 
            "", 
            "XANH SM",
            "XANHWIN",
            R.drawable.greensm_xanhwin
        ));
        allVouchers.add(new VoucherAdapter.Voucher(
            "2", 
            "Voucher mua 1 tặng 1", 
            30, 
            "", 
            "HIGHLANDS COFFEE",
            "BUY1GET1",
            R.drawable.highland_1uy1get1
        ));
        allVouchers.add(new VoucherAdapter.Voucher(
            "3", 
            "Voucher đồng giá 39k", 
            50, 
            "", 
            "THE COFFEE HOUSE",
            "DONGIA39",
            R.drawable.thecfhouse_donggia39
        ));
        allVouchers.add(new VoucherAdapter.Voucher(
            "4", 
            "Voucher giảm giá 15% toàn menu", 
            80, 
            "", 
            "JOLLIBEE VIỆT NAM",
            "JOLLI15PER",
            R.drawable.jollibee_15per
        ));
        
        filteredVouchers.addAll(allVouchers);
        updateAdapter();

        // Optional: Real data from Firestore
        /*
        db.collection("vouchers").get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                allVouchers.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    // Map to Voucher object
                }
                filteredVouchers.clear();
                filteredVouchers.addAll(allVouchers);
                updateAdapter();
            }
        });
        */
    }

    private void updateAdapter() {
        if (adapter == null) {
            adapter = new VoucherAdapter(this, filteredVouchers, currentPoints);
            rvVouchers.setAdapter(adapter);
        } else {
            adapter = new VoucherAdapter(this, filteredVouchers, currentPoints);
            rvVouchers.setAdapter(adapter);
        }
    }
}
