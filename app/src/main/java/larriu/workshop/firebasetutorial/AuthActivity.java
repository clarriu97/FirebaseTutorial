package larriu.workshop.firebasetutorial;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AuthActivity extends AppCompatActivity {

    private FirebaseAnalytics firebaseAnalytics;
    private FirebaseAuth firebaseAuth;
    private String email, password;
    private Button loginButton, signinButton, googleButton, facebookButton;
    private boolean emailPasswordNoEmpty;
    private static final int GOOGLE_SIGN_IN = 100;
    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        loginButton = findViewById(R.id.loginButton);
        signinButton = findViewById(R.id.signupButton);
        googleButton = findViewById(R.id.googleButton);
        facebookButton = findViewById(R.id.facebookButton);
        emailPasswordNoEmpty = false;
        callbackManager = CallbackManager.Factory.create();

        // Obtener la instancia de FirebaseAnalytics
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Bundle bundle = new Bundle();
        bundle.putString("message", "Integración de Firebase completa");
        firebaseAnalytics.logEvent("InitScreen", bundle);

        setUp();

        session();

        //Suscribirse a un tema, según el cual luego podremos enviar notificaciones
        FirebaseMessaging.getInstance().subscribeToTopic("tutorial");
    }

    @Override
    protected void onStart() {
        super.onStart();

        //((LinearLayout)findViewById(R.id.authLayout)).setVisibility(View.VISIBLE);
    }

    private void session() {
        SharedPreferences preferences = getSharedPreferences("credenciales", Context.MODE_PRIVATE);
        String email = preferences.getString("email", null);
        String provider = preferences.getString("provider", null);

        if (email != null && provider != null){
            //((LinearLayout)findViewById(R.id.authLayout)).setVisibility(View.INVISIBLE);
            showHome(email, ProviderType.valueOf(provider));
        }
    }

    private void setUp() {
        setTitle("AUTENTICACIÓN");

        signinButton.setOnClickListener(new SignInButtonListener());
        loginButton.setOnClickListener(new LogInButtonListener());
        googleButton.setOnClickListener(new GoogleButtonListener());
        facebookButton.setOnClickListener(new FacebookButtonListener());

    }

    private void showHome(String email, ProviderType provider) {
        Bundle bundle = new Bundle();
        bundle.putString("email", email);
        bundle.putString("provider", provider.toString());
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("bundle", bundle);
        startActivity(intent);
        finish();
    }

    private void showAlert(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error");
        builder.setMessage("Se ha producido un error autenticando al usuario");
        builder.setPositiveButton("Aceptar", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private class SignInButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {

            if (checkEmailPasswordNoEmpty()){
                firebaseAuth = FirebaseAuth.getInstance();
                firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            showHome(task.getResult().getUser().getEmail(), ProviderType.BASIC);
                        } else {
                            showAlert();
                        }
                    }
                });
            } else {
                Toast.makeText(AuthActivity.this, "Los campos deben estar rellenos", Toast.LENGTH_SHORT).show();
            }


        }
    }

    private class LogInButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {

            if (checkEmailPasswordNoEmpty()){
                firebaseAuth = FirebaseAuth.getInstance();
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            showHome(task.getResult().getUser().getEmail(), ProviderType.BASIC);
                        } else {
                            showAlert();
                        }
                    }
                });
            } else {
                Toast.makeText(AuthActivity.this, "Los campos deben estar rellenos", Toast.LENGTH_SHORT).show();
            }

        }
    }

    private boolean checkEmailPasswordNoEmpty(){
        email = ((TextView)findViewById(R.id.emailEditText)).getText().toString();
        password = ((TextView)findViewById(R.id.passwordEditText)).getText().toString();
        if (!email.isEmpty() && !password.isEmpty()){
            emailPasswordNoEmpty = true;
        }
        return emailPasswordNoEmpty;
    }

    private class GoogleButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            GoogleSignInOptions googleConf = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();

            GoogleSignInClient googleClient = GoogleSignIn.getClient(AuthActivity.this, googleConf);
            googleClient.signOut();

            startActivityForResult(googleClient.getSignInIntent(), GOOGLE_SIGN_IN);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        callbackManager.onActivityResult(requestCode, resultCode, data);

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GOOGLE_SIGN_IN){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                final GoogleSignInAccount account = task.getResult(ApiException.class);

                if (account != null){
                    AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                    FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){
                                showHome(account.getEmail(), ProviderType.GOOGLE);
                            } else {
                                showAlert();
                            }
                        }
                    });
                }
            } catch (ApiException e){
                showAlert();
            }

        }
    }

    private class FacebookButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            List<String> list = new ArrayList<>();
            list.add("email");
            LoginManager.getInstance().logInWithReadPermissions(AuthActivity.this, list);

            LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(final LoginResult loginResult) {
                    if (loginResult != null){
                        AccessToken token = loginResult.getAccessToken();

                        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
                        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    showHome(task.getResult().getUser().getEmail(), ProviderType.FACEBOOK);
                                } else {
                                    showAlert();
                                }
                            }
                        });
                        }
                }

                @Override
                public void onCancel() {}

                @Override
                public void onError(FacebookException error) {
                    showAlert();
                }
            });
        }
    }
}