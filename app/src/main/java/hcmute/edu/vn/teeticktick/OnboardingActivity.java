package hcmute.edu.vn.teeticktick;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.teeticktick.adapter.OnboardingAdapter;
import hcmute.edu.vn.teeticktick.model.OnboardingItem;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button btnNext, btnSkip;
    private OnboardingAdapter onboardingAdapter;
    private LinearLayout layoutOnboardingIndicators;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.view_pager);
        btnNext = findViewById(R.id.btn_next);
        btnSkip = findViewById(R.id.btn_skip);
        layoutOnboardingIndicators = findViewById(R.id.dots_layout);

        setupOnboardingItems();

        viewPager.setAdapter(onboardingAdapter);
        setupOnboardingIndicators();
        setCurrentOnboardingIndicator(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentOnboardingIndicator(position);
            }
        });

        btnSkip.setOnClickListener(v -> finishOnboarding());

        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            int total = onboardingAdapter.getItemCount();
            if (current < total - 1) {
                viewPager.setCurrentItem(current + 1);
            } else {
                finishOnboarding();
            }
        });
    }

    private void setupOnboardingItems() {
        List<OnboardingItem> onboardingItems = new ArrayList<>();

        OnboardingItem item1 = new OnboardingItem(
                R.drawable.teetickticknotext,
                "Quản lý thông minh",
                "Giải pháp toàn diện để tổ chức mọi công việc và nhiệm vụ một cách linh hoạt, phù hợp với mọi nhu cầu."
        );
        OnboardingItem item2 = new OnboardingItem(
                R.drawable.teetickticknotext, // Reusing logo or we can use ic_tasks if missing big vectors
                "Theo dõi chi tiết",
                "Theo dõi tiến độ, đặt nhắc nhở và quản lý lịch trình với công cụ trực quan và dễ sử dụng nhất."
        );
        OnboardingItem item3 = new OnboardingItem(
                R.drawable.teetickticknotext,
                "Đồng bộ liên tục",
                "Lưu trữ đám mây đảm bảo trải nghiệm quản lý tác vụ mạch lạc ở mọi nơi, mọi lúc."
        );

        onboardingItems.add(item1);
        onboardingItems.add(item2);
        onboardingItems.add(item3);

        onboardingAdapter = new OnboardingAdapter(onboardingItems);
    }

    private void setupOnboardingIndicators() {
        ImageView[] indicators = new ImageView[onboardingAdapter.getItemCount()];
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(8, 0, 8, 0);

        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(getApplicationContext());
            indicators[i].setImageDrawable(ContextCompat.getDrawable(
                    getApplicationContext(),
                    R.drawable.dot_inactive
            ));
            indicators[i].setLayoutParams(layoutParams);
            layoutOnboardingIndicators.addView(indicators[i]);
        }
    }

    private void setCurrentOnboardingIndicator(int index) {
        int childCount = layoutOnboardingIndicators.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) layoutOnboardingIndicators.getChildAt(i);
            if (i == index) {
                imageView.setImageDrawable(
                        ContextCompat.getDrawable(getApplicationContext(), R.drawable.dot_active)
                );
            } else {
                imageView.setImageDrawable(
                        ContextCompat.getDrawable(getApplicationContext(), R.drawable.dot_inactive)
                );
            }
        }
        
        if (index == onboardingAdapter.getItemCount() - 1) {
            btnNext.setText("Bắt đầu");
            btnNext.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.green_primary));
        } else {
            btnNext.setText("Tiếp theo");
        }
    }

    private void finishOnboarding() {
        SharedPreferences prefs = getSharedPreferences(SplashActivity.PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(SplashActivity.KEY_ONBOARDING_DONE, true).apply();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}

