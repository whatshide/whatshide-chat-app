package com.whatshide.android;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.whatshide.android.models.User;
import com.whatshide.android.utilities.Constants;

import org.w3c.dom.Text;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {
    CircleImageView profile;
    TextView name,mobileNumber;
    FirebaseAuth mAuth;
    FirebaseFirestore mDatabase;
    RelativeLayout signout,edit_profile,security;
    ProgressBar progressBar;
    ImageView back;
    String mine;

    private EditText securityPassword;
    private RelativeLayout securityContainer;
    private TextView securitySubmit;
    private View cover;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        bindView();

        initDB();

        getUserData();

        setListener();
    }

    private void setListener() {
        back.setOnClickListener(backListener);
        signout.setOnClickListener(signOutListener);
        edit_profile.setOnClickListener(editProfileClickListener);
        security.setOnClickListener(securityListener);
        cover.setOnClickListener(coverListener);
        securitySubmit.setOnClickListener(submitListener);
    }

    private View.OnClickListener backListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };

    private View.OnClickListener signOutListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
            builder.setTitle("Sign Out");
            builder.setMessage("Do You Really Want to Do It?");
            builder.setCancelable(true);
            builder.setPositiveButton("Sign Out", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mAuth.signOut();
                    Intent intent = new Intent(getApplicationContext(),LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    return;
                }
            });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();

        }
    };

    private View.OnClickListener editProfileClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Intent intent = new Intent(getApplicationContext(),RegisterActivity.class);
            intent.putExtra(Constants.KEY_MOBILE,mAuth.getCurrentUser().getPhoneNumber());
            startActivity(intent);
        }
    };

    private void getUserData() {
        mine = mAuth.getCurrentUser().getPhoneNumber();
        mDatabase.collection(Constants.KEY_COLLECTION_USERS)
                .document(mine)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful() && task.getResult() != null){
                            User me = task.getResult().toObject(User.class);
                            name.setText(me.getName());
                            Glide.with(getApplicationContext()).load(me.getProfile_url()).into(profile);
                            mobileNumber.setText(me.getMobile());
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    }
                });
    }

    private void initDB() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseFirestore.getInstance();
    }

    private void bindView() {
        profile = (CircleImageView) findViewById(R.id.profile);
        name = (TextView) findViewById(R.id.name);
        mobileNumber = (TextView) findViewById(R.id.mobileNumber);
        signout = (RelativeLayout) findViewById(R.id.signout);
        edit_profile = (RelativeLayout) findViewById(R.id.edit_profile);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        security = (RelativeLayout) findViewById(R.id.security);

        securityContainer = (RelativeLayout) findViewById(R.id.security_container);
        securityPassword = (EditText) findViewById(R.id.password);
        securitySubmit = (TextView) findViewById(R.id.security_submit);
        cover = (View) findViewById(R.id.cover);

        back = (ImageView) findViewById(R.id.back);
    }

    private View.OnClickListener securityListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            openSecurityContainer();
        }
    };

    private void openSecurityContainer() {
        securityContainer.setVisibility(View.VISIBLE);
        cover.setVisibility(View.VISIBLE);
        securityContainer.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.image_open));
        cover.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fade_in));
    }


    private View.OnClickListener coverListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            closeSecurityContainer();
        }
    };

    private void closeSecurityContainer() {

        securityContainer.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.image_close));
        cover.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fade_out));

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                securityContainer.setVisibility(View.GONE);
                cover.setVisibility(View.GONE);
            }
        },200);
    }

    private View.OnClickListener submitListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String passwordText = securityPassword.getText().toString();
            if(passwordText.equals("")){
               return;
            }
            if(passwordText.length()!=4){
                Toast.makeText(ProfileActivity.this, "Password length must be 4.", Toast.LENGTH_SHORT).show();
                return;
            }
            SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);

            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putString(Constants.KEY_PASSWORD,passwordText);
            editor.apply();

            securityPassword.setText(null);
            Toast.makeText(ProfileActivity.this, "Password changed successfully!", Toast.LENGTH_SHORT).show();
        }
    };


}