package com.devabhi.adminapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    EditText enterNumber, edOTP;
    Button btnGetOTP, btnVerify, btnLogout;
    private static final String CHANNEL_ID = "101";
    String TAG = "MAINACTIVITY";

    FirebaseAuth firebaseAuth;
    private String verificationId;
    LinearLayout linearLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        linearLayout = findViewById(R.id.linear);
        enterNumber = findViewById(R.id.editTextMobileNum);
        btnGetOTP = findViewById(R.id.button);
        edOTP = findViewById(R.id.ed_otp);
        btnVerify = findViewById(R.id.btn_verify);
        btnLogout = findViewById(R.id.btn_logout);

        firebaseAuth = FirebaseAuth.getInstance();

        createNotificationChannel();
        getFCMToken();
        subscribeToTopic();

        setEvents();
    }

    private void setEvents() {

        btnLogout.setOnClickListener(v -> {
            firebaseAuth.signOut();
            Toast.makeText(MainActivity.this, "Logged out.", Toast.LENGTH_SHORT).show();
            btnGetOTP.setEnabled(true);
            btnVerify.setEnabled(true);
            btnLogout.setEnabled(false);
        });

        btnGetOTP.setOnClickListener(v -> {
            String phoneNum = enterNumber.getText().toString().trim();
            if (!phoneNum.isEmpty()) {
                if (phoneNum.length() == 10) {

                    setVerificationCode(phoneNum);

                }
            }
        });

        btnVerify.setOnClickListener(v -> {
            String otp = edOTP.getText().toString().trim();
            if (!otp.isEmpty()) {
                if(otp.length() != 6)
                    Toast.makeText(MainActivity.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                else
                    verifyCode(otp);
            } else {
                Toast.makeText(MainActivity.this, "Enter OTP", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser != null) {
            Toast.makeText(this, "User Already Logged In", Toast.LENGTH_SHORT).show();
            btnGetOTP.setEnabled(false);
            btnVerify.setEnabled(false);
            btnLogout.setEnabled(true);
        }
    }

    private void verifyCode(String code) {
        if(!code.isEmpty()) {
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
            signinByCredentials(credential);
        } else {

        }
    }

    private void signinByCredentials(PhoneAuthCredential credential) {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "Login Successful.", Toast.LENGTH_SHORT).show();
                            btnGetOTP.setEnabled(false);
                            btnVerify.setEnabled(false);
                            btnLogout.setEnabled(true);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Snackbar.make(linearLayout, "OTP is wrong", BaseTransientBottomBar.LENGTH_SHORT).show();
                    }
                });
    }

    private void setVerificationCode(String phoneNum) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(firebaseAuth)
                        .setPhoneNumber("+91" + phoneNum)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks
            mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            Log.d(TAG, "onVerificationCompleted:" + credential);

            final String code = credential.getSmsCode();
            if (code != null) {
                verifyCode(code);
            }
        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            // This callback is invoked in an invalid request for verification is made,
            // for instance if the the phone number format is not valid.
            Log.w(TAG, "onVerificationFailed", e);
            Toast.makeText(MainActivity.this, "Verification Failed.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCodeSent(@NonNull String s,
                               @NonNull PhoneAuthProvider.ForceResendingToken token) {
            super.onCodeSent(s, token);

            verificationId = s;
            Toast.makeText(MainActivity.this, "Code sent", Toast.LENGTH_SHORT).show();
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            Log.d(TAG, "onCodeSent:" + verificationId);

        }
    };

    private void getFCMToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                    return;
                }
                // Get new FCM registration token
                String token = task.getResult();

                Log.d(TAG, token);

            }
        });
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "FCMChannel";
            String description = "DESC of FCM notification";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void subscribeToTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("newsletter")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
//                        String msg = getString(R.string.msg_subscribed);
//                        if (!task.isSuccessful()) {
//                            msg = getString(R.string.msg_subscribe_failed);
//                        }
//                        Log.d(TAG, msg);
                        Toast.makeText(MainActivity.this, "Topic Subscribed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
